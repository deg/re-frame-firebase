;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx


(ns com.degel.re-frame-firebase
  (:require
   [com.degel.re-frame-firebase.auth :as auth]
   [com.degel.re-frame-firebase.core :as core]
   [re-frame.core :as re-frame]))

(enable-console-print!)
(println "Re-frame-firebase loaded")


;;; Write a value to Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#set
;;;
;;; FX:
;;; {:firebase/write [:path [:my :data]
;;;                   :value 42
;;;                   :on-success #(prn "Write succeeded")
;;;                   :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/write core/firebase-write-effect)


;;; Write a value to a Firebase list.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#push
;;;
;;; FX:
;;; {:firebase/push [:path [:my :collection]
;;;                  :value "Hello world"
;;;                  :on-success #(prn "Push succeeded")
;;;                  :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/push core/firebase-push-effect)


;;; Asynch one-time read of a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#once
;;;
;;; FX:
;;; {:firebase/read-once [:path [:my :data]
;;;                      :on-success [:got-my-data]
;;;                      :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/read-once core/firebase-once-effect)


;;; Dispatch a vector of firebase effects
;;;
;;;
;;; FX:
;;; {:firebase/multi [[:firebase/write {:path ,,,}]
;;;                   [:firebase/push {:path ,,,}]
;;;                   ,,,]}
;;;
(re-frame/reg-fx
 :firebase/multi
 (fn [effects]
   (run! (fn [[event-type args]]
           (case event-type
             :firebase/write    (core/firebase-write-effect args)
             :firebase/push     (core/firebase-push-effect args)
             :firebase/read-once (core/firebase-once-effect args)
             (js/alert "Internal error: unknown firebase effect: " event-type " (" args ")")))
         effects)))


;;; Watch a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#on
;;;
;;; Subscription:
;;; (<sub {:path [:my :data]
;;;        :on-failure [:firebase-error]})
;;;
(re-frame/reg-sub-raw :firebase/on-value  core/firebase-on-value-sub)


;;; Login to firebase, using one of OAuth providers
;;;
;;; Accepts a map of the following options:
;;;
;;; - :sign-in-method      either :redirect (default) or :popup mode
;;;
;;; - :scopes              a sequence of additional OAuth scopes; supported for following auth providers:
;;;                        Google: https://developers.google.com/identity/protocols/googlescopes
;;;                        Facebook: https://developers.facebook.com/docs/facebook-login/permissions
;;;                        GitHub: https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/
;;;
;;; - :custom-parameters   check auth providers documentation for supported values:
;;;                        Google: https://firebase.google.com/docs/reference/js/firebase.auth.GoogleAuthProvider#setCustomParameters
;;;                        Facebook: https://firebase.google.com/docs/reference/js/firebase.auth.FacebookAuthProvider#setCustomParameters
;;;                        Twitter: https://firebase.google.com/docs/reference/js/firebase.auth.TwitterAuthProvider#setCustomParameters
;;;                        GitHub: https://firebase.google.com/docs/reference/js/firebase.auth.GithubAuthProvider#setCustomParameters
;;;
;;; Example usage:
;;; FX:
;;; {firebase/google-sign-in {:sign-in-method :popup
;;;                           :scopes ["https://www.googleapis.com/auth/contacts.readonly"
;;;                                    "https://www.googleapis.com/auth/calendar.readonly"]
;;;                           :custom-parameters {"login_hint" "user@example.com"}}}
;;;
(re-frame/reg-fx :firebase/google-sign-in auth/google-sign-in)
(re-frame/reg-fx :firebase/facebook-sign-in auth/facebook-sign-in)
(re-frame/reg-fx :firebase/twitter-sign-in auth/twitter-sign-in)
(re-frame/reg-fx :firebase/github-sign-in auth/github-sign-in)


;;; Logout
;;;
;;; FX:
;;; {firebase/sign-out []}
;;;
(re-frame/reg-fx :firebase/sign-out auth/sign-out)



;;; Start library and register callbacks.
;;;
;;;
;;; In Sodium style, most of the parameters can be either a function or a
;;; re-frame event/sub vector. If there is a parameter, it will passed to the
;;; function or conj'd onto the vector.
;;;
;;; - :firebase-app-info - Firebase application credentials. This is the one
;;;   parameter that takes a map:
;;;    {:apiKey "MY-KEY-MY-KEY-MY-KEY-MY-KEY"
;;;     :authDomain "my-app.firebaseapp.com"
;;;     :databaseURL "https://my-app.firebaseio.com"
;;;     :storageBucket "my-app.appspot.com"}
;;;
;;; - :set-user-event - Function or re-frame event that will be called back
;;;     to receive and store the user object from us, when login succeeds.
;;;     This object is a map that includes several fields that we need, plus
;;;     the following that may be useful to the calling app:
;;;       :display-name - The user's full name
;;;       :email - The user's email address
;;;       :photo-url - The user's photo
;;;       :uid - The user's unique id, used by Firebase.
;;;
;;; - :get-user-sub - Function or re-frame subscription vector that this
;;;   library will use to access the user object stored by :set-user-event
;;;
;;; - :default-error-handler - Function or re-frame event that will be called
;;;   to handle any otherwise unhandled errors.
;;;
(defn init [& {:keys [firebase-app-info
                      get-user-sub
                      set-user-event
                      default-error-handler]}]
  (core/set-firebase-state :get-user-sub          get-user-sub
                           :set-user-event        set-user-event
                           :default-error-handler default-error-handler)
  (js/firebase.initializeApp (clj->js firebase-app-info))
  (auth/init-auth))
