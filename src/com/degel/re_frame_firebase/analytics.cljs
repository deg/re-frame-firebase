(ns com.degel.re-frame-firebase.analytics
  (:require
  ["@firebase/analytics" :refer (getAnalytics logEvent)]
  [com.degel.re-frame-firebase.core :as core]))

(defn init
  []
  (swap! core/firebase-state assoc
         :analytics (-> @core/firebase-state
                        :app
                        getAnalytics)))

(defn log-effect
  [{:keys [event props]} _]
  (-> @core/firebase-state
       :analytics
       (logEvent (name event) (clj->js props))))
