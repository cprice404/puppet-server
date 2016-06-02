package puppetlabs.jackson.unencoded;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import puppetlabs.jackson.unencoded.impl.SkipTypeCheckModule;
import puppetlabs.jackson.unencoded.impl.JackedSonDeserializer;
import puppetlabs.jackson.unencoded.impl.JackedSonFactory;
import puppetlabs.jackson.unencoded.impl.JackedSonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class JackedSonMapper {
    private final ObjectMapper mapper;

    private static final TypeReference TYPE_OBJECT = new TypeReference<Object>(){};
    private static final TypeReference TYPE_MAP_STRING_OBJECT = new TypeReference<Map<String, Object>>(){};

    public JackedSonMapper() {
        this(null, null);
    }

    public JackedSonMapper(InputStreamWrapper inputWrapper,
                           InputStreamWrapper outputWrapper) {
        // TODO: consider using inheritance rather than delegation?
        this.mapper = new ObjectMapper(new JackedSonFactory());
        SkipTypeCheckModule module =
                new SkipTypeCheckModule("UnencodedInputStreamModule",
                        new Version(0, 1, 0, null, "puppetlabs", "unencoded-mapper"));
        module.addSerializer(new JackedSonSerializer(inputWrapper));
        module.addDeserializerWithoutTypeCheck(String.class, new JackedSonDeserializer(outputWrapper));
        mapper.registerModule(module);
    }

    public void writeValue(OutputStream out, Object value) throws IOException {
        mapper.writeValue(out, value);
    }

    public Map<String, Object> readMapWithUnencodedInputStreams(InputStream in) throws IOException {
        return mapper.readValue(in, TYPE_MAP_STRING_OBJECT);
    }

    public Object readValue(InputStream in) throws IOException {
        return mapper.readValue(in, TYPE_OBJECT);
    }

}
