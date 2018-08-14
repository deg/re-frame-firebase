(defproject firestore "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript  "1.10.238"]
                 [reagent  "0.8.1"]
                 [re-frame "0.10.5"]
                 [com.degel/re-frame-firebase "0.7.0"]
                 [com.degel/iron "0.4.0"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel  "0.5.16"]]

  :hooks [leiningen.cljsbuild]

  :profiles {:dev {:cljsbuild
                   {:builds {:client {:figwheel     {:on-jsload "firestore.core/run"}
                                      :compiler     {:main "firestore.core"
                                                     :asset-path "js"
                                                     :optimizations :none
                                                     :source-map true
                                                     :source-map-timestamp true}}}}}

             :prod {:cljsbuild
                    {:builds {:client {:compiler    {:optimizations :advanced
                                                     :elide-asserts true
                                                     :pretty-print false}}}}}}

  :figwheel {:repl false}

  :clean-targets ^{:protect false} ["resources/public/js"]

  :cljsbuild {:builds {:client {:source-paths ["src" "../../src/"]
                                :compiler     {:output-dir "resources/public/js"
                                               :output-to  "resources/public/js/client.js"}}}})
