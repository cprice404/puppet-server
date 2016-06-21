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
    [puppetlabs.http.client.sync :as http-client]))

(def test-resources-dir
  "./dev-resources/puppetlabs/services/certificate_authority/expired_ca_test")

(use-fixtures :once
              schema-test/validate-schemas
              (testutils/with-puppet-conf (fs/file test-resources-dir "puppet.conf")))
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

(deftest ^:integration expired-ca-test
  (testing "an expired CA cert cannot sign certs"
    (bootstrap/with-puppetserver-running
      app
      {}
      (println "inside the expired-ca-test"))))
