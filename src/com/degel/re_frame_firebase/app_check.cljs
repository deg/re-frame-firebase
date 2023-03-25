(ns com.degel.re-frame-firebase.app-check
  (:require
   ["@firebase/app-check" :refer (initializeAppCheck ReCaptchaV3Provider)]
  [com.degel.re-frame-firebase.core :as core]))

(defn init
  [settings]
  (when (:debug-provider settings)
    (set! js/FIREBASE_APPCHECK_DEBUG_TOKEN true))
  (swap! core/firebase-state assoc
         :app-check (initializeAppCheck (:app @core/firebase-state)
                                        (clj->js
                                         {:provider (ReCaptchaV3Provider. (:site-key settings))
                                          :isTokenAutoRefreshEnabled true}))))
