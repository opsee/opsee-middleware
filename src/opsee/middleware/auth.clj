(ns opsee.middleware.auth
    (:require [clojure.tools.logging :as log]
      [cheshire.core :refer :all]
      [clojure.string :as string])
  (:import (java.util Base64)
           (org.jose4j.keys AesKey)
           (org.jose4j.jwe JsonWebEncryption)))

(def secret (atom nil))

(defn set-secret! [sekrit]
  (reset! secret (AesKey. sekrit)))

(defmulti token->login (fn [scheme _] (keyword (string/lower-case scheme))))

(defmethod token->login :basic [_ token]
  (parse-string (->
                  (Base64/getDecoder)
                  (.decode token)
                  (String.)) keyword))

(defmethod token->login :bearer [_ token]
  (-> (doto
        (JsonWebEncryption.)
        (.setKey @secret)
        (.setCompactSerialization token))
      .getPayload
      (parse-string keyword)))

(defn authorized? [scheme token]
  (try
    (if-let [login (token->login scheme token)]
      [true {:login login}]
      [false nil])
    (catch Exception e (log/warn e "invalid token:" scheme token)
                        [false nil])))

(defn login->token [login]
  (str "Basic "
       (-> (Base64/getEncoder)
           (.encodeToString (generate-cbor login)))))