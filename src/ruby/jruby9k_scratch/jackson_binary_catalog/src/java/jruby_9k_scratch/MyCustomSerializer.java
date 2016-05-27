package jruby_9k_scratch;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MyCustomSerializer extends StdScalarSerializer<String> {
    public MyCustomSerializer() {
        super(String.class);
    }

    public static class MyUTF8StreamJsonParser extends UTF8StreamJsonParser {

        public MyUTF8StreamJsonParser(IOContext ctxt, int features, InputStream in, ObjectCodec codec, ByteQuadsCanonicalizer sym, byte[] inputBuffer, int start, int end, boolean bufferRecyclable) {
            super(ctxt, features, in, codec, sym, inputBuffer, start, end, bufferRecyclable);
        }
    }

    public static class MyCustomDeserializer extends StdScalarDeserializer<String> {
        protected MyCustomDeserializer() {
            super(String.class);
        }

        // since 2.6, slightly faster lookups for this very common type
        @Override
        public boolean isCachable() { return true; }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
                return p.getText();
            }
            throw new IllegalStateException("I dunno why my string deserializer is being called for something that is not a string!");
//            JsonToken t = p.getCurrentToken();
//            // // [WTF databind#381]
//            if ((t == JsonToken.START_ARRAY) && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
//                p.nextToken();
//                final String parsed = _parseString(p, ctxt);
//                if (p.nextToken() != JsonToken.END_ARRAY) {
//                    throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY,
//                            "Attempted to unwrap single value array for single 'String' value but there was more than a single value in the array");
//                }
//                return parsed;
//            }
//            // need to gracefully handle byte[] data, as base64
//            if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
//                Object ob = p.getEmbeddedObject();
//                if (ob == null) {
//                    return null;
//                }
//                if (ob instanceof byte[]) {
//                    return ctxt.getBase64Variant().encode((byte[]) ob, false);
//                }
//                // otherwise, try conversion using toString()...
//                return ob.toString();
//            }
//            // allow coercions for other scalar types
//            String text = p.getValueAsString();
//            if (text != null) {
//                return text;
//            }
//            throw ctxt.mappingException(_valueClass, p.getCurrentToken());
        }
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

        @Override
        public JsonParser createParser(char[] content) throws IOException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(content);
        }

        @Override
        public JsonParser createParser(char[] content, int offset, int len) throws IOException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(content, offset, len);
        }

        @Override
        public JsonParser createParser(String content) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(content);
        }

        @Override
        public JsonParser createParser(byte[] data) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(data);
        }

        @Override
        public JsonParser createParser(byte[] data, int offset, int len) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(data, offset, len);
        }

        @Override
        public JsonParser createParser(File f) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(f);
        }

        @Override
        public JsonParser createParser(InputStream in) throws IOException, JsonParseException {
            return super.createParser(in);
//            IOContext ctxt = _createContext(in, false);

        }

        @Override
        public JsonParser createParser(Reader r) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(r);
        }

        @Override
        public JsonParser createParser(URL url) throws IOException, JsonParseException {
            throw new IllegalStateException("Not implemented!");
//            return super.createParser(url);
        }
    }

    public static void main(String[] args) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new MyMappingJsonFactory());
        SimpleModule testModule = new SimpleModule("MyModule", new Version(1, 0, 0, null, "foo", "foo"));
        testModule.addSerializer(new MyCustomSerializer()); // assuming serializer declares correct class to bind to
        testModule.addDeserializer(String.class, new MyCustomDeserializer());
        mapper.registerModule(testModule);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, Object> value = new HashMap<>();
        value.put("foo", "bar");
        Map<String, String> nested = new HashMap<>();
        nested.put("nesty", "nestyvalue");
        value.put("mappy", nested);
        mapper.writeValue(out, value);
        out.close();
        InputStream in = out.toInputStream();
//        System.out.println("Serialized: '" + IOUtils.toString(in, "UTF-8") + "'");
//        in.reset();
        Map<String, Object> wtf = mapper.readValue(in, new TypeReference<Map<String, Object>>(){});
        System.out.println("Read map:" + wtf.keySet());
        System.out.println("map[foo].class:" + wtf.get("foo").getClass());
        System.out.println("map[foo].length:" + ((String)wtf.get("foo")).length());
        System.out.println("map[mappy].class:" + wtf.get("mappy").getClass());
        System.out.println("map[mappy].keys:" + ((Map)wtf.get("mappy")).keySet());
        System.out.println("map[mappy][nesty].class:" + ((Map)wtf.get("mappy")).get("nesty").getClass());
        System.out.println("map[foo].length:" + ((String)((Map)wtf.get("mappy")).get("nesty")).length());
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
