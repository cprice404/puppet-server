require 'puppet/server/environments/cached'
require 'puppet/environments'

class Puppet::Server::Environments::Cached::AutoFlushEntry < Puppet::Environments::Cached::Entry
  def initialize(value)
    super value
    puts "CREATING AUTOFLUSH CACHE ENTRY FOR: #{value}"
    @ctime = Time.now
  end

  def expired?
    puts "AUTOFLUSH EXPIRED? ctime: #{@ctime} (#{@ctime.class})"
    mtime = Puppet::Server::Config.environment_mtime_registry.get_env_mtime(value)
    puts "AUTOFLUSH RETURNING EXPIRED? #{@ctime} <= #{mtime} ? #{@ctime <= mtime}"
    @ctime <= mtime
  end
end