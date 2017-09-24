(ns com.degel.re-frame-firebase.auth
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [com.degel.re-frame-firebase.core :as core]
   [re-frame.core :as re-frame]
   [sodium.re-utils :refer [>evt event->fn sub->fn]]))


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

(def ^:private sign-in-fns
  {:popup (memfn signInWithPopup auth-provider)
   :redirect (memfn signInWithRedirect auth-provider)})

(defn- oauth-sign-in
  [auth-provider opts]
  (let [{:keys [sign-in-method scopes custom-parameters]
         :or {sign-in-method :redirect}} opts
        auth (js/firebase.auth.)]

    (doseq [scope scopes]
      (.addScope auth-provider scope))

    (when custom-parameters
      (.setCustomParameters auth-provider (clj->js custom-parameters)))

    (if-let [sign-in (sign-in-fns sign-in-method)]
      (.catch
        (sign-in auth auth-provider)
        (core/default-error-handler))
      (>evt [(core/default-error-handler)
             (js/Error. (str "Unsupported sign-in-method: " sign-in-method ". Either :redirect or :popup are supported."))]))))


(defn google-sign-in
  [opts]
  ;; TODO: use Credential for mobile.
  (oauth-sign-in (js/firebase.auth.GoogleAuthProvider.) opts))


(defn facebook-sign-in
  [opts]
  (oauth-sign-in (js/firebase.auth.FacebookAuthProvider.) opts))


(defn twitter-sign-in
  [opts]
  (oauth-sign-in (js/firebase.auth.TwitterAuthProvider.) opts))


(defn github-sign-in
  [opts]
  (oauth-sign-in (js/firebase.auth.GithubAuthProvider.) opts))


(defn sign-out []
  (.catch
   (.signOut (js/firebase.auth))
   (core/default-error-handler)))
