package puppetlabs.jackson.unencoded;

import java.io.InputStream;

public interface InputStreamWrapper {
    InputStream wrap(InputStream inputStream);
}
