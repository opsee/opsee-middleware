(ns opsee.middleware.t-auth
    (:use midje.sweet)
  (:require [opsee.middleware.auth :as auth]
            [clojure.string :as str]))

(def session-header (str "eyJhY3RpdmUiOnRydWUsImlkIjo4LCJlbWFpbCI6ImNsaWZmQGxlYW5pbnRvLml0IiwidmVyaWZpZWQiOnRydWUsImN1c"
                         "3RvbWVyX2lkIjoiMTU0YmE1N2EtNTE4OC0xMWU1LTgwNjctOWI1ZjJkOTZkY2UxIiwiZXhwIjoxNzU2NzgwOTQxLCJzdW"
                         "IiOiJjbGlmZkBsZWFuaW50by5pdCIsImlhdCI6MTQ0MTIxMjE0MSwibmFtZSI6ImNsaWZmIiwiYWRtaW4iOmZhbHNlfQ=="))

(facts "about sessions"
  (fact "we can turn a tokin' into a clojer"
    (let [login (auth/token->login "basic" session-header)]
      (:email login) => "cliff@leaninto.it"))

  (fact "gives u a user w a basic tokin'"
    (let [[authed session] (auth/authorized? "Basic" session-header)]
      authed => true
      (:email (:login session)) => "cliff@leaninto.it"))

  (fact "gives u a tokin for dependent services"
    (let [token (auth/login->token {:email "cliff"})
          decode (auth/token->login "Basic" (last (str/split token #" ")))]
      (:email decode) => "cliff")))
