(ns puppetlabs.services.request-handler.request-handler-service
  (:require [puppetlabs.trapperkeeper.core :as tk]
            [puppetlabs.services.protocols.request-handler :as handler]
            [puppetlabs.services.request-handler.request-handler-core :as core]
            [puppetlabs.services.jruby.jruby-puppet-service :as jruby]
            [puppetlabs.trapperkeeper.services :as tk-services]
            [clojure.tools.logging :as log]
            [puppetlabs.services.protocols.jruby-puppet :as jruby-protocol]))

(defn- handle-request
  [request jruby-service config state]
  (jruby/with-jruby-puppet jruby-puppet jruby-service
    (try
      (core/handle-request request jruby-puppet config)
      (finally
        (let [new-state (swap! state
                               update-in
                               [:requests-since-last-flush]
                               inc)
              count     (:requests-since-last-flush new-state)]
          (log/warn "Updated requests since last flush to:" count)
          (log/warn "Max requests before flush:" (:max-requests-before-flush config))
          (when (> count (:max-requests-before-flush config))
            (jruby-protocol/flush-jruby-pool! jruby-service)
            (swap! state assoc-in [:requests-since-last-flush] 0)))))))

(tk/defservice request-handler-service
  handler/RequestHandlerService
  [[:PuppetServerConfigService get-config]
   [:JRubyPuppetService pool-size]]
  (init [this context]
        (assoc context
          :state
          (atom {:requests-since-last-flush 0
                 :config (core/config->request-handler-settings
                           (get-config) (pool-size))})))
  (handle-request
    [this request]
    (let [jruby-service (tk-services/get-service this :JRubyPuppetService)
          state  (-> (tk-services/service-context this) :state)
          config (:config @state)]
      (handle-request request jruby-service config state))))
