package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;
import java.io.InputStream;

public class UnencodedInputStreamDeserializer extends StdScalarDeserializer<InputStream> {
    public UnencodedInputStreamDeserializer() {
        super(String.class);
    }

    @Override
    public InputStream deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return ((UnencodedInputStreamJsonParser)p).getBytesAsInputStream();
        }
        throw new IllegalStateException("I dunno why my string deserializer is being called for something that is not a string!");
    }
}
