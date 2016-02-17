package com.getui.logful.server.parse;

import com.getui.logful.server.GlobalReference;
import com.getui.logful.server.ServerProperties;
import com.getui.logful.server.entity.Layout;
import com.getui.logful.server.entity.LayoutItem;
import com.getui.logful.server.entity.LogMessage;
import com.getui.logful.server.mongod.LogMessageRepository;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.apache.commons.lang.StringUtils;
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
    ServerProperties serverProperties;

    @Autowired
    LogMessageRepository logMessageRepository;

    private AtomicBoolean connected = new AtomicBoolean(false);

    private GelfTransport graylogTransport;

    @PostConstruct
    public void create() {
        MongoOperations operations = logMessageRepository.getOperations();
        try {
            BasicDBObject index = new BasicDBObject("writeDate", 1);
            BasicDBObject options = new BasicDBObject("expireAfterSeconds", serverProperties.expires());

            DBCollection collection = operations.getCollection(operations.getCollectionName(LogMessage.class));
            collection.createIndex(index, options);
        } catch (Exception e) {
            LOG.error("Exception", e);
        }

        String host = serverProperties.graylogHost();
        int port = serverProperties.graylogPort();

        if (StringUtils.isEmpty(host)) {
            throw new IllegalArgumentException("Graylog host can not be null!");
        }

        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("Graylog port Not valid!");
        }

        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        GelfConfiguration config = new GelfConfiguration(socketAddress)
                .transport(GelfTransports.TCP)
                .queueSize(serverProperties.getGraylog().getQueueCapacity())
                .connectTimeout(serverProperties.getGraylog().getConnectTimeout())
                .reconnectDelay(serverProperties.getGraylog().getReconnectDelay())
                .tcpNoDelay(true)
                .sendBufferSize(serverProperties.getGraylog().getSendBufferSize());
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
            public void retrySuccessful(LogMessage logMessage) {
                if (logMessage.getId() != null && logMessage.getId().length() > 0) {
                    logMessage.setStatus(LogMessage.STATE_SUCCESSFUL);
                    logMessageRepository.save(logMessage);
                }
            }

            @Override
            public void failed(LogMessage logMessage) {
                logMessageRepository.save(logMessage);
            }
        });
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void write(LogMessage logMessage) {
        if (connected.get()) {
            GelfMessage message = createMessage(logMessage);
            if (message != null) {
                try {
                    write(message);
                } catch (InterruptedException e) {
                    logMessageRepository.save(logMessage);
                    LOG.error("Exception", e);
                }
            }
        } else {
            logMessageRepository.save(logMessage);
        }
    }

    public void write(GelfMessage message) throws InterruptedException {
        graylogTransport.send(message);
    }

    @Override
    public void send(LogMessage logMessage) {
        write(logMessage);
    }

    @Override
    public void release() {
        // Nothing.
    }

    public GelfMessage createMessage(LogMessage logMessage) {
        boolean formatError = false;
        GelfMessageBuilder builder = new GelfMessageBuilder(logMessage.getTag(), "127.0.0.1")
                .level(GelfMessageLevel.INFO);
        GelfMessage message = builder.message(logMessage.getMessage())
                .timestamp(logMessage.getTimestamp() / 1000D)
                .additionalField("_tag", logMessage.getTag())
                .additionalField("_platform", logMessage.getPlatform())
                .additionalField("_uid", logMessage.getUid())
                .additionalField("_app_id", logMessage.getAppId())
                .additionalField("_log_level", logMessage.getLevel())
                .additionalField("_log_name", logMessage.getLoggerName())
                .additionalField("_log_timestamp", logMessage.getTimestamp())
                .build();

        String attachment = logMessage.getAttachment();
        if (StringUtils.isNotEmpty(attachment)) {
            message.addAdditionalField("_attachment", attachment);
        }

        String alias = logMessage.getAlias();
        if (StringUtils.isNotEmpty(alias)) {
            message.addAdditionalField("_alias", alias);
        }

        String msg = logMessage.getMessage();
        String template = logMessage.getMsgLayout();

        Layout layout = GlobalReference.getLayout(template);

        String[] columns = msg.split("\\|");
        int index = 1;
        for (String col : columns) {
            String[] keyValue = col.split(":", 2);
            if (layout != null && keyValue.length == 2) {
                String abbr = keyValue[0];
                LayoutItem layoutItem = layout.getItem(abbr);
                if (layoutItem == null) {
                    message.addAdditionalField("_col" + index, col);
                } else {
                    String key = "_col_" + layoutItem.getFullName();
                    switch (layoutItem.getType()) {
                        case LayoutItem.TYPE_NUMBER:
                            NumberParseResult result = parse(keyValue[1]);
                            if (result.isSuccessful()) {
                                message.addAdditionalField(key, result.getObject());
                            } else {
                                formatError = true;
                            }
                            break;
                        case LayoutItem.TYPE_STRING:
                            message.addAdditionalField(key, keyValue[1]);
                            break;
                        default:
                            break;
                    }
                }
            } else {
                message.addAdditionalField("_col" + index, col);
            }
            index++;
        }

        return formatError ? null : message;
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

    private static class NumberParseResult {

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
