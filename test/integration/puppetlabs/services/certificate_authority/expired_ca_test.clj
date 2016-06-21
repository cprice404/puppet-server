(ns puppetlabs.services.certificate-authority.expired-ca-test
  (:require
    [clojure.test :refer :all]
    [puppetlabs.kitchensink.core :as ks]
    [puppetlabs.puppetserver.bootstrap-testutils :as bootstrap]
    [puppetlabs.ssl-utils.core :as ssl-utils]
    [puppetlabs.puppetserver.testutils :as testutils :refer
     [ca-cert localhost-cert localhost-key ssl-request-options http-get]]
    [puppetlabs.trapperkeeper.testutils.logging :as logutils]
    [schema.test :as schema-test]
    [me.raynes.fs :as fs]
    [cheshire.core :as json]
    [puppetlabs.http.client.sync :as http-client]
    [puppetlabs.ssl-utils.core :as utils]
    [clj-time.core :as time]
    [puppetlabs.puppetserver.certificate-authority :as ca]))

(def test-resources-dir
  "./dev-resources/puppetlabs/services/certificate_authority/expired_ca_test")

(use-fixtures :once
              schema-test/validate-schemas
              #_(testutils/with-puppet-conf (fs/file test-resources-dir "puppet.conf")))
;; cacert      (utils/sign-certificate
;; x500-name
;; private-key
;; serial
;; (:not-before validity)
;; (:not-after validity)
;; x500-name
;; public-key
;; ca-exts)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tests

(deftest generate-ca-cert
  (let [keypair     (utils/generate-key-pair 1024)
        public-key  (utils/get-public-key keypair)
        private-key (utils/get-private-key keypair)
        x500-name (utils/cn "foo_ca")
        serial 100
        ca-exts (ca/create-ca-extensions x500-name
                                         serial
                                         public-key)
        not-before (time/minus (time/now) (time/days 365))
        not-after (time/minus (time/now) (time/days 1))
        validity {:not-before (.toDate not-before)
                  :not-after  (.toDate not-after)}
        ca-cert (utils/sign-certificate
                  x500-name
                  private-key
                  serial
                  (:not-before validity)
                  (:not-after validity)
                  x500-name
                  public-key
                  ca-exts)]
    (ssl-utils/cert->pem! ca-cert (str "./expired_ca_cert.pem"))
    ))


(deftest ^:integration expired-ca-test
  (testing "an expired CA cert cannot sign certs"
    (bootstrap/with-puppetserver-running
      app
      {}
      (println "inside the expired-ca-test"))))
