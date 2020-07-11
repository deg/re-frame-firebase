(ns com.degel.re-frame-firebase.functions
  (:require
   [firebase.functions :as firebase-functions]
   [clojure.walk :as w]))

(defn call-effect [options]
  (let [{:keys [cfn-name data on-success on-error]} options
        cfn (.httpsCallable (.functions js/firebase) cfn-name)]
    (.catch
     (.then
      (cfn (clj->js data))
      #(on-success (-> (.. % -data)
                       js->clj
                       w/keywordize-keys)))
     #(on-error %))))

