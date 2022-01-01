;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.auth
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [iron.re-utils :refer [>evt]]
   [com.degel.re-frame-firebase.core :as core]
   ["@firebase/auth" :refer (getAuth onAuthStateChanged getRedirectResult updateProfile)]))


(defn- user
  "Extract interesting details from the Firebase JS user object."
  [firebase-user]
  (when firebase-user
    {:uid           (.-uid firebase-user)
     :provider-data (.-providerData firebase-user)
     :display-name  (.-displayName firebase-user)
     :photo-url     (.-photoURL firebase-user)
     :email         (let [provider-data (.-providerData firebase-user)]
                      (when-not (empty? provider-data)
                        (-> provider-data first .-email)))}))

(defn- set-user
  [firebase-user]
  (-> firebase-user
      (user)
      (core/set-current-user)))

(defn- init-auth []

  (onAuthStateChanged (getAuth) set-user (core/default-error-handler))

  (-> (getAuth)
      getRedirectResult
      (.then (fn on-user-credential [user-credential]
               (when user-credential
                 (-> user-credential
                     (.-user)
                     set-user))))
      (.catch (core/default-error-handler))))


(def ^:private sign-in-fns
  {:popup (memfn signInWithPopup auth-provider)
   :redirect (memfn signInWithRedirect auth-provider)})

(defn- maybe-link-with-credential
  [pending-credential user-credential]
  (when (and pending-credential user-credential)
    (when-let [firebase-user (.-user user-credential)]
      (-> firebase-user
          (.linkWithCredential pending-credential)
          (.catch (core/default-error-handler))))))

(defn- oauth-sign-in
  [auth-provider opts]
  (let [{:keys [sign-in-method scopes custom-parameters link-with-credential]
         :or {sign-in-method :redirect}} opts]

    (doseq [scope scopes]
      (.addScope ^js auth-provider scope))

    (when custom-parameters
      (.setCustomParameters ^js auth-provider (clj->js custom-parameters)))

    (if-let [sign-in (sign-in-fns sign-in-method)]
      (-> (getAuth)
          (sign-in auth-provider)
          (.then (partial maybe-link-with-credential link-with-credential))
          (.catch (core/default-error-handler)))
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


(defn email-sign-in [{:keys [email password]}]
  (-> (getAuth)
      (.signInWithEmailAndPassword email password)
      (.then set-user)
      (.catch (core/default-error-handler))))


(defn email-create-user [{:keys [email password]}]
  (-> (getAuth)
      (.createUserWithEmailAndPassword email password)
      (.then set-user)
      (.catch (core/default-error-handler))))


(defn anonymous-sign-in [opts]
  (-> (getAuth)
      (.signInAnonymously)
      (.then set-user)
      (.catch (core/default-error-handler))))


(defn custom-token-sign-in [{:keys [token]}]
  (-> (getAuth)
      (.signInWithCustomToken token)
      (.then set-user)
      (.catch (core/default-error-handler))))


(defn init-recaptcha [{:keys [on-solve container-id]}]
  (let [recaptcha (js/firebase.auth.RecaptchaVerifier.
                   container-id
                   (clj->js {:size     "invisible"
                             :callback #(re-frame/dispatch on-solve)}))]
    (swap! core/firebase-state assoc
           :recaptcha-verifier recaptcha)))


(defn phone-number-sign-in [{:keys [phone-number on-send]}]
  (if-let [verifier (:recaptcha-verifier @core/firebase-state)]
    (-> (getAuth)
        (.signInWithPhoneNumber phone-number verifier)
        (.then (fn [confirmation]
                 (when on-send
                   (re-frame/dispatch on-send))
                 (swap! core/firebase-state assoc
                        :recaptcha-confirmation-result confirmation)))
        (.catch (core/default-error-handler)))
    (.warn js/console "Initialise reCaptcha first")))


(defn phone-number-confirm-code [{:keys [code]}]
  (if-let [confirmation (:recaptcha-confirmation-result @core/firebase-state)]
    (-> confirmation
        (.confirm code)
        (.then set-user)
        (.catch (core/default-error-handler)))
    (.warn js/console "reCaptcha confirmation missing")))


(defn sign-out
  [{:keys [on-success on-error]}]
  (-> (getAuth)
      (.signOut)
      (.then on-success)
      (.catch #(on-error (-> % js->clj .-message)))))

(defn update-profile
  [{:keys [profile on-success on-error]}]
  (-> (getAuth)
      (.-currentUser)
      (updateProfile (clj->js profile))
      (.then on-success)
      (.catch #(on-error (-> % js->clj .-message)))))

(defn update-email
  [{:keys [email on-success on-error]}]
  (-> (getAuth)
      (.-currentUser)
      (.updateEmail email)
      (.then on-success)
      (.catch #(on-error (-> % js->clj .-message)))))

(defn send-email-verification
  [{:keys [action-code-settings on-success on-error]}]
  (-> (getAuth)
      (.-currentUser)
      (.sendEmailVerification (clj->js action-code-settings))
      (.then on-success)
      (.catch #(on-error (-> % js->clj .-message)))))

(defn apply-action-code
  [{:keys [action-code on-success on-error]}]
  (-> (getAuth)
      (.applyActionCode action-code)
      (.then on-success)
      (.catch #(on-error (-> % js->clj .-message)))))
