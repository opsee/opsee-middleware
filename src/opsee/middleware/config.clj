(ns opsee.middleware.config
  (:require [clostache.parser :refer :all]
            [clojure.tools.logging :as log]
            [opsee.middleware.auth :as auth]
            [opsee.middleware.core :as core]
            [clojure.walk :refer [keywordize-keys]]
            [cheshire.core :refer :all]))

(def db-env ["DB_HOST" "DB_PORT" "DB_NAME" "DB_USER" "DB_PASS"])

(defn- find-addr [env]
  (let [key (some (fn [[k _]] (re-matches #"POSTGRESQL_PORT_\d+_TCP_ADDR" k)) env)]
    (get env key)))

(defn- find-port [env]
  (let [key (some (fn [[k _]] (re-matches #"POSTGRESQL_PORT_\d+_TCP_PORT" k)) env)]
    (get env key)))

(defn- read-system-env []
  (->> (System/getenv)
       (into {})))

(defn- docker-knockout [env defaults]
  (let [port (find-port env)
        addr (find-addr env)]
    (reduce (fn [env db-param]
              (let [alt-key (str "POSTGRESQL_ENV_" db-param)
                    default (get defaults db-param)
                    alt-val (get env alt-key default)]
                (if (get env db-param)
                  env
                  (assoc env db-param (or alt-val default)))))
              (assoc env "POSTGRESQL_ENV_DB_HOST" addr
                         "POSTGRESQL_ENV_DB_PORT" port)
              db-env)))

(defn config
  ([filename] (config filename {}))
  ([filename defaults]
    (let [env (keywordize-keys (docker-knockout (read-system-env) defaults))
          contents (render (slurp filename) env)]
      (when (:DEBUG_CONFIG env)
        (log/info contents))
      (let [cfg (parse-string contents true)]
        (auth/set-secret! (core/slurp-bytes (:secret cfg)))
        (core/init-yeller! (:yeller cfg))
        cfg))))