(ns com.degel.re-frame-firebase.storage
  (:require
   [firebase.storage :as firebase-storage]))

(defn get-storage [bucket]
  (if bucket
    (.storage (.app js/firebase) (str "gs://" bucket))
    (js/firebase.storage)))                                 ;default firebase bucket

(defn put
  ([path file metadata on-success on-error]
   (put path file metadata on-success on-error nil))
  ([path file metadata on-success on-error bucket]
   (let [upload-task (.put (-> (.ref (get-storage bucket))
                               (.child path))
                           file metadata)]
     (.on
      upload-task
      "state_changed"
      #(let [progress (* (/ (.-bytesTransferred %) (.-totalBytes %)) 100)]
         (.log js/console (str "Upload is " progress)))
      #(on-error %)
      #(on-success)))))

(defn delete
  ([key on-success on-error]
   (delete key on-success on-error nil))
  ([key on-success on-error bucket]
   (.then (.delete (-> (.ref (get-storage bucket))
                       (.child key)))
          on-success
          on-error)))

(defn download-url
  ([key on-success on-error]
   (download-url key on-success on-error nil))
  ([key on-success on-error bucket]
   (.then (.getDownloadURL (-> (.ref (get-storage bucket))
                               (.child key)))
          on-success
          on-error)))

(defn put-effect [items _]
  (doseq [item items]
    (let [{:keys [file metadata on-success on-error]} item]
      (put (key file) (val file)
           (clj->js metadata)
           on-success on-error))))

(defn delete-effect [items _]
  (doseq [item items]
    (let [{:keys [file on-success on-error]} item]
      (delete (key file) on-success on-error))))




