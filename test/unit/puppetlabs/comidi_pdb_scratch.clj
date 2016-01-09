(ns puppetlabs.comidi-pdb-scratch
  (:require [clojure.test :refer :all]
            [puppetlabs.comidi :as cmdi]
            [ring.mock.request :as mock]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Fake impls of real PDB functions, just to test routes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def version :v4)

(def query-params ["query" "limit" "offset" "order_by" "include_total"])

(defn add-action-to-req
  [req action]
  (update-in req [:fake-query-actions] (fnil conj []) action))

(defn query-handler
  [version]
  (fn [req]
    (let [req (add-action-to-req req :query-handler)]
      {:status 200
       :body "QUERY HANDLER: STREAMING BODY"
       :fake-query-actions (:fake-query-actions req)})))

(defn restrict-query-to-entity
  [entity req]
  (add-action-to-req req (keyword (str "restrict-query-to-"  entity))))

(defn restrict-query-to-active-nodes
  [req]
  (add-action-to-req req :restrict-query-to-active-nodes))

(defn extract-query'
  [param-spec]
  (fn [req]
    (add-action-to-req req :extract-query')))

(defn node-status
  [api-version node options]
  {:status 200
   :body "NODE STATUS"})

(defn validate-query-params
  [params
   params-spec]
  params)

(defn restrict-fact-query-to-name
  [fact req]
  (add-action-to-req req (keyword (str "restrict-fact-query-to-name_"  fact))))

(defn restrict-fact-query-to-value
  [value req]
  (add-action-to-req req (keyword (str "restrict-fact-query-to-value_"  value))))

(defn restrict-query-to-node'
  [req]
  (add-action-to-req req (keyword (str "restrict-query-to-node'_"
                                       (get-in req [:route-params :node])))))

(defn wrap-with-parent-check''
  [app version parent route-param-key]
  (fn [req]
    (let [req (add-action-to-req req
                                 (keyword (str "wrap-with-parent-check''-" parent)))]
      (app req))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PDB Route Trees
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn facts-app
  [version]
  (let [param-spec {:optional query-params}]
    {""
     (comp (query-handler version)
           #(restrict-query-to-entity "facts" %)
           restrict-query-to-active-nodes
           (extract-query' param-spec))

     ["/" :fact]
     {"" (comp (query-handler version)
               #(restrict-query-to-entity "facts" %)
               (fn [{:keys [route-params] :as req}]
                 (restrict-fact-query-to-name (:fact route-params) req))
               restrict-query-to-active-nodes
               (extract-query' param-spec))

      ["/" :value]
      (comp (query-handler version)
            #(restrict-query-to-entity "facts" %)
            (fn [{:keys [route-params] :as req}]
              (restrict-fact-query-to-name (:fact route-params) req))
            (fn [{:keys [route-params] :as req}]
              (restrict-fact-query-to-value (:value route-params) req))
            restrict-query-to-active-nodes
            (extract-query' param-spec))}}))

(defn node-app
  [version]
  (let [param-spec {:optional query-params}]
    {"" (comp (query-handler version)
              #(restrict-query-to-entity "nodes" %)
              restrict-query-to-active-nodes
              (extract-query' param-spec))

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
                                  restrict-query-to-node'
                                  (extract-query' param-spec))))
        #(wrap-with-parent-check'' % version :node :node)))

      #_["/resources"]
      #_(second
       (cmdi/wrap-routes
        (cmdi/wrap-routes ["" (resources-app version)]
                          (fn [handler]
                            (comp handler
                                  restrict-query-to-node'
                                  (extract-query' param-spec))))
        #(wrap-with-parent-check'' % version :node :node)))}}))

(def v4-app
  {;"" (experimental-index-app version)
   "/facts" (facts-app version)
   ;"/edges" (comp (query-handler version)
   ;               restrict-query-to-active-nodes
   ;               #(restrict-query-to-entity "edges" %)
   ;               (extract-query' {:optional query-params}))
   ;"/factsets" (factset-app version)
   ;"/fact-names" (fact-names-app version)
   ;"/fact-contents"   (comp (query-handler version)
   ;                         #(restrict-query-to-entity "fact_contents" %)
   ;                         restrict-query-to-active-nodes
   ;                         (extract-query' {:optional query-params}))
   ;"/fact-paths" (create-paging-query-handler "fact_paths")

   "/nodes" (node-app version)
   ;"/environments" (environments-app version)
   ;
   ;
   ;"/resources" (resources-app version)
   ;"/catalogs" (catalog-app version)
   ;"/events" (events-app version)
   ;"/event-counts" (create-query-handler "event_counts" {:required ["summarize_by"]
   ;                                                      :optional (concat ["counts_filter" "count_by"
   ;                                                                         "distinct_resources" "distinct_start_time"
   ;                                                                         "distinct_end_time"]
   ;                                                                        query-params)})
   ;"/aggregate-event-counts" (create-query-handler "aggregate_event_counts"
   ;                                                {:required ["summarize_by"]
   ;                                                 :optional ["query" "counts_filter" "count_by"
   ;                                                            "distinct_resources" "distinct_start_time"
   ;                                                            "distinct_end_time"]})
   ;"/reports" (reports-app version)
   })

(def routes
  ["" {;"/v1" [[true (refuse-retired-api "v1")]]
       ;"/v2" [[true (refuse-retired-api "v2")]]
       ;"/v3" [[true (refuse-retired-api "v3")]]
       "/v4" v4-app}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Tests / test utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn request
  [uri]
  (mock/request :get uri))

(deftest test-routes
  (let [handler (cmdi/routes->handler routes)]
    (testing "/v4/nodes"
      (let [resp (handler (request "/v4/nodes"))]
        (is (= "QUERY HANDLER: STREAMING BODY" (:body resp)))
        (is (= [:extract-query'
                :restrict-query-to-active-nodes
                :restrict-query-to-nodes
                :query-handler]
               (:fake-query-actions resp)))))))