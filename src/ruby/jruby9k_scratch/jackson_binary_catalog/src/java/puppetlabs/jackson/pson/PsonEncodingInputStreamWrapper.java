package puppetlabs.jackson.pson;

import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;

public class PsonEncodingInputStreamWrapper implements InputStreamWrapper {
    public InputStream wrap(InputStream inputStream) {
        return new PsonEncodedInputStream(inputStream);
    }

    public static class PsonEncodedInputStream extends InputStream {

        private final InputStream orig;
        private int b = -1;
        private int tmp;

        public PsonEncodedInputStream(InputStream orig) {
            this.orig = orig;
        }

        @Override
        public int read() throws IOException {
            if (b != -1) {
                tmp = b;
                b = -1;
                return tmp;
            }

            b = orig.read();
            if (b != '"') {
                tmp = b;
                b = -1;
                return tmp;
            } else {
                return '\\';
            }
        }
    }
}
