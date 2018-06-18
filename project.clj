;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(defproject com.degel/re-frame-firebase "0.6.0-SNAPSHOT"
  :description "A re-frame wrapper around firebase"
  :url "https://github.com/deg/re-frame-firebase"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.312"]
                 [cljsjs/firebase "5.0.4-1"]
                 [re-frame "0.10.5"]
                 [reagent "0.8.1"]
                 [com.degel/iron "0.4.0"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :cljsbuild {:builds {}} ; prevent https://github.com/emezeske/lein-cljsbuild/issues/413
  :plugins [[lein-npm "0.6.2"]]
  :npm {:dependencies [[source-map-support "0.5.6"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target")
