(ns opsee.middleware.auth
    (:require [clojure.tools.logging :as log]
      [cheshire.core :refer :all]
      [clojure.string :as string])
    (:import (java.util Base64)))

(defn token->login [token]
      (parse-string (->
                      (Base64/getDecoder)
                      (.decode token)
                      (String.)) keyword))

(defn authorized? [scheme token]
      (case (string/lower-case scheme)
            "basic" (try
                      (if-let [login (token->login token)]
                              [true, {:login login}])
                      (catch Exception e
                        (log/info "invalid token:" token "exception:" e)))
            [false nil]))
