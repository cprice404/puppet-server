package jruby_9k_scratch;

import java.io.IOException;
import java.io.InputStream;

public class EscapedQuoteInputStream extends InputStream {

    private final InputStream orig;
    private int b = -1;
    private int tmp;

    public EscapedQuoteInputStream(InputStream orig) {
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
