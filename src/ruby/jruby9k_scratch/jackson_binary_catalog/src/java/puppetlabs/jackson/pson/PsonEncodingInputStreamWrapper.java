package puppetlabs.jackson.pson;

import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PsonEncodingInputStreamWrapper implements InputStreamWrapper {
    public InputStream wrap(InputStream inputStream) {
        return new PsonEncodedInputStream(inputStream);
    }

    public static class PsonEncodedInputStream extends InputStream {

        private static final int INT_QUOTE = '"';
        private static final int INT_BACKSLASH = '\\';

        private static final Map<Integer, String> REPLACEMENTS = new HashMap<>();
        static {
            REPLACEMENTS.put(0x0, "\\u0000");
            REPLACEMENTS.put(0x1, "\\u0001");
            REPLACEMENTS.put(0x2, "\\u0002");
            REPLACEMENTS.put(0x3, "\\u0003");
            REPLACEMENTS.put(0x4, "\\u0004");
            REPLACEMENTS.put(0x5, "\\u0005");
            REPLACEMENTS.put(0x6, "\\u0006");
            REPLACEMENTS.put(0x7, "\\u0007");
            REPLACEMENTS.put(0x8, "\\b");
            REPLACEMENTS.put(0x9, "\\t");
            REPLACEMENTS.put(0xa, "\\n");
            REPLACEMENTS.put(0xb, "\\u000b");
            REPLACEMENTS.put(0xc, "\\f");
            REPLACEMENTS.put(0xd, "\\r");
            REPLACEMENTS.put(0xe, "\\u000e");
            REPLACEMENTS.put(0xf, "\\u000f");
            REPLACEMENTS.put(0x10, "\\u0010");
            REPLACEMENTS.put(0x11, "\\u0011");
            REPLACEMENTS.put(0x12, "\\u0012");
            REPLACEMENTS.put(0x13, "\\u0013");
            REPLACEMENTS.put(0x14, "\\u0014");
            REPLACEMENTS.put(0x15, "\\u0015");
            REPLACEMENTS.put(0x16, "\\u0016");
            REPLACEMENTS.put(0x17, "\\u0017");
            REPLACEMENTS.put(0x18, "\\u0018");
            REPLACEMENTS.put(0x19, "\\u0019");
            REPLACEMENTS.put(0x1a, "\\u001a");
            REPLACEMENTS.put(0x1b, "\\u001b");
            REPLACEMENTS.put(0x1c, "\\u001c");
            REPLACEMENTS.put(0x1d, "\\u001d");
            REPLACEMENTS.put(0x1e, "\\u001e");
            REPLACEMENTS.put(0x1f, "\\u001f");
            REPLACEMENTS.put(INT_QUOTE, "\\\"");
            REPLACEMENTS.put(INT_BACKSLASH, "\\\\");
        };


        private final InputStream orig;
        private int b = -1;
        private byte[] psonBuffer = new byte[6];
        private int psonBufferSize = -1;
        private int psonBufferOffset = -1;


        public PsonEncodedInputStream(InputStream orig) {
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
            } else if (((b >= 0) && (b <= 0x1f)) ||
                    (b == INT_QUOTE) || (b == INT_BACKSLASH)) {
                populatePsonBuffer(b);
                return psonBuffer[psonBufferOffset];
            } else {
                return b;
            }
        }

        private void populatePsonBuffer(int val) {
            String subst = REPLACEMENTS.get(val);
            byte[] substBytes = subst.getBytes();
            System.arraycopy(substBytes, 0, psonBuffer, 0, substBytes.length);
            psonBufferSize = substBytes.length;
            psonBufferOffset = 0;
        }
    }
}
