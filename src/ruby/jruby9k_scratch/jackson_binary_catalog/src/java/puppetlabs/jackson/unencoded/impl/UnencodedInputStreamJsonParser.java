package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;

public class UnencodedInputStreamJsonParser extends UTF8StreamJsonParser {
    public UnencodedInputStreamJsonParser(IOContext ctxt, int features, InputStream in, ObjectCodec codec, ByteQuadsCanonicalizer sym, byte[] inputBuffer, int start, int end, boolean bufferRecyclable) {
        super(ctxt, features, in, codec, sym, inputBuffer, start, end, bufferRecyclable);
    }

    public InputStream getBytesAsInputStream() throws IOException {
        if (_currToken == JsonToken.VALUE_STRING) {
            if (_tokenIncomplete) {
                _tokenIncomplete = false;
                return _readToInputStream();
            }
        }
        throw new IllegalStateException("Unexpected state!  Expected parser to only be called for string values.");
    }


    private InputStream _readToInputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte b;
        boolean isPrevCharBackslash = false;

        main_loop:
        while (true) {
            int ptr = _inputPtr;
            if (ptr >= _inputEnd) {
                loadMoreGuaranteed();
                ptr = _inputPtr;
            }
            while (ptr < _inputEnd) {
                b = _inputBuffer[ptr++];

                _inputPtr = ptr;

                if (!isPrevCharBackslash && (b == INT_QUOTE)) {
                    break main_loop;
                }

                if (b == INT_BACKSLASH) {
                    isPrevCharBackslash = true;
                } else {
                    isPrevCharBackslash = false;
                }

                out.write(b);
            }
        }

        return out.toInputStream();
    }
}
