package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UnencodedInputStreamJsonGenerator extends UTF8JsonGenerator {
    private final static byte BYTE_QUOTE = (byte) '"';

    public UnencodedInputStreamJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out) {
        super(ctxt, features, codec, out);
    }

    public UnencodedInputStreamJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out, byte[] outputBuffer, int outputOffset, boolean bufferRecyclable) {
        super(ctxt, features, codec, out, outputBuffer, outputOffset, bufferRecyclable);
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        throw new IllegalStateException("Not supported!");
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
        _verifyValueWrite(WRITE_BINARY);
        // Starting quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;


        byte[] encodingBuffer = _ioContext.allocBase64Buffer();
        int bytes;

        try {
            bytes = _writeUnencodedBinary(data, encodingBuffer);
        } finally {
            _ioContext.releaseBase64Buffer(encodingBuffer);
        }

        // and closing quotes
        if (_outputTail >= _outputEnd) {
            _flushBuffer();
        }
        _outputBuffer[_outputTail++] = BYTE_QUOTE;
        return dataLength;
    }

    private int _writeUnencodedBinary(InputStream data, byte[] readBuffer) throws IOException {

        int inputPtr = readBuffer.length;
        int inputEnd = 0;
        int bytesRead = 0;

        while (true) {
            if (inputPtr >= readBuffer.length) { // need to load more
                inputPtr = 0;
                inputEnd = data.read(readBuffer, 0, readBuffer.length);
                bytesRead += inputEnd;
            }
            while (inputPtr < inputEnd) {
                if (_outputTail >= _outputEnd) {
                    _flushBuffer();
                }
                _outputBuffer[_outputTail++] = readBuffer[inputPtr];
                inputPtr++;
            }
            if (inputEnd < readBuffer.length) {
                break;
            }
        }

        return bytesRead;
    }

}
