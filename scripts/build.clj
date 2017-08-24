(require '[cljs.build.api :as b])

(println "Building ...")

(let [start (System/nanoTime)]
  (b/build "src"
    {:main 'com.degel.re-frame-firebase.core
     :output-to "out/re_frame_firebase.js"
     :output-dir "out"
     :verbose true})
  (println "... done. Elapsed" (/ (- (System/nanoTime) start) 1e9) "seconds"))


