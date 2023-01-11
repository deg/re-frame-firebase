;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017-8, David Goldfarb

(defproject tallyfor/re-frame-firebase "0.10.1"
  :description "A re-frame wrapper around firebase"
  :url "https://github.com/deg/re-frame-firebase"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.439"]
                 [cljsjs/firebase "7.5.0-0"] ;;"5.7.3-1"
                 [re-frame "0.10.6"]
                 [com.degel/iron "0.4.0"]
                 [lein-pprint             "1.3.2"]
                 [lein-cljsbuild "1.1.8"]
                 [lein-bump-version "0.1.6"]
                 [lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  ;; run lein install with LEIN_SNAPSHOTS_IN_RELEASE=true lein install
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :cljsbuild {:builds {}} ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413
  :plugins [[lein-npm "0.6.2"]]
  :npm {:dependencies [[source-map-support "0.5.6"]]}
  :profiles {:dev      {:dependencies [[clj-stacktrace "0.2.8"]
                                       [binaryage/devtools "0.9.10"]
                                       [org.clojure/tools.namespace "1.1.0"]]}}
  :source-paths ["src" "target/classes"]
  ;; Change your environment variables (maybe editing .zshrc or .bashrc) to have:
  ;; export LEIN_USERNAME="pdelfino"
  ;; export LEIN_PASSWORD="your-personal-access-token-the-same-used-on-.npmrc"
  ;; LEIN_PASSWORD should use the same Token used by .npmrc
  ;; Also, do "LEIN_SNAPSHOTS_IN_RELEASE=true lein install" or edit your .zshrc:
  ;; export LEIN_SNAPSHOTS_IN_RELEASE=true
  :repositories {"releases"  {:url           "https://maven.pkg.github.com/tallyfor/*"
                              :username      :env/LEIN_USERNAME ;; change your env
                              :password      :env/LEIN_PASSWORD}}

  :pom-addition [:distribution-management [:repository [:id "github"]
                                           [:name "GitHub Packages"]
                                           [:url "https://maven.pkg.github.com/tallyfor/re-frame-firebase"]]]
  :clean-targets ["out" "release"]
  :target-path "target")
