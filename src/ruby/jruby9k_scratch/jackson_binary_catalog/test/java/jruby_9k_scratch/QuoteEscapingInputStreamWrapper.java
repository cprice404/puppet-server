package jruby_9k_scratch;

import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.InputStream;

public class QuoteEscapingInputStreamWrapper implements InputStreamWrapper {
    public InputStream wrap(InputStream inputStream) {
        return new EscapedQuoteInputStream(inputStream);
    }
}
