package puppetlabs.jackson.pson;

import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;

public class PsonDecodingInputStreamWrapper implements InputStreamWrapper {
    public InputStream wrap(InputStream inputStream) {
        return new PsonDecodedInputStream(inputStream);
    }

    public static class PsonDecodedInputStream extends InputStream {

        private final InputStream orig;
        private int curByte;
        private int peekByte = -1;


        public PsonDecodedInputStream(InputStream orig) {
            this.orig = orig;
        }

        @Override
        public int read() throws IOException {
            if (peekByte != -1) {
                curByte = peekByte;
                peekByte = -1;
            } else {
                curByte = orig.read();
            }

            if (curByte == -1) {
                return -1;
            }

            if (curByte == '\\') {
                peekByte = orig.read();
                if (peekByte == '"') {
                    peekByte = -1;
                    return '"';
                } else {
                    return curByte;
                }
            }

            return curByte;
        }
    }
}
