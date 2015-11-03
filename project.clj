(defproject co.opsee/opsee-middleware "0.1.7-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[s3-wagon-private "1.1.2"]
            [lein-shell "0.4.1"]]
  :java-source-paths ["src"]
  :repositories [["snapshots" {:url "s3p://opsee-maven-snapshots/snapshot"
                               :username :env
                               :passphrase :env}]
                 ["releases" {:url "s3p://opsee-maven-snapshots/releases"
                              :username :env
                              :passphrase :env
                              :sign-releases false}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["shell" "git" "checkout" "master"]
                  ["shell" "git" "merge" "release"]
                  ["shell" "git" "push" "origin" "master"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.0.0"]]}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring-cors "0.1.6"]
                 [liberator "0.13"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.liquibase/liquibase-core "3.1.1"]
                 [cheshire "5.4.0"]
                 [clostache "0.6.1"]
                 [metosin/compojure-api "0.22.0" :exclusions [org.clojure/java.classpath hiccup clj-time joda-time]]
                 [c3p0/c3p0 "0.9.1.2"]
                 [com.google.protobuf/protobuf-java "3.0.0-alpha-3.1"]
                 [io.grpc/grpc-all "0.7.2"]
                 [org.bitbucket.b_c/jose4j "0.4.4"]
                 [org.clojure/tools.logging "0.3.1"]])
