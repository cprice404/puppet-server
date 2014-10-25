require 'puppet/server/environments/cached'
require 'puppet/environments'

class Puppet::Server::Environments::Cached::AutoFlushEntry < Puppet::Environments::Cached::Entry
  def initialize(value)
    super value
    puts "CREATING AUTOFLUSH CACHE ENTRY FOR: #{value}"
  end

  def expired?
    puts "AUTOFLUSH RETURNING EXPIRED?=true"
    true
  end
end