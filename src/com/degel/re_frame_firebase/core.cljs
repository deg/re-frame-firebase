;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [cljsjs.firebase]
   [com.degel.re-frame-firebase.helpers :refer [js->clj-tree success-failure-wrapper]]
   [com.degel.re-frame-firebase.specs]
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [reagent.core :as reagent]
   [reagent.ratom :as rv]
   [sodium.chrome-utils :as chrome]
   [sodium.re-utils :refer [<sub >evt event->fn sub->fn]]
   [sodium.utils :as utils]))


(s/def ::cache (s/nilable (s/keys)))

;;; Used mostly to register client handlers
(defonce firebase-state (atom {}))


(defn set-firebase-state [& {:keys [get-user-sub set-user-event default-error-handler]}]
  (swap! firebase-state assoc
         :set-user-fn           (event->fn set-user-event)
         :get-user-fn           (sub->fn get-user-sub)
         :default-error-handler (event->fn (or default-error-handler js/alert))))

;;; [TODO] Consider adding a default atom to hold the user state when :get-user-fn and
;;; and :set-user-fn are not defined. Need to do this carefully, so as not to cause any
;;; surprises for users who accidentally defined just one of the two callbacks.
(defn current-user []
  (when-let [handler (:get-user-fn @firebase-state)]
    (handler)))

(defn set-current-user [user]
  (when-let [handler (:set-user-fn @firebase-state)]
    (handler user)))

(defn default-error-handler []
  (:default-error-handler @firebase-state))


(defn- fb-ref [path]
  {:pre [(utils/validate :firebase/fb-path path)]}
  (.ref (js/firebase.database)
        (str/join "/" (clj->js path))))


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


(defn firebase-on-value-sub [app-db [_ {:keys [path on-failure]}]]
  (if path
    (let [ref (fb-ref path)
          ;; [TODO] Potential bug alert:
          ;;        We are caching the results, keyed only by path, and we clear
          ;;        the cache entry in :on-dispose.  I can imagine situations
          ;;        where this would be problematic if someone tried watching the
          ;;        same path from two code locations. If this becomes an issue, we
          ;;        might need to add an optional disambiguation argument to the
          ;;        subscription.
          ;;        Note that firebase itself seems to guard against this by using
          ;;        the callback itself as a unique key to .off.  We can't do that
          ;;        (modulo some reflection hack), since we use the id as part of
          ;;        the callback closure.
          id path
          callback #(>evt [::on-value-handler id (js->clj-tree %)])]
      (.on ref "value" callback (event->fn (or on-failure (default-error-handler))))
      (rv/make-reaction
       (fn [] (get-in @app-db [::cache id] []))
       :on-dispose #(do (.off ref "value" callback)
                        (>evt [::on-value-handler id nil]))))
    (do
      (console :error "Received null Firebase on-value request")
      (rv/make-reaction
       (fn []
         ;; Minimal dummy response, to avoid blowing up caller
         nil)))))

(re-frame/reg-event-db
 ::on-value-handler
 (fn [app-db [_ id value]]
   (if value
     (assoc-in app-db [::cache id] value)
     (update app-db ::cache dissoc id))))


