package com.puppetlabs.puppetserver;
import org.joda.time.DateTime;

public interface EnvironmentRegistry {
    public void registerEnvironment(String name, String[] dirs);
    public DateTime getEnvironmentModifiedTime(String name);
}
