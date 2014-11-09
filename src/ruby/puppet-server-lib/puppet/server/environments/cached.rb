require 'puppet/server/environments'
require 'puppet/environments'

class Puppet::Server::Environments::Cached
  class CacheExpirationService
    def initialize(environment_registry)
      @environment_registry = environment_registry
    end

    def created(env)
      @environment_registry.register_environment(env.name, env.modulepath)
    end

    def expired?(env_name)
      @environment_registry.is_expired?(env_name)
    end

    def evicted(env_name)
      @environment_registry.remove_environment(env_name)
    end
  end
end