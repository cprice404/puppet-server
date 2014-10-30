require 'puppet/server/environments'
require 'puppet/environments'

class Puppet::Server::Environments::Cached
  class CacheExpirationService
    def initialize()
      puts "CREATING CACHE EXPIRATION SERVICE"
    end

    def created(env)
      puts "REGISTERING CACHED ENVIRONMENT: #{env}"
      puts "CACHED ENV MODULEPATH: #{env.modulepath}"
      Puppet::Server::Config.environment_registry.register_environment(env.name, env.modulepath)
      # @ctime = Time.now
    end

    def expired?(env_name)
      puts "CACHE SERVICE CHECKING EXPIRY FOR '#{env_name}'"
      Puppet::Server::Config.environment_registry.is_expired?(env_name)
      # false
      # puts "AUTOFLUSH EXPIRED? ctime: #{@ctime} (#{@ctime.class})"
      # mtime = Puppet::Server::Config.environment_registry.get_environment_modified_time(value.name).to_date
      # puts "GOT MTIME FROM REGISTRY: #{mtime} (#{mtime.class})2"
      # puts "AUTOFLUSH RETURNING EXPIRED? #{@ctime} <= #{mtime} ? #{@ctime <= mtime}"
      # @ctime <= mtime
    end

    def evicted(env_name)
      puts "CACHE SERVICE EVICTING CACHE ENTRY FOR #{env_name}"
    end
  end
end