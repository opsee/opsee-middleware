(ns opsee.middleware.core
  (:require [clojure.tools.logging :as log]
            [cheshire.core :refer :all]
            [clojure.string :as str]
            [opsee.middleware.auth :as auth]
            [liberator.representation :refer [ring-response render-map-generic render-seq-generic]])
  (:import (java.sql BatchUpdateException)
           (java.io ByteArrayInputStream)))

(defn env [name]
  (System/getenv (.toUpperCase name)))

(defmacro if-env
  ([name positive]
   '(if-env name positive nil))
  ([name positive negative]
   '(if (env name)
      positive
      negative)))

(defmethod render-map-generic "application/json" [data _]
  (generate-string data))

(defmethod render-seq-generic "application/json" [data _]
  (generate-string data))

(defn scrub-keys [msg]
  (-> msg
      (str/replace #"\"access-key\":\".*?\"" (str "\"access-key\":\"XXXXXXX\""))
      (str/replace #"\"secret-key\":\".*?\"" (str "\"secret-key\":\"XXXXXXXXXXXX\""))))

(defn log-request [handler]
  (fn [request]
    (if-let [body-rdr (:body request)]
      (let [body (slurp body-rdr)
            req' (assoc request
                   :strbody body
                   :body (ByteArrayInputStream. (.getBytes body)))
            req'' (if-not (get-in req' [:headers "Content-Type"])
                    (assoc-in req' [:headers "Content-Type"] "application/json"))]
        (log/info "request:" (scrub-keys req''))
        (handler req''))
      (do (log/info "request:" (scrub-keys request))
          (handler request)))))

(defn log-response [handler]
  (fn [request]
    (let [response (handler request)
          response' (if (and
                          (instance? java.io.InputStream (:body response))
                          (.contains (get-in response [:headers "Content-Type"]) "application/json"))
                      (assoc response :body (slurp (:body response)))
                      response)]
      (log/info "response:" (scrub-keys response'))
      response')))

(defn log-and-error [ex]
  (log/error ex "problem encountered")
  {:status  500
   :headers {"Content-Type" "application/json"} `:body    (generate-string {:error (.getMessage ex)})})

(defn robustify-errors [^Exception ex]
  (if (instance? BatchUpdateException ex)
    (log-and-error (.getNextException ex))
    (log-and-error ex)))

(defn json-body [ctx]
  (if-let [body (get-in ctx [:request :strbody])]
    (parse-string body true)
    (if-let [in (get-in ctx [:request :body])]
      (parse-stream in true))))

(defn vary-origin [handler]
  (fn [request]
    (let [resp (handler request)]
      (assoc-in resp [:headers "Vary"] "origin"))))

(defn user-authorized? [ctx]
  (if-let [auth-header (get-in ctx [:request :headers "authorization"])]
    (let [[scheme slug] (str/split auth-header #" " 2)]
      (auth/authorized? scheme slug))))

(defn superuser-authorized? [ctx]
  (if-let [[answer {login :login}] (user-authorized? ctx)]
    (if (and answer (:admin login))
      [true, {:login login}])))

(defn authorized?
  "Determines whether a request has the correct authorization headers, and sets the login id in the ctx."
  ([fn-auth-level]
   (fn [ctx]
     (case (if (fn? fn-auth-level)
             (fn-auth-level ctx)
             fn-auth-level)
       :unauthenticated true
       :user (user-authorized? ctx)
       :superuser (superuser-authorized? ctx))))
  ([]
   (authorized? :user)))

(defn if-and-let*
  [bindings then-clause else-clause deshadower]
  (if (empty? bindings)
    then-clause
    `(if-let ~(vec (take 2 bindings))
       ~(if-and-let* (drop 2 bindings) then-clause else-clause deshadower)
       (let ~(vec (apply concat deshadower))
         ~else-clause))))

(defmacro if-and-let
  ([bindings then-clause]
   `(if-and-let ~bindings ~then-clause nil))
  ([bindings then-clause else-clause]
   (let [shadowed-syms (filter #(or ((or &env {}) %) (resolve %))
                         (filter symbol?
                           (tree-seq coll? seq (take-nth 2 bindings))))
         deshadower (zipmap shadowed-syms (repeatedly gensym))]
     `(let ~(vec (apply concat (map (fn [[k v]] [v k]) deshadower)))
        ~(if-and-let* bindings then-clause else-clause deshadower)))))

(defmacro try-let
  "A combination of try and let such that exceptions thrown in the binding or
   body can be handled by catch clauses in the body, and all bindings are
   available to the catch and finally clauses. If an exception is thrown while
   evaluating a binding value, it and all subsequent binding values will be nil.
   Example:
   (try-let [x (f a)
             y (f b)]
     (g x y)
     (catch Exception e (println a b x y e)))"
  {:arglists '([[bindings*] exprs* catch-clauses* finally-clause?])}
  [bindings & exprs]
  (when-not (even? (count bindings))
    (throw (IllegalArgumentException. "try-let requires an even number of forms in binding vector")))
  (let [names  (take-nth 2 bindings)
        values (take-nth 2 (next bindings))
        ex     (gensym "ex__")]
    `(let [~ex nil
           ~@(interleave names (repeat nil))
           ~@(interleave
               (map vector names (repeat ex))
               (for [v values]
                 `(if ~ex
                    [nil ~ex]
                    (try [~v nil]
                         (catch Throwable ~ex [nil ~ex])))))]
       (try
         (when ~ex (throw ~ex))
         ~@exprs))))


(defn slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (clojure.java.io/copy (clojure.java.io/input-stream x) out)
    (.toByteArray out)))
