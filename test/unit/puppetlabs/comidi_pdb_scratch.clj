(ns puppetlabs.comidi-pdb-scratch
  (:require [clojure.test :refer :all]
            [puppetlabs.comidi :as comidi]
            [ring.mock.request :as mock]))

(defn node-app
  [version]
  (let [param-spec {:optional paging/query-params}]
    {"" (comp (http-q/query-handler version)
              #(http-q/restrict-query-to-entity "nodes" %)
              http-q/restrict-query-to-active-nodes
              (http-q/extract-query' param-spec))

     ["/" :node]
     {"" (-> (fn [{:keys [globals route-params]}]
               (node-status version
                            (:node route-params)
                            (select-keys globals [:scf-read-db :url-prefix :warn-experimental])))
             ;; Being a singular item, querying and pagination don't really make
             ;; sense here
             (validate-query-params {}))

      ["/facts"]
      (second
       (cmdi/wrap-routes
        (cmdi/wrap-routes ["" (facts-app version)]
                          (fn [handler]
                            (comp handler
                                  http-q/restrict-query-to-node'
                                  (http-q/extract-query' param-spec))))
        #(wrap-with-parent-check'' % version :node :node)))

      ["/resources"]
      (second
       (cmdi/wrap-routes
        (cmdi/wrap-routes ["" (resources-app version)]
                          (fn [handler]
                            (comp handler
                                  http-q/restrict-query-to-node'
                                  (http-q/extract-query' param-spec))))
        #(wrap-with-parent-check'' % version :node :node)))}}))

(def v4-app
  {"" (experimental-index-app version)
   "/facts" (facts-app version)
   "/edges" (comp (http-q/query-handler version)
                  http-q/restrict-query-to-active-nodes
                  #(http-q/restrict-query-to-entity "edges" %)
                  (http-q/extract-query' {:optional paging/query-params}))
   "/factsets" (factset-app version)
   "/fact-names" (fact-names-app version)
   "/fact-contents"   (comp (http-q/query-handler version)
                            #(http-q/restrict-query-to-entity "fact_contents" %)
                            http-q/restrict-query-to-active-nodes
                            (http-q/extract-query' {:optional paging/query-params}))
   "/fact-paths" (create-paging-query-handler "fact_paths")

   "/nodes" (node-app version)
   "/environments" (environments-app version)


   "/resources" (resources-app version)
   "/catalogs" (catalog-app version)
   "/events" (events-app version)
   "/event-counts" (create-query-handler "event_counts" {:required ["summarize_by"]
                                                         :optional (concat ["counts_filter" "count_by"
                                                                            "distinct_resources" "distinct_start_time"
                                                                            "distinct_end_time"]
                                                                           paging/query-params)})
   "/aggregate-event-counts" (create-query-handler "aggregate_event_counts"
                                                   {:required ["summarize_by"]
                                                    :optional ["query" "counts_filter" "count_by"
                                                               "distinct_resources" "distinct_start_time"
                                                               "distinct_end_time"]})
   "/reports" (reports-app version)})

(def routes
  ["" {"/v1" [[true (refuse-retired-api "v1")]]
       "/v2" [[true (refuse-retired-api "v2")]]
       "/v3" [[true (refuse-retired-api "v3")]]
       "/v4" v4-app}])

(defn request
  [uri]
  (mock/request :get uri))

(deftest test-routes
  (let [handler (comidi/routes->handler routes)]
    (is (= :foo (handler (request "/foo/bar"))))))