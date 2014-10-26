require 'puppet/server/environments/cached'
require 'puppet/environments'

class Puppet::Server::Environments::Cached::AutoFlushEntry < Puppet::Environments::Cached::Entry
  def initialize(env)
    super env
    puts "CREATING AUTOFLUSH CACHE ENTRY FOR: #{env}"
    puts "AUTOFLUSH MODULEPATH: #{env.modulepath}"
    Puppet::Server::Config.environment_registry.register_environment(env.name, env.modulepath)
    # @ctime = Time.now
  end

  def expired?
    puts "AUTOFLUSH CHECKING EXPIRY FOR '#{value.name}'"
    Puppet::Server::Config.environment_registry.is_expired?(value.name)
    # false
    # puts "AUTOFLUSH EXPIRED? ctime: #{@ctime} (#{@ctime.class})"
    # mtime = Puppet::Server::Config.environment_registry.get_environment_modified_time(value.name).to_date
    # puts "GOT MTIME FROM REGISTRY: #{mtime} (#{mtime.class})2"
    # puts "AUTOFLUSH RETURNING EXPIRED? #{@ctime} <= #{mtime} ? #{@ctime <= mtime}"
    # @ctime <= mtime
  end
end