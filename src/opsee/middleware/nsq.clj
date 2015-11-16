(ns opsee.middleware.nsq
  (:import (com.github.brainlag.nsq.lookup DefaultNSQLookup)
           (java.io IOException)
           (com.github.brainlag.nsq ServerAddress)
           (com.google.common.collect Sets)))

(defn ensure-int [val]
  (if (= String (class val))
    (try
      (Integer/parseInt val)
      (catch Exception _ 0))
    val))

(defn nsq-lookup [lookup-addrs produce-addr]
  (let [proxy (proxy [DefaultNSQLookup] []
                (lookup [topic]
                  (try
                    (proxy-super lookup topic)
                    (catch IOException _
                      (let [set (Sets/newHashSet)]
                        (.add set (ServerAddress. (:host produce-addr) (ensure-int (:port produce-addr))))
                        set)))))]
    (doseq [lookup-addr (flatten (list lookup-addrs))]
      (.addLookupAddress proxy (:host lookup-addr) (ensure-int (:port lookup-addr))))
    proxy))