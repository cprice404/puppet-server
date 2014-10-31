require 'puppet'
require 'puppet/server'
require 'puppet/server/jvm_profiler'
require 'puppet/server/http_client'

require 'java'
java_import com.puppetlabs.certificate_authority.CertificateAuthority
java_import java.io.FileReader

class Puppet::Server::Config

  def self.initialize_settings(puppet_server_config)
    if puppet_server_config.has_key?("profiler")
      @profiler = Puppet::Server::JvmProfiler.new(puppet_server_config["profiler"])

    end
    @environment_registry = puppet_server_config["environment_registry"]
    Puppet::Server::HttpClient.initialize_settings(puppet_server_config)
  end

  def self.ssl_context
    # Initialize an SSLContext for use during HTTPS client requests.
    # Do this lazily due to startup-ordering issues - to give the CA
    # service time to create these files before they are referenced here.
    unless @ssl_context
      @ssl_context = CertificateAuthority.pems_to_ssl_context(
          FileReader.new(Puppet[:hostcert]),
          FileReader.new(Puppet[:hostprivkey]),
          FileReader.new(Puppet[:localcacert]))
    end
    @ssl_context
  end

  def self.profiler
    @profiler
  end

  def self.environment_registry
    @environment_registry
  end
end
