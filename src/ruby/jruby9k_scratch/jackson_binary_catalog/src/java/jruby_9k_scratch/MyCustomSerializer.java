package jruby_9k_scratch;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.json.UTF8StreamJsonParser;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MyCustomSerializer extends StdScalarSerializer<String> {
    public MyCustomSerializer() {
        super(String.class);
    }

//    public static class NotActuallyAString extends String {
//
//        @Override
//        public int read() throws IOException {
//            throw new IllegalStateException("Not implemented.");
//        }
//    }

    public static class LessSimpleModule extends SimpleModule {
        public LessSimpleModule(String name, Version version) {
            super(name, version);
        }

        public LessSimpleModule addDeserializerWithoutTypeCheck(Class type, JsonDeserializer deser) {
            if (_deserializers == null) {
                _deserializers = new SimpleDeserializers();
            }
            _deserializers.addDeserializer(type, deser);
            return this;
        }
    }

    public static class MyUTF8StreamJsonParser extends UTF8StreamJsonParser {

        public MyUTF8StreamJsonParser(IOContext ctxt, int features, InputStream in, ObjectCodec codec, ByteQuadsCanonicalizer sym, byte[] inputBuffer, int start, int end, boolean bufferRecyclable) {
            super(ctxt, features, in, codec, sym, inputBuffer, start, end, bufferRecyclable);
        }

        private InputStream _myReturnString() throws IOException {
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

        public InputStream getBytesAsInputStream() throws IOException {
            if (_currToken == JsonToken.VALUE_STRING) {
                if (_tokenIncomplete) {
                    _tokenIncomplete = false;
                    return _myReturnString(); // only strings can be incomplete
                }
//                return _textBuffer.contentsAsString();
            }
//            return _getText2(_currToken);
            throw new IllegalStateException("Not implemented!");
        }
    }

    public static class MyCustomDeserializer extends StdScalarDeserializer<InputStream> {
        protected MyCustomDeserializer() {
            super(String.class);
        }

        // since 2.6, slightly faster lookups for this very common type
        @Override
        public boolean isCachable() { return true; }

        @Override
        public InputStream deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            if (p.hasToken(JsonToken.VALUE_STRING)) {
//                return p.getBinaryValue();
                return ((MyUTF8StreamJsonParser)p).getBytesAsInputStream();
//                return IOUtils.toString(
//                        ((MyUTF8StreamJsonParser)p).getBytesAsInputStream(),
//                        "UTF-8");
            }
            throw new IllegalStateException("I dunno why my string deserializer is being called for something that is not a string!");
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
//            return super.createParser(in);
            IOContext ctxt = _createContext(in, false);
            int inputEnd = 0;
            int inputPtr = 0;

            byte[] inputBuffer = ctxt.allocReadIOBuffer();

            ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
            return new MyUTF8StreamJsonParser(ctxt, _parserFeatures,
                    _decorate(in, ctxt),_objectCodec, can,
                    inputBuffer, inputPtr, inputEnd, true);
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
        LessSimpleModule testModule = new LessSimpleModule("MyModule", new Version(1, 0, 0, null, "foo", "foo"));
        testModule.addSerializer(new MyCustomSerializer()); // assuming serializer declares correct class to bind to
        testModule.addDeserializerWithoutTypeCheck(String.class, new MyCustomDeserializer());
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
//        System.out.println("map[foo].length:" + ((String)wtf.get("foo")).length());
        System.out.println("map[foo].length:" + IOUtils.toString((InputStream)wtf.get("foo"), "UTF-8").length());
        System.out.println("map[mappy].class:" + wtf.get("mappy").getClass());
        System.out.println("map[mappy].keys:" + ((Map)wtf.get("mappy")).keySet());
        System.out.println("map[mappy][nesty].class:" + ((Map)wtf.get("mappy")).get("nesty").getClass());
//        System.out.println("map[foo].length:" + ((String)((Map)wtf.get("mappy")).get("nesty")).length());
        System.out.println("map[foo].length:" + IOUtils.toString((InputStream)((Map)wtf.get("mappy")).get("nesty"), "UTF-8").length());
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
