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


(defn set-firestore-settings
  [settings]
  (.settings (js/firebase.firestore) (clj->js (or settings {}))))

;; Extra public functions
(defn server-timestamp
  "Returns a field value to be used to store the server timestamp.
  See https://firebase.google.com/docs/firestore/manage-data/add-data#update_fields_in_nested_objects
  You should use this as a field value when setting/updating/adding a document.

  Example usage:
  {:firestore/add {:path [:some-colection]
                   :data {:name \"document-with-timestamp\"
                          :timestamp (server-timestamp)}}"
  []
  (.serverTimestamp js/firebase.firestore.FieldValue))

(defn delete-field-value
  "Returns a field value to be used to delete a field.
  See https://firebase.google.com/docs/firestore/manage-data/delete-data#fields
  When updating a document, you should use this as a field value if you want to
  delete such field.

  Example usage:
  {:firestore/update {:path [:my \"document\"]
                      :data {:field-to-delete (delete-field-value)}}}"
  []
  (.delete js/firebase.firestore.FieldValue))

(defn document-id-field-path
  "Returns a field path which can be used to refer to ID of a document.
  See https://firebase.google.com/docs/reference/js/firebase.firestore.FieldPath#.documentId
  It can be used in queries to sort or filter by the document ID.

  Example usage:
  {:firestore/get {:path-collection [:my-collection]
                   :where [[(document-id-field-path) :>= \"start\"]]}}"
  []
  (.documentId firebase.firestore.FieldPath))


;; Type Conversion/Parsing
(defn clj->CollectionReference
  "Converts a seq of keywords and/or strings into a CollectionReference.
  The seq represents the path to the collection (e.g. [:path \"to\" :collection]).
  See https://firebase.google.com/docs/reference/js/firebase.firestore.CollectionReference"
  [path]
  {:pre [(utils/validate ::specs/path-collection path)]}
  (if (instance? js/firebase.firestore.CollectionReference path)
    path
    (.collection (js/firebase.firestore)
                 (str/join "/" (clj->js path)))))

(defn clj->DocumentReference
  "Converts a seq of keywords and/or strings into a DocumentReference.
  The seq represents the path to the document (e.g. [:path-to \"document\"]).
  See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentReference"
  [path]
  {:pre [(utils/validate ::specs/path-document path)]}
  (if (instance? js/firebase.firestore.DocumentReference path)
    path
    (.doc (js/firebase.firestore)
          (str/join "/" (clj->js path)))))

(defn clj->FieldPath
  "Converts a string/keyword or a seq of string/keywords into a FieldPath.
  Uses the FieldPath contructor.
  Only tries conversion if the argument isn't a FieldPath already.
  Possible arguments: \"string.dotted.path\", :keyword-path, [:path :in-a :seq], a FieldPath object.
  See https://firebase.google.com/docs/reference/js/firebase.firestore.FieldPath"
  [field-path]
  (cond
    (nil? field-path) nil
    (instance? js/firebase.firestore.FieldPath field-path) field-path
    (coll? field-path) (apply js/firebase.firestore.FieldPath. (clj->js field-path))
    :else (js/firebase.firestore.FieldPath. (clj->js field-path))))

(defn clj->SetOptions
  "Converts a clojure-style map into a SetOptions satisfying one.
  The provided map can contain a :merge key with either true or false, and a
  :merge-fields key with a seq of field paths to be passed to clj->FieldPath.
  See https://firebase.google.com/docs/reference/js/firebase.firestore.SetOptions"
  [set-options]
  (as-> {} $
    (if (:merge set-options) (assoc $ :merge (:merge set-options)) $)
    (if (:marge-fields set-options)
      (assoc $ :mergeFields (into-array (map clj->FieldPath (:merge-fields set-options))))
      $)
    (clj->js $)))

(defn clj->GetOptions
  "Converts a clojure-style map into a GetOptions satisfying one.
  The provided map can contain a :source key with one of the following values:
  :default, :server or :cache. You can also provide a string like \"server\".
  See https://firebase.google.com/docs/reference/js/firebase.firestore.GetOptions"
  [get-options]
  (if get-options
    (clj->js {:source (:source get-options :default)})
    #js {}))

(defn clj->SnapshotListenOptions
  "Converts a clojure-style map into a SnapshotListenOptions satisfying one.
  The provided map can contain a :include-metadata-changes key with either true or false.
  See https://firebase.google.com/docs/reference/js/firebase.firestore.SnapshotListenOptions"
  [snapshot-listen-options]
  (if snapshot-listen-options
    (clj->js {:includeMetadataChanges (:include-metadata-changes snapshot-listen-options false)})
    #js {}))

(defn clj->SnapshotOptions
  "Converts a clojure-style map into a SnapshotOptions satisfying one.
  The provided map can containe a :server-timestamps key with one of the following values:
  :estimate, :previous or :none. You can also provide a string like \"estimate\".
  See https://firebase.google.com/docs/reference/js/firebase.firestore.SnapshotOptions"
  [snapshot-options]
  (clj->js {:serverTimestamps (:server-timestamps snapshot-options :none)}))

(defn PathReference->clj [reference]
  ;; [TODO]: Can this be optimized through some internal property of a Reference?
  "Converts a CollectionReference/DocumentReference into a vector of strings representing its path."
  (loop [ref reference
         result '()]
    (if ref
      (recur (.-parent ref) (conj result (.-id ref)))
      (vec result))))

(defn SnapshotMetadata->clj [metadata]
  "Converts a SnapshotMetadata object into a clojure-style map."
  {:from-cache (.-fromCache metadata)
   :has-pending-writes (.-hasPendingWrites metadata)})

(defn DocumentSnapshot->clj
  "Converts a DocumentSnapshot object into a clojure-style map.
  :data      the document's contents (nil if it doesn't exist).
  :id        a string representing document's id.
  :metadata  metadata converted with SnapshotMetadata->clj.
  :ref       the object's path converted with PathReference->clj.
  :object    the original DocumentSnapshot if expose-objects argument
             is set to true (nil otherwise).
  See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentSnapshot"
  ([doc]
   (DocumentSnapshot->clj doc nil nil nil))
  ([doc snapshot-options]
   (DocumentSnapshot->clj doc snapshot-options nil nil))
  ([doc snapshot-options expose-objects]
   (DocumentSnapshot->clj doc snapshot-options expose-objects nil))
  ([doc snapshot-options expose-objects sure-exists]
   {:data (when (or sure-exists (.-exists doc))
            (js->clj (.data doc (clj->SnapshotOptions snapshot-options))))
    :id (.-id doc)
    :metadata (SnapshotMetadata->clj (.-metadata doc))
    :ref (PathReference->clj (.-ref doc))
    :object (when expose-objects doc)}))

(defn DocumentChange->clj
  "Converts a DocumentChange object into a clojure-style map.
  :doc       the DocumentSnapshot converted with DocumentSnapshot->clj.
  :new-index a number.
  :old-index a number.
  :type      a string.
  :object    the original DocumentChange if expose-objects argument
             is set to true (nil otherwise).
  See https://firebase.google.com/docs/reference/js/firebase.firestore.DocumentChange"
  ([change] (DocumentChange->clj change nil nil))
  ([change snapshot-options] (DocumentChange->clj change snapshot-options nil))
  ([change snapshot-options expose-objects]
   {:doc (DocumentSnapshot->clj (.-doc change) snapshot-options expose-objects true)
    :new-index (.-newIndex change)
    :old-index (.-oldIndex change)
    :type (.-type change)
    :object (when expose-objects change)}))

(defn QuerySnapshot->clj
  "Converts a QuerySnapshot object into a clojure-style map.
  :docs        vector of documents converted with DocumentSnapshot->clj.
  :metadata    metadata converted with SnapshotMetadata->clj.
  :size        the number of documents.
  :doc-changes vector of DocumentChanges converted with DocumentChange->clj if
               doc-changes argument is set to true (nil otherwise).
  :object      the original DocumentSnapshot if expose-objects argument
               is set to true (nil otherwise).
  See https://firebase.google.com/docs/reference/js/firebase.firestore.QuerySnapshot"
  ([query]
   (QuerySnapshot->clj query nil nil nil nil))
  ([query snapshot-options]
   (QuerySnapshot->clj query snapshot-options nil nil nil))
  ([query snapshot-options snapshot-listen-options]
   (QuerySnapshot->clj query snapshot-options snapshot-listen-options nil nil))
  ([query snapshot-options snapshot-listen-options doc-changes]
   (QuerySnapshot->clj query snapshot-options snapshot-listen-options doc-changes nil))
  ([query snapshot-options snapshot-listen-options doc-changes expose-objects]
   {:docs (vec (map #(DocumentSnapshot->clj % snapshot-options expose-objects true)
                    (.-docs query)))
    :metadata (SnapshotMetadata->clj (.-metadata query))
    :size (.-size query)
    :doc-changes (when doc-changes
                   (vec (map #(DocumentChange->clj % snapshot-options expose-objects)
                             (.docChanges query (clj->SnapshotListenOptions snapshot-listen-options)))))
    :object (when expose-objects query)}))


(defn- document-parser-wrapper [callback snapshot-options expose-objects]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback
    #((re-utils/event->fn callback)
      (DocumentSnapshot->clj % snapshot-options expose-objects false))))

(defn- collection-parser-wrapper [callback snapshot-options snapshot-listen-options doc-changes expose-objects]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback
    #((re-utils/event->fn callback)
      (QuerySnapshot->clj % snapshot-options snapshot-listen-options doc-changes expose-objects))))

(defn- reference-parser-wrapper [callback]
  {:pre [(utils/validate (s/nilable :re-frame/vec-or-fn) callback)]}
  (when callback #((re-utils/event->fn callback) (PathReference->clj %))))


;; re-frame Effects/Subscriptions
(defn- setter
  ([path data set-options]
   (.set (clj->DocumentReference path)
         (clj->js data)
         (clj->SetOptions set-options)))
  ([instance path data set-options]
   (.set instance
         (clj->DocumentReference path)
         (clj->js data)
         (clj->SetOptions set-options))))

(defn- updater
  ([path data] (.update (clj->DocumentReference path) (clj->js data)))
  ([instance path data] (.update instance (clj->DocumentReference path) (clj->js data))))

(defn- deleter
  ([path] (.delete (clj->DocumentReference path)))
  ([instance path] (.delete instance (clj->DocumentReference path))))

(defn set-effect [{:keys [path data set-options on-success on-failure]}]
  (promise-wrapper (setter path data set-options) on-success on-failure))

(defn update-effect [{:keys [path data on-success on-failure]}]
  (promise-wrapper (updater path data) on-success on-failure))

(defn delete-effect [{:keys [path on-success on-failure]}]
  (promise-wrapper (deleter path) on-success on-failure))

(defn write-batch-effect [{:keys [operations on-success on-failure]}]
  (let [batch-instance (.batch (js/firebase.firestore))]
    (run! (fn [[event-type {:keys [path data set-options]}]]
            (case event-type
              :firestore/delete (deleter batch-instance path)
              :firestore/set    (setter batch-instance path data set-options)
              :firestore/update (updater batch-instance path data)
              (js/alert "Internal error: unknown write effect: " event-type)))
          operations)
    (promise-wrapper (.commit batch-instance) on-success on-failure)))

(defn- adder [path data]
  (.add (clj->CollectionReference path) (clj->js data)))

(defn add-effect [{:keys [path data on-success on-failure]}]
  (promise-wrapper (adder path data) (reference-parser-wrapper on-success) on-failure))

(defn- query [ref where order-by limit
              start-at start-after end-at end-before]
  (as-> ref $
    (if where
      (reduce
        (fn [$$ [field-path op value]] (.where $$ (clj->FieldPath field-path) (clj->js op) (clj->js value)))
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

(defn- getter-document [path get-options]
  (.get (clj->DocumentReference path) (clj->GetOptions get-options)))

(defn- getter-collection [path get-options where order-by limit
                          start-at start-after end-at end-before]
  (.get (query (clj->CollectionReference path) where order-by limit
               start-at start-after end-at end-before)
        (clj->GetOptions get-options)))

(defn get-effect [{:keys [path-document
                          path-collection where order-by limit
                          start-at start-after end-at end-before
                          doc-changes snapshot-listen-options
                          get-options snapshot-options expose-objects
                          on-success on-failure]}]
  (if path-document
    (promise-wrapper (getter-document path-document get-options)
                     (document-parser-wrapper on-success snapshot-options expose-objects)
                     on-failure)
    (promise-wrapper (getter-collection path-collection get-options where order-by limit
                                        start-at start-after end-at end-before)
                     (collection-parser-wrapper on-success snapshot-options snapshot-listen-options
                                                doc-changes expose-objects)
                     on-failure)))

(defn- on-snapshotter [reference-or-query snapshot-listen-options on-next on-error]
  (.onSnapshot reference-or-query
    (clj->SnapshotListenOptions snapshot-listen-options)
    on-next
    (if on-error (event->fn on-error) (core/default-error-handler))))

(defn on-snapshot [{:keys [path-document
                            path-collection where order-by limit
                            start-at start-after end-at end-before doc-changes
                            snapshot-listen-options snapshot-options
                            expose-objects
                            on-next on-error]}]
    {:pre [(utils/validate :re-frame/vec-or-fn on-next)
           (utils/validate (s/nilable :re-frame/vec-or-fn) on-error)]}
    (if path-document
      (on-snapshotter (clj->DocumentReference path-document)
                      snapshot-listen-options
                      (document-parser-wrapper on-next snapshot-options expose-objects)
                      on-error)
      (on-snapshotter (query (clj->CollectionReference path-collection) where order-by limit
                             start-at start-after end-at end-before)
                      snapshot-listen-options
                      (collection-parser-wrapper on-next snapshot-options snapshot-listen-options
                                                 doc-changes expose-objects)
                      on-error)))

(def on-snapshot-effect on-snapshot)

(defn on-snapshot-sub [app-db [_ params]]
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
