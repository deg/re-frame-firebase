(ns com.degel.re-frame-firebase.functions
  (:require
   [clojure.walk :as w]
   [com.degel.re-frame-firebase.core :as core]
   ["@firebase/functions" :refer (httpsCallable getFunctions)]))

(defn call-effect [options]
  (let [{:keys [cfn-name data on-success on-error]} options
        cfn (-> @core/firebase-state
                :app
                getFunctions
                (httpsCallable cfn-name))]
    (.catch
     (.then
      (cfn (clj->js data))
      #(on-success (-> (.. % -data)
                       js->clj
                       w/keywordize-keys)))
     #(on-error %))))
