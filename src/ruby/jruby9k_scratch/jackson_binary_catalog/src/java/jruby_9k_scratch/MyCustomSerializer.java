package jruby_9k_scratch;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MyCustomSerializer extends StdScalarSerializer<String> {
    public MyCustomSerializer() {
        super(String.class);
    }

    public static class MyUnencodedJsonGenerator extends UTF8JsonGenerator {
        private final static byte BYTE_QUOTE = (byte) '"';

        public MyUnencodedJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out) {
            super(ctxt, features, codec, out);
        }

        public MyUnencodedJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out, byte[] outputBuffer, int outputOffset, boolean bufferRecyclable) {
            super(ctxt, features, codec, out, outputBuffer, outputOffset, bufferRecyclable);
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
            throw new IllegalStateException("Not supported!");
        }

        @Override
        public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
            //            super.writeBinary(b64variant, data, offset, len);
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

    public static class MyMappingJsonFactory extends MappingJsonFactory {
        @Override
        protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
            throw new IllegalStateException("No UTF8 here, buddy.");
        }

        protected JsonGenerator _createUnencodedGenerator(OutputStream out, IOContext ctxt) throws IOException {
            MyUnencodedJsonGenerator gen = new MyUnencodedJsonGenerator(ctxt,
                    _generatorFeatures, _objectCodec, out);
            if (_characterEscapes != null) {
                gen.setCharacterEscapes(_characterEscapes);
            }
            SerializableString rootSep = _rootValueSeparator;
            if (rootSep != DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR) {
                gen.setRootValueSeparator(rootSep);
            }
            return gen;
        }

        /** TODO: may not need to override the stuff below, may be that
         *  overriding _createUTF8Generator is enough **/

        @Override
        public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
//            return super.createGenerator(out, enc);
            // false -> we won't manage the stream unless explicitly directed to
            IOContext ctxt = _createContext(out, false);
            ctxt.setEncoding(enc);
            if (enc == JsonEncoding.UTF8) {
                return _createUnencodedGenerator(_decorate(out, ctxt), ctxt);
            } else {
                throw new IllegalStateException("In the world of Jackpson, we expect everyone to say they're UTF-8 even though they are not.");
            }
//            Writer w = _createWriter(out, enc, ctxt);
//            return _createGenerator(_decorate(w, ctxt), ctxt);
        }

        @Override
        public JsonGenerator createGenerator(OutputStream out) throws IOException {
            throw new IllegalStateException("Not implemented!");
//            return super.createGenerator(out);
        }

        @Override
        public JsonGenerator createGenerator(Writer w) throws IOException {
            throw new IllegalStateException("Not implemented!");
//            return super.createGenerator(w);
        }

        @Override
        public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
            throw new IllegalStateException("Not implemented!");
//            return super.createGenerator(f, enc);
        }
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new MyMappingJsonFactory());
        SimpleModule testModule = new SimpleModule("MyModule", new Version(1, 0, 0, null, "foo", "foo"));
        testModule.addSerializer(new MyCustomSerializer()); // assuming serializer declares correct class to bind to
        mapper.registerModule(testModule);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, String> value = new HashMap<>();
        value.put("foo", "bar");
        mapper.writeValue(out, value);
        out.close();
        System.out.println("Serialized: '" + IOUtils.toString(out.toInputStream(), "UTF-8") + "'");
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        int numBytes = 10000;
        byte[] bytes =  new byte[numBytes];
        for (int i = 0; i < 10000; i++) {
            bytes[i] = 'f';
        }
        InputStream binaryData = new ByteArrayInputStream(bytes);
        gen.writeBinary(binaryData, -1);
    }
}
