package com.igexin.log.restapi;

public class Constants {

    /**
     * 详细信息.
     */
    public static final int VERBOSE = 0x01;

    /**
     * 调试信息.
     */
    public static final int DEBUG = 0x02;

    /**
     * 通告信息.
     */
    public static final int INFO = 0x03;

    /**
     * 警告信息.
     */
    public static final int WARN = 0x04;

    /**
     * 错误信息.
     */
    public static final int ERROR = 0x05;

    /**
     * 异常信息.
     */
    public static final int EXCEPTION = 0x06;

    /**
     * 致命信息.
     */
    public static final int FATAL = 0x07;

    /**
     * 缓存目录.
     */
    public static final String CACHE_DIR = "cache";

    /**
     * 解析出错的日志文件目录.
     */
    public static final String ERROR_DIR = "error";

    /**
     * 已解密的日志文件目录.
     */
    public static final String DECRYPTED_DIR = "decrypted";

    /**
     * 应用崩溃文件目录.
     */
    public static final String CRASH_REPORT_DIR = "crash";

    /**
     * 日志文件处理临时文件目录.
     */
    public static final String LOG_FILE_TEMP_DIR = "temp";

    /**
     * 附件文件目录.
     */
    public static final String ATTACHMENT_DIR = "attachment";

    public static final String WEED_TEMP_DIR = "weed";

    public static final String DEFAULT_ATTRIBUTE_SEPARATOR = ",";

    public static final String DEFAULT_FIELD_SEPARATOR = ":";

    public static final String LOG_LINE_SEPARATOR = "\u1699\u168f\u16e5";

    public static final String NEW_LINE_CHARACTER = "\u203c\u204b\u25a9";

    /**
     * 加解密失败返回字符串.
     */
    public static final String CRYPTO_ERROR = "CRYPTO_ERROR";

    public static final int DEFAULT_GRAY_LEVEL = 100;

    public static final int GRAYLOG_QUEUE_CAPACITY = 5120;

    public static final int WEED_QUEUE_CAPACITY = 5120;

    public static final int PARSER_QUEUE_CAPACITY = 2048;

    public static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 4;

    public static final String CLIENT_ID = "525b8747323d49078a96e49f0189de98";

    public static final String CLIENT_SECRET = "02ce8e2adba94ae5a4807e3f12ea34f3";

    public static final int PLATFORM_ANDROID = 1;

    public static final int PLATFORM_IOS = 2;

    public static final int RECONNECT_WEED_DELAY = 10;
}
