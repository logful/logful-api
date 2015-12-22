package com.getui.logful.server.parse;

import com.getui.logful.server.util.ByteUtil;
import com.getui.logful.server.util.CryptoTool;
import com.getui.logful.server.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LogFileParser implements ParserInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LogFileParser.class);

    public interface ParserEventListener {
        void output(long timestamp, String tag, String msg, short layoutId, int attachmentId);

        void result(boolean successful);
    }

    private final ParserEventListener listener;

    public LogFileParser(ParserEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void parse(byte[] security, int version, InputStream inputStream) {
        boolean successful = true;
        BufferedInputStream bufferedInputStream = null;
        try {
            bufferedInputStream = new BufferedInputStream(inputStream);
            while (true) {
                int bytesRead;

                // Parse log line time
                byte[] timeLenChunk = new byte[2];
                bytesRead = bufferedInputStream.read(timeLenChunk);
                if (bytesRead == -1) {
                    break;
                }
                if (bytesRead != 2) {
                    successful = false;
                    break;
                }
                short timeLength = ByteUtil.bytesToShort(timeLenChunk);
                byte[] timeChunk = new byte[timeLength];
                bytesRead = bufferedInputStream.read(timeChunk);
                if (bytesRead != timeLength) {
                    successful = false;
                    break;
                }
                long timestamp = ByteUtil.bytesToLong(timeChunk);

                // Parse log line tag
                byte[] tagLenChunk = new byte[2];
                bytesRead = bufferedInputStream.read(tagLenChunk);
                if (bytesRead != 2) {
                    successful = false;
                    break;
                }
                short tagLength = ByteUtil.bytesToShort(tagLenChunk);
                byte[] tagChunk = new byte[tagLength];
                bytesRead = bufferedInputStream.read(tagChunk);
                if (bytesRead != tagLength) {
                    successful = false;
                    break;
                }

                // Parse log line msg
                byte[] msgLenChunk = new byte[2];
                bytesRead = bufferedInputStream.read(msgLenChunk);
                if (bytesRead != 2) {
                    successful = false;
                    break;
                }
                short msgLength = ByteUtil.bytesToShort(msgLenChunk);
                byte[] msgChunk = new byte[msgLength];
                bytesRead = bufferedInputStream.read(msgChunk);
                if (bytesRead != msgLength) {
                    successful = false;
                    break;
                }

                // Parse log line msg layout id
                byte[] layoutIdLenChunk = new byte[2];
                bytesRead = bufferedInputStream.read(layoutIdLenChunk);
                if (bytesRead != 2) {
                    successful = false;
                    break;
                }
                short layoutIdLen = ByteUtil.bytesToShort(layoutIdLenChunk);
                byte[] layoutIdChunk = new byte[layoutIdLen];
                bytesRead = bufferedInputStream.read(layoutIdChunk);
                if (bytesRead != layoutIdLen) {
                    successful = false;
                    break;
                }
                short layoutId = ByteUtil.bytesToShort(layoutIdChunk);

                // Test parse log line eof chunk
                byte[] testMark = new byte[2];
                bytesRead = bufferedInputStream.read(testMark);
                if (bytesRead != 2) {
                    successful = false;
                    break;
                }
                short value = ByteUtil.bytesToShort(testMark);

                String tag = CryptoTool.AESDecrypt(security, tagChunk, version);
                String msg = CryptoTool.AESDecrypt(security, msgChunk, version);
                if (StringUtil.decryptError(tag) || StringUtil.decryptError(msg)) {
                    successful = false;
                    break;
                }

                if (value == -100) {
                    if (listener != null) {
                        listener.output(timestamp, tag, msg, layoutId, -1);
                    }
                } else {
                    if (value == 4) {
                        // Read attachment id chunk
                        byte[] attachmentIdChunk = new byte[value];
                        bytesRead = bufferedInputStream.read(attachmentIdChunk);
                        if (bytesRead != value) {
                            successful = false;
                            break;
                        }
                        int attachmentId = ByteUtil.bytesToInt(attachmentIdChunk);

                        // Read eof mark chunk
                        byte[] eofMark = new byte[2];
                        bytesRead = bufferedInputStream.read(eofMark);
                        if (bytesRead != 2) {
                            successful = false;
                            break;
                        }
                        short eof = ByteUtil.bytesToShort(eofMark);
                        if (eof == -100) {
                            if (listener != null) {
                                listener.output(timestamp, tag, msg, layoutId, attachmentId);
                            }
                        }
                    }
                }
            }
            bufferedInputStream.close();
            inputStream.close();
        } catch (Exception e) {
            LOG.error("Exception", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error("Exception", e);
                }
            }
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    LOG.error("Exception", e);
                }
            }
        }
        if (listener != null) {
            listener.result(successful);
        }
    }

}
