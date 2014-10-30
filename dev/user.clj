(ns user)

(def jvm-puppet-conf
  {:global       {:logging-config       "/home/cprice/work/puppet/jvm-puppet/conf/logback.xml"}
   :os-settings  {:ruby-load-path       ["./ruby/puppet/lib" "./ruby/facter/lib"]}
   :jruby-puppet {:gem-home             "./target/jruby-gems"
                  :max-active-instances 2
                  :master-conf-dir      "/home/cprice/work/puppet/jvm-puppet/conf/master-conf"
                  :master-var-dir       "/home/cprice/work/puppet/jvm-puppet/conf/master-var"}
   :webserver    {:client-auth "want"
                  :ssl-host    "localhost"
                  :ssl-port    8140}
   :certificate-authority {:certificate-status {:client-whitelist []}}})

(defn repl
  []
  (load-file "./dev/user_repl.clj")
  (ns user-repl))