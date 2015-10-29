package com.igexin.log.restapi.parse;

import com.igexin.log.restapi.util.ByteUtil;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class LogFileParser implements ParserInterface {

    public interface ParserEventListener {
        void output(long timestamp, String encryptedTag, String encryptedMsg, short layoutId, int attachmentId);

        void result(String inFilePath, boolean successful);
    }

    private ParserEventListener listener;

    public void setListener(ParserEventListener listener) {
        this.listener = listener;
    }

    @Override
    public void parse(String inFilePath) {
        boolean successful = true;
        FileInputStream fileInputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            fileInputStream = new FileInputStream(inFilePath);
            bufferedInputStream = new BufferedInputStream(fileInputStream);
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
                String encryptedTag = new String(tagChunk);

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
                String encryptedMsg = new String(msgChunk);

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

                if (value == -100) {
                    if (listener != null) {
                        listener.output(timestamp, encryptedTag, encryptedMsg, layoutId, -1);
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
                                listener.output(timestamp, encryptedTag, encryptedMsg, layoutId, attachmentId);
                            }
                        }
                    }
                }
            }
            bufferedInputStream.close();
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (listener != null) {
            listener.result(inFilePath, successful);
        }
    }

}
