(ns puppetlabs.puppetserver.bootstrap-testutils
  (:require [puppetlabs.trapperkeeper.config :as tk-config]
            [puppetlabs.trapperkeeper.bootstrap :as tk-bootstrap]
            [puppetlabs.trapperkeeper.testutils.bootstrap :as tk-testutils]
            [puppetlabs.kitchensink.core :as ks]))

(def dev-config-file
  "./dev/sample-configs/puppet-server.sample.conf")

(def dev-bootstrap-file
  "./dev/bootstrap.cfg")

(defmacro with-puppetserver-running
  [app config-overrides & body]
  `(let [config# (-> (tk-config/load-config dev-config-file)
                     (assoc-in [:global :logging-config] "./dev-resources/logback-test.xml")
                     (ks/deep-merge ~config-overrides))
         services# (tk-bootstrap/parse-bootstrap-config! dev-bootstrap-file)]
     (tk-testutils/with-app-with-config
       ~app
       services#
       config#
       ~@body)))
