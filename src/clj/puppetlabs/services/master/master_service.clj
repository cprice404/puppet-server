(ns puppetlabs.services.master.master-service
  (:require [clojure.tools.logging :as log]
            [puppetlabs.trapperkeeper.core :refer [defservice]]
            [puppetlabs.services.master.master-core :as core]
            [puppetlabs.puppetserver.certificate-authority :as ca]
            [compojure.core :as compojure]
            [puppetlabs.bidi :as pl-bidi]))

(defservice master-service
  [[:WebroutingService add-ring-handler get-route]
   [:PuppetServerConfigService get-config]
   [:RequestHandlerService handle-request]
   [:CaService initialize-master-ssl! retrieve-ca-cert!]]
  (init
   [this context]
   (core/validate-memory-requirements!)
   (let [path        (get-route this)
         config      (get-config)
         certname    (get-in config [:puppet-server :certname])
         localcacert (get-in config [:puppet-server :localcacert])
         settings    (ca/config->master-settings config)]

     (retrieve-ca-cert! localcacert)
     (initialize-master-ssl! settings certname)

     (log/info "Master Service adding a ring handler")
     (add-ring-handler
       this
       (pl-bidi/context-handler
        path
        (core/build-ring-handler handle-request))))
   context)
  (start
    [this context]
    (log/info "Puppet Server has successfully started and is now ready to handle requests")
    context))
