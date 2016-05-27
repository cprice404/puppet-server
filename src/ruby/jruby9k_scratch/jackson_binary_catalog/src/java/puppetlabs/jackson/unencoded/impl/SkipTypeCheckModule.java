package puppetlabs.jackson.unencoded.impl;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.module.SimpleDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class SkipTypeCheckModule extends SimpleModule {
    public SkipTypeCheckModule(String name, Version version) {
        super(name, version);
    }

    public SkipTypeCheckModule addDeserializerWithoutTypeCheck(Class type, JsonDeserializer deser) {
        if (_deserializers == null) {
            _deserializers = new SimpleDeserializers();
        }
        _deserializers.addDeserializer(type, deser);
        return this;
    }
}
