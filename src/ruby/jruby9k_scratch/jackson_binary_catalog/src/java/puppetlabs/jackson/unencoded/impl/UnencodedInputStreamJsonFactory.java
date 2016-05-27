package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.MappingJsonFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;

public class UnencodedInputStreamJsonFactory extends MappingJsonFactory {
    // NOTE: The overridden methods here were originally copied from the
    // parent class, so if anything is broken it's worth comparing to those.


    // We're overriding all of the createGenerator and createParser signatures.
    // We expect that there is only one signature of each that will ever be
    // called for our use cases, so we implement the rest to throw exceptions
    // to highlight that we are going down unexpected code paths.

    //***************************************************************************
    //* Signatures that we support
    //***************************************************************************

    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
//            return _createUnencodedGenerator(_decorate(out, ctxt), ctxt);

            UnencodedInputStreamJsonGenerator gen =
                    new UnencodedInputStreamJsonGenerator(ctxt,
                    _generatorFeatures, _objectCodec, _decorate(out, ctxt));
            if (_characterEscapes != null) {
                gen.setCharacterEscapes(_characterEscapes);
            }
            SerializableString rootSep = _rootValueSeparator;
            if (rootSep != DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR) {
                gen.setRootValueSeparator(rootSep);
            }
            return gen;
        } else {
            throw new IllegalStateException("In the world of Jackpson, we expect everyone to say they're UTF-8 even though they are not.");
        }
    }


    @Override
    public JsonParser createParser(InputStream in) throws IOException, JsonParseException {
        IOContext ctxt = _createContext(in, false);
        int inputEnd = 0;
        int inputPtr = 0;

        byte[] inputBuffer = ctxt.allocReadIOBuffer();

        ByteQuadsCanonicalizer can = _byteSymbolCanonicalizer.makeChild(_factoryFeatures);
        return new UnencodedInputStreamJsonParser(ctxt, _parserFeatures,
                _decorate(in, ctxt),_objectCodec, can,
                inputBuffer, inputPtr, inputEnd, true);
    }


    //***************************************************************************
    //* Signatures that we do NOT support
    //***************************************************************************

    @Override
    public JsonGenerator createGenerator(OutputStream out) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonGenerator createGenerator(Writer w) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(char[] content) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(String content) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(File f) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }


    @Override
    public JsonParser createParser(Reader r) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }

    @Override
    public JsonParser createParser(URL url) throws IOException, JsonParseException {
        throw new IllegalStateException("Not implemented!");
    }
}
