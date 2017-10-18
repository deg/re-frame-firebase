;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(defproject com.degel/re-frame-firebase "0.3.0"
  :description "A re-frame wrapper around firebase"
  :url "https://github.com/deg/re-frame-firebase"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta2"]
                 [org.clojure/clojurescript "1.9.946"]
                 [cljsjs/firebase "4.4.0-0"]
                 [re-frame "0.10.2"]
                 [reagent "0.7.0"]
                 [com.degel/sodium "0.6.1"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.2"]]
  :npm {:dependencies [[source-map-support "0.4.0"]]}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target")
