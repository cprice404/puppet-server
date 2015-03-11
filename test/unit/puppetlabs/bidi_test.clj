(ns puppetlabs.bidi-test
  (require [clojure.test :refer :all]
           [puppetlabs.bidi :as pl-bidi]))

(deftest route-metadata-test
  (testing "route metadata includes ordered list of routes and lookup by handler"
    (let [routes ["" [[["/foo/" :foo]
                       :foo-handler]
                      [["/bar/" :bar]
                       {:get :bar-handler}]]]
          expected-foo-meta {:path '("" "/foo/" :foo)
                             :request-method :any}
          expected-bar-meta {:path '("" "/bar/" :bar)
                             :request-method :get}]
      (is (= (pl-bidi/route-metadata routes)
             {:routes [expected-foo-meta
                       expected-bar-meta]
              :handlers {:foo-handler expected-foo-meta
                         :bar-handler expected-bar-meta}})))))

