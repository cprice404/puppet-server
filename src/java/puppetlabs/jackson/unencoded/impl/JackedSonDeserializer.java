package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;

public class JackedSonDeserializer extends StdScalarDeserializer<InputStream> {
    private final InputStreamWrapper outWrapper;

    public JackedSonDeserializer(InputStreamWrapper outWrapper) {
        super(String.class);
        this.outWrapper = outWrapper;
    }

    @Override
    public InputStream deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            InputStream result = ((JackedSonParser)p).getBytesAsInputStream();
            if (outWrapper != null) {
                return outWrapper.wrap(result);
            } else {
                return result;
            }
        }
        throw new IllegalStateException("I dunno why my string deserializer is being called for something that is not a string!");
    }
}
