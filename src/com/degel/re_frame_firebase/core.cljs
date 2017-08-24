;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [reagent.ratom :as rv]
   [sodium.utils :as utils]
   [sodium.chrome-utils :as chrome]
   [sodium.re-utils :refer [<sub >evt]]))

(enable-console-print!)
(println "Hello world!")


;;; Built on ideas and code from
;;; http://timothypratley.blogspot.co.il/2016/07/reacting-to-changes-with-firebase-and.html
;;; and https://github.com/timothypratley/voterx


;;; TODO
;;; - Move to own project, when stable. Comments below suggest file breaks


;;; ================================================================
;;; Specs - should move to separate file
;;; ================================================================

(s/def ::fb-path (s/coll-of (s/or :string string? :keyword keyword?) :into []))
(s/def ::app-db #(= reagent.ratom/RAtom (type %)))
(s/def ::vec-or-fn (s/or :event-or-sub vector? :function fn?))

;;; ================================================================
;;; Helpers - should move to separate file
;;; ================================================================

(defn- fb-ref [path]
  {:pre [(utils/validate ::fb-path path)]}
  (.ref (js/firebase.database)
        (str/join "/" (clj->js path))))


(defn- vec->fn [vec-or-fn key-fn]
  {:pre [(utils/validate (s/nilable ::vec-or-fn) vec-or-fn)]
   :post (fn? %)}
  (if (vector? vec-or-fn)
    #(key-fn (conj vec-or-fn %))
    vec-or-fn))

(defn event->fn [event-or-fn] (vec->fn event-or-fn >evt))
(defn sub->fn   [sub-or-fn]   (vec->fn sub-or-fn   <sub))


(defn- js->clj-tree [x]
  (-> (.val x)
      js->clj
      clojure.walk/keywordize-keys))


(defn success-failure-wrapper [on-success on-failure]
  {:pre [(utils/validate (s/nilable ::vec-or-fn) on-success)
         (utils/validate (s/nilable ::vec-or-fn) on-failure)]
   :post (fn? %)}
  (let [on-success (event->fn on-success)
        on-failure (event->fn on-failure)]
    (fn [err]
      (cond (nil? err) (when on-success (on-success))
            on-failure (on-failure err)
            :else      (js/console.error "Firebase error:" err)))))


;;; ================================================================
;;; Auth API (for now, just Google Auth)
;;; ================================================================

(declare firebase-write-effect)

(defonce firebase-state (atom {}))

(defn- is-user-equal [google-user firebase-user]
  (and
    firebase-user
    (some
      #(and (= (.-providerId %) js/firebase.auth.GoogleAuthProvider.PROVIDER_ID)
            (= (.-uid %) (.getId (.getBasicProfile google-user))))
      (:provider-data firebase-user))))

(defn ^:export onSignIn [google-user]
  (when (not (is-user-equal google-user ((:get-user-fn @firebase-state))))
    (.catch
      (.signInWithCredential
        (js/firebase.auth)
        (js/firebase.auth.GoogleAuthProvider.credential
          (.-id_token (.getAuthResponse google-user))))
      (fn [error]
        ;; [TODO] What should be error handler? Presumably, need to hold the
        ;; handler globally; maybe set up in init.
        (js/alert error)))))

(defn- user [firebase-user]
  {:uid           (.-uid firebase-user)
   :provider-data (.-providerData firebase-user)
   :display-name  (.-displayName firebase-user)
   :photo-url     (.-photoURL firebase-user)
   :email         (-> firebase-user .-providerData first .-email)})

(defn- init-auth []
  (.onAuthStateChanged
   (js/firebase.auth)
   (fn auth-state-changed [firebase-user]
     (swap! firebase-state assoc :current-uid (.-uid firebase-user))
     ((:set-user-fn @firebase-state)
      (if (.-uid firebase-user)
        (user firebase-user)
        nil)))
   (fn auth-error [error]
     (js/alert error))))

(defn init [firebase-app-info {:keys [sub-get-user event-set-user]}]
  (swap! firebase-state assoc
         :set-user-fn (event->fn event-set-user)
         :get-user-fn (sub->fn sub-get-user))
  (js/firebase.initializeApp firebase-app-info)
  (init-auth))


(defn google-sign-in []
  ;; TODO: use Credential for mobile.
  (.signInWithRedirect
    (js/firebase.auth.)
    (js/firebase.auth.GoogleAuthProvider.)))

(defn sign-out []
  ;; TODO: add then/error handlers
  (.signOut (js/firebase.auth))
  ((:set-user-fn @firebase-state) nil))

(re-frame/reg-fx :firebase/google-sign-in google-sign-in)
(re-frame/reg-fx :firebase/sign-out sign-out)

;;; ================================================================
;;; Read/write API
;;; ================================================================

(defn- firebase-write-effect [{:keys [path value on-success on-failure]}]
  (.set (fb-ref path)
        (clj->js value)
        (success-failure-wrapper on-success on-failure)))


(defn- firebase-push-effect [{:keys [path value on-success on-failure]}]
  (.push (fb-ref path)
         (clj->js value)
         (success-failure-wrapper on-success on-failure)))


(defn- firebase-once-effect [{:keys [path on-success on-failure]}]
  (.once (fb-ref path)
         "value"
         #((event->fn on-success) (js->clj-tree %))
         #((event->fn on-failure) %)))


(defonce local-id-num (atom 0))

(defn local-id [path]
  (str "ID-" (swap! local-id-num inc) "-" path))

(defn firebase-on-value-sub [app-db [_ path]]
  (let [ref (fb-ref path)
        id (local-id path) ;;; ID to disambiguate multiple watches on same
                           ;;; node. (Firebase uses the handler to do this,
                           ;;; which we could use too. But, this is better
                           ;;; for debugging, at very little cost)
        callback #(>evt [::on-value-handler id (js->clj-tree %)])]
    (.on ref "value" callback #(js/alert %))
    (rv/make-reaction
     (fn [] (get-in @app-db [::cache id] []))
     :on-dispose #(do (.off ref "value" callback)
                      (>evt [::on-value-handler id nil])))))

(re-frame/reg-event-db
 ::on-value-handler
 (fn [app-db [_ id value]]
   (if value
     (assoc-in app-db [::cache id] value)
     (update app-db ::cache dissoc id))))


(re-frame/reg-fx      :firebase/write     firebase-write-effect)
(re-frame/reg-fx      :firebase/push      firebase-push-effect)
(re-frame/reg-fx      :firebase/read-once firebase-once-effect)
(re-frame/reg-sub-raw :firebase/on-value  firebase-on-value-sub)
