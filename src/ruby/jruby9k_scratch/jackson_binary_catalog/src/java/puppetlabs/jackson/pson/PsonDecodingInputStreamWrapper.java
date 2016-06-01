package puppetlabs.jackson.pson;

import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PsonDecodingInputStreamWrapper implements InputStreamWrapper {
    public InputStream wrap(InputStream inputStream) {
        return new PsonDecodedInputStream(inputStream);
    }

    public static class PsonDecodedInputStream extends InputStream {

        private static final int INT_QUOTE = '"';
        private static final int INT_BACKSLASH = '\\';

        private static final Map<Integer, Integer> REPLACEMENTS = new HashMap<>();
        static {
            REPLACEMENTS.put((int)'b', (int)'\b');
            REPLACEMENTS.put((int)'t', (int)'\t');
            REPLACEMENTS.put((int)'n', (int)'\n');
            REPLACEMENTS.put((int)'f', (int)'\f');
            REPLACEMENTS.put((int)'r', (int)'\r');
            REPLACEMENTS.put(INT_QUOTE, INT_QUOTE);
            REPLACEMENTS.put(INT_BACKSLASH, INT_BACKSLASH);
        };

        private final InputStream orig;
        private int b;
        private byte[] psonBuffer = new byte[4];
        private int psonBufferSize = -1;
        private int psonBufferOffset = -1;


        public PsonDecodedInputStream(InputStream orig) {
            this.orig = orig;
        }

        @Override
        public int read() throws IOException {
            if (psonBufferOffset != -1) {
                psonBufferOffset++;
                if (psonBufferOffset >= psonBufferSize) {
                    psonBufferOffset = -1;
                } else {
                    return psonBuffer[psonBufferOffset];
                }
            }

            b = orig.read();
            if (b == -1) {
                return -1;
            } else if (b == '\\') {
                return readEscapedByte();
            } else {
                return b;
            }
        }

        private int readEscapedByte() throws IOException {
            b = orig.read();
            if (REPLACEMENTS.containsKey(b)) {
                return REPLACEMENTS.get(b);
            } else if (b == 'u') {
                return readUnicodeByte();
            } else {
                psonBuffer[0] = INT_BACKSLASH;
                psonBuffer[1] = (byte)b;
                psonBufferSize = 2;
                psonBufferOffset = 0;
                return psonBuffer[psonBufferOffset];
            }
        }

        private int readUnicodeByte() throws IOException {
            // TODO: this code is crap.  need to go back through it and use the
            // appropriate data types (char/int/byte) in the appropriate places.
            for (psonBufferOffset = 0; psonBufferOffset < 4; psonBufferOffset++) {
                // this is disgusting and doesn't account for the case where
                // Character.digit returns -1, but I haven't yet found a better way
                // to go from an ascii hex char to the corresponding byte representation.
                psonBuffer[psonBufferOffset] = (byte) Character.digit((char)(orig.read()), 16);
            }
//            orig.read(psonBuffer, 0, 4);
            System.out.println("READING UNICODE BYTE; FILLED PSON BUFFER:");
            for (int i = 0; i < 4; i++) {
                System.out.println("\tpsonBuffer[" + i + "]: " + psonBuffer[i]);
            }
            // we know that during encoding we only went up to 0x1f, so
            // we can assume the integer will only require one byte to represent.
            b = ((psonBuffer[0] & 0xff) << 12) | ((psonBuffer[1] & 0xff) << 8) |
                    ((psonBuffer[2] & 0xff) << 4)  | (psonBuffer[3] & 0xff);
            System.out.println("Bitwise math yielded: " + b);
            return b;
        }
    }
}
