package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import puppetlabs.jackson.unencoded.InputStreamWrapper;

import java.io.IOException;
import java.io.InputStream;

public class JackedSonSerializer extends StdScalarSerializer<InputStream> {
    private final InputStreamWrapper inputWrapper;

    public JackedSonSerializer(InputStreamWrapper inputWrapper) {
        super(InputStream.class);
        this.inputWrapper = inputWrapper;
    }


    @Override
    public void serialize(InputStream value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // if we get here and the generator isn't our complementary one, we're
        // hosed.
        if (! (gen instanceof JackedSonGenerator)) {
            throw new IllegalStateException("JackedSonSerializer only works in combination with UnencodedInputStreamGenerator.");
        }
        InputStream source;
        if (inputWrapper != null) {
            source = inputWrapper.wrap(value);
        } else {
            source = value;
        }
        gen.writeBinary(source, -1);
    }
}
