;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx

(ns com.degel.re-frame-firebase
  (:require
   [re-frame.core :as re-frame]
   [com.degel.re-frame-firebase.core :as core]
   [com.degel.re-frame-firebase.auth :as auth]
   [com.degel.re-frame-firebase.database :as database]
   [com.degel.re-frame-firebase.firestore :as firestore]))

;;; Write a value to Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#set
;;;
;;; Example FX:
;;; {:firebase/write [:path [:my :data]
;;;                   :value 42
;;;                   :on-success #(prn "Write succeeded")
;;;                   :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/write database/write-effect)


;;; Write a value to a Firebase list.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#push
;;;
;;; Example FX:
;;; {:firebase/push [:path [:my :collection]
;;;                  :value "Hello world"
;;;                  :on-success #(prn "Push succeeded")
;;;                  :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/push database/push-effect)


;;; Asynch one-time read of a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#once
;;;
;;; Example FX:
;;; {:firebase/read-once [:path [:my :data]
;;;                      :on-success [:got-my-data]
;;;                      :on-failure [:firebase-error]]}
;;;
(re-frame/reg-fx :firebase/read-once database/once-effect)


;;; Dispatch a vector of firebase effects
;;;
;;;
;;; Example FX:
;;; {:firebase/multi [[:firebase/write {:path ,,,}]
;;;                   [:firebase/push {:path ,,,}]
;;;                   ,,,]}
;;;
(re-frame/reg-fx
 :firebase/multi
 (fn [effects]
   (run! (fn [[event-type args]]
           (case event-type
             :firebase/write        (database/write-effect args)
             :firebase/push         (database/push-effect args)
             :firebase/read-once    (database/once-effect args)
             :firestore/delete      (firestore/delete-effect args)
             :firestore/set         (firestore/set-effect args)
             :firestore/update      (firestore/update-effect args)
             :firestore/add         (firestore/add-effect args)
             :firestore/batch-write (firestore/batch-write-effect args)
             :firestore/get         (firestore/get-effect args)
             :firestore/on-snapshot (firestore/on-snapshot args)
             (js/alert "Internal error: unknown firebase effect: " event-type " (" args ")")))
         effects)))


;;; Watch a value in Firebase.
;;; See https://firebase.google.com/docs/reference/js/firebase.database.Reference#on
;;;
;;; Subscription:
;;; (<sub {:path [:my :data]
;;;        :on-failure [:firebase-error]})
;;;
(re-frame/reg-sub-raw :firebase/on-value  database/on-value-sub)


;;; Login to firebase, using one of OAuth providers
;;;
;;; Accepts a map of the following options:
;;;
;;; - :sign-in-method  either :redirect (default) or :popup mode
;;;
;;; - :scopes       a sequence of additional OAuth scopes; supported for following auth providers:
;;;       Google:   https://developers.google.com/identity/protocols/googlescopes
;;;       Facebook: https://developers.facebook.com/docs/facebook-login/permissions
;;;       GitHub:   https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/
;;;
;;; - :custom-parameters  check auth providers documentation for supported values:
;;;       Google:   https://firebase.google.com/docs/reference/js/firebase.auth.GoogleAuthProvider#setCustomParameters
;;;       Facebook: https://firebase.google.com/docs/reference/js/firebase.auth.FacebookAuthProvider#setCustomParameters
;;;       Twitter:  https://firebase.google.com/docs/reference/js/firebase.auth.TwitterAuthProvider#setCustomParameters
;;;       GitHub:   https://firebase.google.com/docs/reference/js/firebase.auth.GithubAuthProvider#setCustomParameters
;;;
;;; Example usage:
;;; FX:
;;; {firebase/google-sign-in {:sign-in-method :popup
;;;                           :scopes ["https://www.googleapis.com/auth/contacts.readonly"
;;;                                    "https://www.googleapis.com/auth/calendar.readonly"]
;;;                           :custom-parameters {"login_hint" "user@example.com"}}}
;;;
(re-frame/reg-fx :firebase/google-sign-in   auth/google-sign-in)
(re-frame/reg-fx :firebase/facebook-sign-in auth/facebook-sign-in)
(re-frame/reg-fx :firebase/twitter-sign-in  auth/twitter-sign-in)
(re-frame/reg-fx :firebase/github-sign-in   auth/github-sign-in)

;;; Login to firebase using email/password authentication
;;; or registers a new user for email/password authentication.
;;;
;;; Accepts a map with :email and :password
;;;
;;; Example:
;;; FX:
;;; {:firebase/email-sign-in {:email "test@github.com" :password "myverysecretpassword"}}
;;; or to create a new user:
;;; {:firebase/email-create-user {:email "newuser@github.com" :password "anotherverysecretpassword"}}
;;;
(re-frame/reg-fx :firebase/email-sign-in     auth/email-sign-in)
(re-frame/reg-fx :firebase/email-create-user auth/email-create-user)


;;; Logout
;;;
;;; FX:
;;; {firebase/sign-out []}
;;;
(re-frame/reg-fx :firebase/sign-out auth/sign-out)



;;; Monitor connection status
;;;
(re-frame/reg-sub
 :firebase/connection-state
 (fn [_ _]
   (re-frame/subscribe [:firebase/on-value {:path [:.info :connected]}]))
 (fn [connected? _]
   {:firebase/connected? (= connected? true)}))


(re-frame/reg-fx :firestore/delete firestore/delete-effect)
(re-frame/reg-fx :firestore/set firestore/set-effect)
(re-frame/reg-fx :firestore/update firestore/update-effect)
(re-frame/reg-fx :firestore/add firestore/add-effect)
(re-frame/reg-fx :firestore/batch-write firestore/batch-write-effect)
(re-frame/reg-fx :firestore/get firestore/get-effect)
(re-frame/reg-fx :firestore/on-snapshot firestore/on-snapshot)
(re-frame/reg-sub-raw :firestore/on-snapshot firestore/on-snapshot-sub)


;;; Start library and register callbacks.
;;;
;;;
;;; In Iron style, most of the parameters can be either a function or a
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
  (core/initialize-app firebase-app-info)
  (auth/init-auth))
