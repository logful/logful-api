package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.Constants;
import com.igexin.log.restapi.GlobalReference;
import com.igexin.log.restapi.RestApiApplication;
import com.igexin.log.restapi.entity.Layout;
import com.igexin.log.restapi.entity.LayoutItem;
import com.igexin.log.restapi.entity.LogLine;
import com.igexin.log.restapi.util.StringUtil;
import org.graylog2.gelfclient.GelfMessage;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;

public class GrayLogSender implements SenderInterface {

    @Override
    public void send(LogLine logLine) {
        if (GlobalReference.isConnected()) {
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
                // 如果解析没有错误则发送日志
                message.setLogLine(logLine);
                try {
                    RestApiApplication.transport.send(message);
                } catch (InterruptedException e) {
                    logLine.setStatus(LogLine.STATE_FAILED);
                    GlobalReference.saveLogLine(logLine);

                    e.printStackTrace();
                }
            }
        } else {
            logLine.setStatus(LogLine.STATE_FAILED);
            GlobalReference.saveLogLine(logLine);
        }
    }

    @Override
    public void close() {
        // Noting
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
