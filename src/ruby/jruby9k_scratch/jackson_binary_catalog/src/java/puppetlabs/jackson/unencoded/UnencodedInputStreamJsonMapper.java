package puppetlabs.jackson.unencoded;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import puppetlabs.jackson.unencoded.impl.SkipTypeCheckModule;
import puppetlabs.jackson.unencoded.impl.UnencodedInputStreamDeserializer;
import puppetlabs.jackson.unencoded.impl.UnencodedInputStreamJsonFactory;
import puppetlabs.jackson.unencoded.impl.UnencodedInputStreamSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class UnencodedInputStreamJsonMapper {
    private final ObjectMapper mapper;

    private static final TypeReference TYPE_MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>(){};

    public UnencodedInputStreamJsonMapper() {
        // TODO: consider using inheritance rather than delegation?
        this.mapper = new ObjectMapper(new UnencodedInputStreamJsonFactory());
        SkipTypeCheckModule module =
                new SkipTypeCheckModule("UnencodedInputStreamModule",
                        new Version(0, 1, 0, null, "puppetlabs", "unencoded-mapper"));
        module.addSerializer(new UnencodedInputStreamSerializer());
        module.addDeserializerWithoutTypeCheck(String.class, new UnencodedInputStreamDeserializer());
        mapper.registerModule(module);
    }

    public void writeValue(OutputStream out, Object value) throws IOException {
        mapper.writeValue(out, value);
    }

    public Map<String, Object> readMapWithUnencodedInputStreams(InputStream in) throws IOException {
        return mapper.readValue(in, TYPE_MAP_STRING_OBJECT);
    }

}
