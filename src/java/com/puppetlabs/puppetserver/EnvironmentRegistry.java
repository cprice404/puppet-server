package com.puppetlabs.puppetserver;

public interface EnvironmentRegistry {
    public void registerEnvironment(String name, String[] dirs);
    public boolean isExpired(String name);
}
