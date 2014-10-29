require 'puppet/server/environments'
require 'puppet/environments'

module Puppet::Server::Environments::Cached
  class FlushableTTLEntry < Puppet::Environments::Cached::Entry
    def initialize(wrapped, env)
      super env
      @wrapped = wrapped
      puts "CREATING AUTOFLUSH CACHE ENTRY FOR: #{env}"
      puts "AUTOFLUSH MODULEPATH: #{env.modulepath}"
      Puppet::Server::Config.environment_registry.register_environment(env.name, env.modulepath)
      # @ctime = Time.now
    end

    def expired?
      puts "AUTOFLUSH CHECKING EXPIRY FOR '#{value.name}'"
      Puppet::Server::Config.environment_registry.is_expired?(value.name) || @wrapped.expired?
      # false
      # puts "AUTOFLUSH EXPIRED? ctime: #{@ctime} (#{@ctime.class})"
      # mtime = Puppet::Server::Config.environment_registry.get_environment_modified_time(value.name).to_date
      # puts "GOT MTIME FROM REGISTRY: #{mtime} (#{mtime.class})2"
      # puts "AUTOFLUSH RETURNING EXPIRED? #{@ctime} <= #{mtime} ? #{@ctime <= mtime}"
      # @ctime <= mtime
    end
  end

  class FlushableTTLEntryFactory
    def initialize
      @ttl_factory = Puppet::Environments::Cached::TTLEntryFactory.new
    end

    def create_entry(env, conf)
      entry = @ttl_factory.create_entry(env, conf)
      FlushableTTLEntry.new(entry, env)
    end
  end

end