(require '[cljs.build.api :as b])

(b/watch "src"
  {:main 'com.degel.re-frame-firebase.core
   :output-to "out/re_frame_firebase.js"
   :output-dir "out"})
