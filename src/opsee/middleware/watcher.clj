(ns opsee.middleware.watcher
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]
            [verschlimmbesserung.core :as v])
  (:import (java.net SocketTimeoutException MalformedURLException)))

(defn wait-index [client name index path opts]
  (loop [result (atom nil)]
    (try+
      (reset! result (v/get* client path (merge {:wait? true :wait-index index :timeout 30} opts)))

      (catch SocketTimeoutException _)
      (catch MalformedURLException _
        (Thread/sleep 10000))
      (catch [:errorCode 401] _ (reset! result true))
      (catch Exception ex
        (log/warn ex "caught exception in watcher" name)))
    (if-not @result
      (recur result)
      @result)))

(defn trigger-callback [client name callback path]
  (try
    (let [response (v/get* client path)]
      (log/debug "triggering callback for" name)
      (callback response)
      (-> response meta :etcd-index Integer/parseInt inc))
    (catch SocketTimeoutException _ 0)
    (catch MalformedURLException _ 0)
    (catch Exception ex
      (do
        (log/warn ex "caught exception triggering callback" name)
        0))))

(defn watcher [name etcd-url callback path opts]
  (fn []
    (let [client (v/connect etcd-url)]
      (log/info name "watcher started")
      (loop [index (trigger-callback client name callback path)]
        (wait-index client name index path opts)
        (recur (trigger-callback client name callback path))))))

(defn thread-never-die [f]
  (fn []
    (loop []
      (try
        (f)
        (catch Throwable ex (log/error ex "an error occurred in watcher thread " (-> (Thread/currentThread) .getName)))))))

(defn start
  ([name etcd-url callback path]
   (start name etcd-url callback path {}))
  ([name etcd-url callback path opts]
   (doto (Thread. (thread-never-die (watcher name etcd-url callback path opts)))
         (.setName name)
         .start)))