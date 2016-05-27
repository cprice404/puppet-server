package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;
import java.io.InputStream;

public class UnencodedInputStreamSerializer extends StdScalarSerializer<InputStream> {
    public UnencodedInputStreamSerializer() {
        super(InputStream.class);
    }


    @Override
    public void serialize(InputStream value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // if we get here and the generator isn't our complementary one, we're
        // hosed.
        if (! (gen instanceof  UnencodedInputStreamJsonGenerator)) {
            throw new IllegalStateException("UnencodedInputStreamSerializer only works in combination with UnencodedInputStreamGenerator.");
        }
        gen.writeBinary(value, -1);
    }
}
