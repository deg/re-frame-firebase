(ns com.degel.re-frame-firebase.firestore
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core :as re-frame]
   [reagent.ratom :as ratom :refer [make-reaction]]
   [iron.re-utils :as re-utils :refer [<sub >evt event->fn sub->fn]]
   [iron.utils :as utils]
   [firebase.app :as firebase-app]
   [firebase.firestore :as firebase-firestore]
   [com.degel.re-frame-firebase.core :as core]
   [com.degel.re-frame-firebase.specs :as specs]
   [com.degel.re-frame-firebase.helpers :refer [promise-wrapper]]))


;; Parsing/Type Conversion
(defn clj->DocumentReference [path]
  {:pre [(utils/validate ::specs/path-document path)]}
  (.doc (js/firebase.firestore)
        (str/join "/" (clj->js path))))


(defn clj->CollectionReference [path]
  {:pre [(utils/validate ::specs/path-collection path)]}
  (.collection (js/firebase.firestore)
               (str/join "/" (clj->js path))))


(defn PathReference->clj [reference]
  (loop [ref reference
         result '()]
    (if ref
      (recur (.-parent ref) (conj result (.-id ref)))
      (vec result))))

(defn SnapshotMetadata->clj [metadata]
  {:from-cache (.-fromCache metadata)
   :has-pending-writes (.-hasPendingWrites metadata)})

(defn DocumentSnapshot->clj
  ([doc]
   (DocumentSnapshot->clj doc nil))
  ([doc snapshot-options]
   (DocumentSnapshot->clj doc snapshot-options nil))
  ([doc snapshot-options expose-objects]
   (DocumentSnapshot->clj doc snapshot-options expose-objects nil))
  ([doc snapshot-options expose-objects sure-exists]
   {:data (when (or sure-exists (.-exists doc))
            (js->clj (.data doc (clj->js (or snapshot-options {})))))
    :id (.-id doc)
    :metadata (SnapshotMetadata->clj (.-metadata doc))
    :ref (PathReference->clj (.-ref doc))
    :object (when expose-objects doc)}))

(defn DocumentChange->clj
  ([change] (DocumentChange->clj change nil))
  ([change snapshot-options] (DocumentChange->clj change snapshot-options nil))
  ([change snapshot-options expose-objects]
   {:doc (DocumentSnapshot->clj (.-doc change) (clj->js (or snapshot-options {})) expose-objects true)
    :new-index (.-newIndex change)
    :old-index (.-oldIndex change)
    :type (.-type change)
    :object (when expose-objects change)}))

(defn QuerySnapshot->clj
  ([query]
   (QuerySnapshot->clj query nil))
  ([query snapshot-options]
   (QuerySnapshot->clj query snapshot-options nil))
  ([query snapshot-options doc-changes]
   (QuerySnapshot->clj query snapshot-options doc-changes nil))
  ([query snapshot-options doc-changes expose-objects]
   {:docs (if (pos? (.-size query))
            (map #(DocumentSnapshot->clj % snapshot-options expose-objects true)
                 (.-docs query))
            [])
    :metadata (SnapshotMetadata->clj (.-metadata query))
    :size (.-size query)
    :doc-changes (when doc-changes
                   (if (pos? (.-size query))
                     (map #(DocumentChange->clj % snapshot-options expose-objects)
                          (.docChanges query (clj->js (or snapshot-options {}))))
                     []))
    :object (when expose-objects query)}))

(defn- document-parser-wrapper [callback snapshot-options expose-objects]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback
    #((re-utils/event->fn callback)
      (DocumentSnapshot->clj % snapshot-options expose-objects false))))

(defn- collection-parser-wrapper [callback snapshot-options doc-changes expose-objects]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback
    #((re-utils/event->fn callback)
      (QuerySnapshot->clj % snapshot-options doc-changes expose-objects))))

