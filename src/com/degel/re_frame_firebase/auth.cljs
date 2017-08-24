(ns com.degel.re-frame-firebase.auth
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [com.degel.re-frame-firebase.core :as core]
   [re-frame.core :as re-frame]
   [sodium.re-utils :refer [event->fn sub->fn]]))


(defn- user
  "Extract interesting details from the Firebase JS user object."
  [firebase-user]
  (when firebase-user
    {:uid           (.-uid firebase-user)
     :provider-data (.-providerData firebase-user)
     :display-name  (.-displayName firebase-user)
     :photo-url     (.-photoURL firebase-user)
     :email         (-> firebase-user .-providerData first .-email)}))


(defn- is-user-equal [google-user firebase-user]
  (and
    firebase-user
    (some
      #(and (= (.-providerId %) js/firebase.auth.GoogleAuthProvider.PROVIDER_ID)
            (= (.-uid %) (.getId (.getBasicProfile google-user))))
      (:provider-data firebase-user))))


(defn ^:export onSignIn [google-user]
  (when (not (is-user-equal google-user (core/current-user)))
    (.catch
      (.signInWithCredential
        (js/firebase.auth)
        (js/firebase.auth.GoogleAuthProvider.credential
          (.-id_token (.getAuthResponse google-user))))
      (core/default-error-handler))))


(defn- init-auth []
  (.onAuthStateChanged
   (js/firebase.auth)
   (fn auth-state-changed [firebase-user]
     (core/set-current-user (user firebase-user)))
   (core/default-error-handler)))


(defn google-sign-in []
  ;; TODO: use Credential for mobile.
  (.signInWithRedirect
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))


(defn sign-out []
  (.catch
   (.signOut (js/firebase.auth))
   (core/default-error-handler)))
