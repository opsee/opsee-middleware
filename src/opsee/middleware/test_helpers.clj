(ns opsee.middleware.test-helpers
  (:require [cheshire.core :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [opsee.middleware.pool :refer [pool]]
            [opsee.middleware.migrate :refer [migrate-db]])
  (:import (java.io FileNotFoundException)))

(defn slurp-from-classpath
  "Slurps a file from the classpath."
  [path]
  (if-let [url (io/resource path)]
    (slurp url)
    (throw (FileNotFoundException. path))))

(def db (atom nil))

(defn start-connection [config]
  (do
    (if-not @db (reset! db (pool (:db-spec config))))
    (migrate-db @db {:drop-all true :silent true}))
  @db)

(defn is-json [checker]
  (fn [actual]
    (if (instance? String actual)
      (checker (parse-string actual true))
      (checker (parse-stream actual true)))))
