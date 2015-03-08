(ns puppetlabs.services.ca.certificate-authority-service
  (:require [clojure.tools.logging :as log]
            [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.puppetserver.certificate-authority :as ca]
            [puppetlabs.services.ca.certificate-authority-core :as core]
            [puppetlabs.services.protocols.ca :refer [CaService]]
            [puppetlabs.bidi :as pl-bidi]))

(tk/defservice certificate-authority-service
  CaService
  [[:PuppetServerConfigService get-config get-in-config]
   [:WebroutingService add-ring-handler get-route]]
  (init
   [this context]
   (let [path           (get-route this)
         settings       (ca/config->ca-settings (get-config))
         puppet-version (get-in-config [:puppet-server :puppet-version])]
     (ca/initialize! settings)
     (log/info "CA Service adding a ring handler")
     (add-ring-handler
       this
       (core/wrap-middleware
         puppet-version
         (pl-bidi/context-handler
           path
           (core/web-context settings)))
      #_(pl-bidi/context-handler
        path
        (core/build-ring-handler settings puppet-version))))
   context)

  (initialize-master-ssl!
   [this master-settings certname]
   (let [settings (ca/config->ca-settings (get-config))]
     (ca/initialize-master-ssl! master-settings certname settings)))

  (retrieve-ca-cert!
    [this localcacert]
    (ca/retrieve-ca-cert! (get-in-config [:puppet-server :cacert])
                          localcacert)))