(defn- reference-parser-wrapper [callback]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback #((re-utils/event->fn callback) (PathReference->clj %))))


;; re-frame Effects/Subscriptions
(defn- deleter
  ([path] (.delete (clj->DocumentReference path)))
  ([instance path] (.delete instance (clj->DocumentReference path))))

(defn- setter
  ([path data set-options]
   (.set (clj->DocumentReference path)
         (clj->js data)
         (clj->js (or set-options {}))))
  ([instance path data set-options]
   (.set instance
         (clj->DocumentReference path)
         (clj->js data)
         (clj->js (or set-options {})))))


(defn- updater
  ([path data] (.update (clj->DocumentReference path) (clj->js data)))
  ([instance path data] (.update instance (clj->DocumentReference path) (clj->js data))))


(defn- adder [path data]
  (.add (clj->CollectionReference path) (clj->js data)))


(defn- delete-effect [{:keys [path on-success on-failure]}]
  (promise-wrapper (deleter path) on-success on-failure))


(defn- set-effect [{:keys [path data set-options on-success on-failure]}]
  (promise-wrapper (setter path data set-options) on-success on-failure))


(defn- update-effect [{:keys [path data on-success on-failure]}]
  (promise-wrapper (updater path data) on-success on-failure))


(defn- add-effect [{:keys [path data on-success on-failure]}]
  (promise-wrapper (adder path data) (reference-parser-wrapper on-success) on-failure))


(defn- batch-write-effect [{:keys [operations on-success on-failure]}]
  (let [batch-instance (.batch (js/firebase.firestore))]
    (run! (fn [[event-type {:keys [path data set-options]}]]
            (case event-type
              :firestore/delete (deleter batch-instance path)
              :firestore/set    (setter batch-instance path data set-options)
              :firestore/update (updater batch-instance path data)
              (js/alert "Internal error: unknown write effect: " event-type)))
          operations)
    (promise-wrapper (.commit batch-instance) on-success on-failure)))


(defn- query [ref where order-by limit
              start-at start-after end-at end-before]
  (as-> ref $
    (if where
      (reduce
        (fn [$$ [field op value]] (.where $$ (clj->js field) (clj->js op) (clj->js value)))
        $ where)
      $)
    (if order-by
      (reduce
        (fn [$$ order] (.orderBy $$ (clj->js (nth order 0)) (clj->js (nth order 1 :asc))))
        $ order-by)
      $)
    (if limit (.limit $ limit) $)
    (if start-at (.apply (.-startAt $) $ (clj->js start-at)) $)
    (if start-after (.apply (.-startAfter $) $ (clj->js start-after)) $)
    (if end-at (.apply (.-endAt $) $ (clj->js end-at)) $)
    (if end-before (.apply (.-endBefore $) $ (clj->js end-before)) $)))


(defn- getter-document [path]
  (.get (clj->DocumentReference path)))


(defn- getter-collection [path where order-by limit
                          start-at start-after end-at end-before]
  (.get (query (clj->CollectionReference path) where order-by limit
               start-at start-after end-at end-before)))


(defn- get-effect [{:keys [path-document
                           path-collection where order-by limit
                           start-at start-after end-at end-before doc-changes
                           snapshot-options
                           expose-objects
                           on-success on-failure]}]
  (if path-document
    (promise-wrapper (getter-document path-document)
                     (document-parser-wrapper on-success snapshot-options expose-objects)
                     on-failure)
    (promise-wrapper (getter-collection path-collection where order-by limit
                                        start-at start-after end-at end-before)
                     (collection-parser-wrapper on-success snapshot-options doc-changes expose-objects)
                     on-failure)))


(defn- on-snapshotter [reference-or-query listen-options on-next on-error]
  (.onSnapshot reference-or-query
    (clj->js (or listen-options {}))
    on-next
    (if on-error (event->fn on-error) (core/default-error-handler))))


(defn- on-snapshot [{:keys [path-document
                            path-collection where order-by limit
                            start-at start-after end-at end-before doc-changes
                            listen-options snapshot-options
                            expose-objects
                            on-next on-failure]}]
    {:pre [(utils/validate :re-frame/vec-or-fn on-next)
           (utils/validate (s/nilable :re-frame/vec-or-fn) on-failure)]}
    (if path-document
      (on-snapshotter (clj->DocumentReference path-document)
                      listen-options
                      (document-parser-wrapper on-next snapshot-options expose-objects)
                      on-failure)
      (on-snapshotter (query (clj->CollectionReference path-collection) where order-by limit
                             start-at start-after end-at end-before)
                      listen-options
                      (collection-parser-wrapper on-next snapshot-options doc-changes expose-objects)
                      on-failure)))


(defn- on-snapshot-sub [app-db [_ params]]
  ;; [TODO] Potential bug alert:
  ;;        This works the same way as database/on-value-sub, except for UUIDs.
  (let [uuid (str (random-uuid))
        callback #(>evt [::on-snapshot-handler uuid %])
        unsubscribe (on-snapshot (assoc params :on-next callback))]
    (ratom/make-reaction
      (fn [] (get-in @app-db [::cache uuid] []))
      :on-dispose #(do (unsubscribe) (>evt [::on-snapshot-handler uuid nil])))))


(re-frame/reg-event-db
 ::on-snapshot-handler
 (fn [app-db [_ uuid value]]
   (if value
     (assoc-in app-db [::cache uuid] value)
     (update app-db ::cache dissoc uuid))))
