package com.getui.logful.server.parse;

import com.getui.logful.server.Constants;
import com.getui.logful.server.GlobalReference;
import com.getui.logful.server.LogfulProperties;
import com.getui.logful.server.entity.Layout;
import com.getui.logful.server.entity.LayoutItem;
import com.getui.logful.server.entity.LogLine;
import com.getui.logful.server.mongod.MongoLogLineRepository;
import com.getui.logful.server.util.StringUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.graylog2.gelfclient.*;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class GraylogClientService implements SenderInterface {

    private static final Logger LOG = LoggerFactory.getLogger(GraylogClientService.class);

    @Autowired
    LogfulProperties logfulProperties;

    @Autowired
    MongoLogLineRepository mongoLogLineRepository;

    private AtomicBoolean connected = new AtomicBoolean(false);

    private GelfTransport graylogTransport;

    @PostConstruct
    public void create() {
        MongoOperations operations = mongoLogLineRepository.getOperations();
        try {
            BasicDBObject index = new BasicDBObject("writeDate", 1);
            BasicDBObject options = new BasicDBObject("expireAfterSeconds", logfulProperties.expires());

            DBCollection collection = operations.getCollection(operations.getCollectionName(LogLine.class));
            collection.createIndex(index, options);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        String host = logfulProperties.graylogHost();
        int port = logfulProperties.graylogPort();

        if (StringUtil.isEmpty(host)) {
            throw new IllegalArgumentException("Graylog host can not be null!");
        }

        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("Graylog port Not valid!");
        }

        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        GelfConfiguration config = new GelfConfiguration(socketAddress)
                .transport(GelfTransports.TCP)
                .queueSize(logfulProperties.getGraylog().getQueueCapacity())
                .connectTimeout(logfulProperties.getGraylog().getConnectTimeout())
                .reconnectDelay(logfulProperties.getGraylog().getReconnectDelay())
                .tcpNoDelay(true)
                .sendBufferSize(logfulProperties.getGraylog().getSendBufferSize());
        graylogTransport = GelfTransports.create(config);
        graylogTransport.setListener(new GelfTransportListener() {
            @Override
            public void connected() {
                connected.set(true);
            }

            @Override
            public void disconnected() {
                connected.set(false);
            }

            @Override
            public void retrySuccessful(LogLine logLine) {
                if (logLine.getId() != null && logLine.getId().length() > 0) {
                    logLine.setStatus(LogLine.STATE_SUCCESSFUL);
                    mongoLogLineRepository.save(logLine);
                }
            }

            @Override
            public void failed(LogLine logLine) {
                mongoLogLineRepository.save(logLine);
            }
        });
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void write(LogLine logLine) {
        if (connected.get()) {
            GelfMessage message = createMessage(logLine);
            if (message != null) {
                try {
                    write(message);
                } catch (InterruptedException e) {
                    mongoLogLineRepository.save(logLine);
                    LOG.error("Exception", e);
                }
            }
        } else {
            mongoLogLineRepository.save(logLine);
        }
    }

    public void write(GelfMessage message) throws InterruptedException {
        graylogTransport.send(message);
    }

    @Override
    public void send(LogLine logLine) {
        write(logLine);
    }

    @Override
    public void release() {
        // Nothing.
    }

    public GelfMessage createMessage(LogLine logLine) {
        boolean formatError = false;

        GelfMessageBuilder builder = new GelfMessageBuilder(logLine.getTag(), "127.0.0.1")
                .level(GelfMessageLevel.INFO);
        GelfMessage message = builder.message(logLine.getMessage())
                .timestamp(logLine.getTimestamp() / 1000D)
                .additionalField("_tag", logLine.getTag())
                .additionalField("_platform", logLine.getPlatform())
                .additionalField("_uid", logLine.getUid())
                .additionalField("_app_id", logLine.getAppId())
                .additionalField("_log_level", logLine.getLevel())
                .additionalField("_log_name", logLine.getLoggerName())
                .additionalField("_log_timestamp", logLine.getTimestamp())
                .build();

        String attachment = logLine.getAttachment();
        if (!StringUtil.isEmpty(attachment)) {
            message.addAdditionalField("_attachment", attachment);
        }

        // 别名字段
        String alias = logLine.getAlias();
        if (alias != null && alias.length() > 0) {
            message.addAdditionalField("_alias", alias);
        }

        String msg = logLine.getMessage();
        String template = logLine.getMsgLayout();

        if (msg.contains("|")) {
            // 包含 "|"
            String[] columns = msg.split("\\|");
            int length = columns.length;
            if (template == null || template.length() == 0) {
                // 未设置模版
                for (int i = 0; i < length; i++) {
                    String key = String.format("_col%d", i + 1);
                    message.addAdditionalField(key, columns[i]);
                }
            } else {
                // 已设置模版
                Layout layout = GlobalReference.getLayout(template);
                for (int i = 0; i < length; i++) {
                    String value = columns[i];
                    if (value.contains(Constants.DEFAULT_FIELD_SEPARATOR)) {
                        // 包含 ":" 以第一个 ":" 分割
                        String[] keyValue = value.split(Constants.DEFAULT_FIELD_SEPARATOR, 2);
                        String abbr = keyValue[0];
                        LayoutItem layoutItem = layout.getItem(abbr);
                        if (layoutItem == null) {
                            // 未找到设置的字段
                            String key = String.format("_col%d", i + 1);
                            message.addAdditionalField(key, value);
                        } else {
                            // 有设置的字段
                            String key = "_col_" + layoutItem.getFullName();
                            switch (layoutItem.getType()) {
                                case LayoutItem.TYPE_NUMBER:
                                    // Number 类型
                                    NumberParseResult result = parse(keyValue[1]);
                                    if (result.isSuccessful()) {
                                        message.addAdditionalField(key, result.getObject());
                                    } else {
                                        formatError = true;
                                    }
                                    break;
                                case LayoutItem.TYPE_STRING:
                                    // String 类型
                                    message.addAdditionalField(key, keyValue[1]);
                                    break;
                            }
                        }
                    } else {
                        // 不包含 ":""
                        String key = String.format("_col%d", i + 1);
                        message.addAdditionalField(key, value);
                    }
                }
            }
        } else {
            // 不包含 "|"
            if (msg.contains(Constants.DEFAULT_FIELD_SEPARATOR)) {
                String[] keyValue = msg.split(Constants.DEFAULT_FIELD_SEPARATOR, 2);
                if (keyValue.length == 2) {
                    String abbr = keyValue[0];

                    Layout layout = GlobalReference.getLayout(template);
                    LayoutItem layoutItem = layout.getItem(abbr);

                    if (layoutItem != null) {
                        // 找到设置的字段
                        String key = "_col_" + layoutItem.getFullName();
                        switch (layoutItem.getType()) {
                            case LayoutItem.TYPE_NUMBER:
                                // Number 类型
                                NumberParseResult result = parse(keyValue[1]);
                                if (result.isSuccessful()) {
                                    message.addAdditionalField(key, result.getObject());
                                } else {
                                    formatError = true;
                                }
                                break;
                            case LayoutItem.TYPE_STRING:
                                // String 类型
                                message.addAdditionalField(key, keyValue[1]);
                                break;
                        }
                    }
                }
            }
        }
        if (!formatError) {
            return message;
        }

        return null;
    }

    public NumberParseResult parse(String string) {
        NumberParseResult result = new NumberParseResult();
        try {
            result.setObject(Integer.parseInt(string));
            result.setSuccessful(true);
        } catch (NumberFormatException e1) {
            try {
                result.setObject(Long.parseLong(string));
                result.setSuccessful(true);
            } catch (NumberFormatException e2) {
                try {
                    result.setObject(Double.parseDouble(string));
                    result.setSuccessful(true);
                } catch (NumberFormatException e3) {
                    result.setObject(string);
                    result.setSuccessful(false);
                }
            }
        }
        return result;
    }

    private class NumberParseResult {

        private boolean successful;

        private Object object;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }

    }
}
