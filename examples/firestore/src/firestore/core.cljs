(ns firestore.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [com.degel.re-frame-firebase :as firebase]
            [iron.re-utils :as re-utils :refer [<sub >evt event->fn sub->fn]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

;; Global stuff
(defonce firebase-app-info {:apiKey "***REMOVED***"
                            :authDomain "***REMOVED***"
                            :databaseURL "***REMOVED***"
                            :projectId "***REMOVED***"
                            :storageBucket "***REMOVED***.appspot.com"
                            :messagingSenderId "***REMOVED***"})

(re-frame/reg-event-db :set-user (fn [db [_ user]] (assoc db :user user)))

(re-frame/reg-sub :user (fn [db _] (:user db)))

(defn code [language text & args]
  [:div
   [:p (str/join " " args)]
   [:pre [(keyword (str "code.border.language-" language)) text]]])

;; Example 1
;; This example will add a random field to a document every time the user clicks.
(re-frame/reg-event-fx
  :example-1-set
  (fn [_ _] {:firestore/set {:path [:sample-collection :sample-document]
                             :data {:sample-field (str (random-uuid))}}}))
(re-frame/reg-event-fx
  :example-1-update
  (fn [_ _] {:firestore/update {:path [:sample-collection :sample-document]
                                :data {(str (random-uuid)) "Random field"}}}))
(re-frame/reg-event-fx
  :example-1-delete
  (fn [_ _] {:firestore/delete {:path [:sample-collection :sample-document]}}))

(re-frame/reg-event-fx
  :example-1-get
  (fn [_ _] {:firestore/get {:path-document [:sample-collection :sample-document]
                             :on-success [:example-1-updatedb]}}))

(re-frame/reg-event-db
  :example-1-updatedb
  (fn [db [_ value]]
    (assoc db :example-1-value value)))

(re-frame/reg-sub
  :example-1-value1-pre
  (fn [db _] (:example-1-value db)))

(re-frame/reg-sub
  :example-1-value1
  (fn [_ _]
    (re-frame/subscribe [:example-1-value1-pre]))
  (fn [value _]
    (with-out-str (pprint value))))

(re-frame/reg-sub
  :example-1-value2
  (fn [_ _]
    (re-frame/subscribe [:firestore/on-snapshot {:path-document [:sample-collection :sample-document]}]))
  (fn [value _]
    (with-out-str (pprint value))))

(defn example-1
  []
  (let [value1 (<sub [:example-1-value1])
        value2 (<sub [:example-1-value2])]
    [:div.example
     [:h2 "Example 1"]
     [:div [:button {:on-click #(>evt [:example-1-set])} "Set"]
           [:button {:on-click #(>evt [:example-1-update])} "Update"]
           [:button {:on-click #(>evt [:example-1-delete])} "Delete"]
           [:button {:on-click #(>evt [:example-1-get])} "Get"]]
     [:div (code "clojure" value1 "This field will only update when you click \"Get\":")
           (code "clojure" value2 "This field auto-updates via \":firestore/on-snapshot\":")]]))


;; Example 2
(re-frame/reg-event-fx
  :example-2-addsamples
  (fn [_ _]
    {:firestore/batch-write
      {:operations
       [[:firestore/set {:path [:cities "SF"] :data {:name "San Francisco" :state "CA" :country "USA" :capital false :population 860000}}]
        [:firestore/set {:path [:cities "LA"] :data {:name "Los Angeles" :state "CA" :country "USA" :capital false :population 3900000}}]
        [:firestore/set {:path [:cities "DC"] :data {:name "Washington, D.C." :state nil :country "USA" :capital true :population 680000}}]
        [:firestore/set {:path [:cities "TOK"] :data {:name "Tokyo" :state nil :country "Japan" :capital true :population 9000000}}]
        [:firestore/set {:path [:cities "BJ"] :data {:name "Beijing" :state nil :country "China" :capital true :population 2150000}}]
        [:firestore/set {:path [:cities "S1"] :data {:name "Springfield" :state "Massachusetts"}}]
        [:firestore/set {:path [:cities "S2"] :data {:name "Springfield" :state "Missouri"}}]
        [:firestore/set {:path [:cities "S3"] :data {:name "Springfield" :state "Wisconsin"}}]]}}))

(re-frame/reg-event-fx
  :example-2-addrandom
  (fn [_ _] {:firestore/add {:path [:cities] :data {:name (str (random-uuid))
                                                    :state nil
                                                    :country "No Man's Land"
                                                    :population (rand-int 10000000)}
                             :on-success #(js/alert (str "Added random city to: " %))}}))

(re-frame/reg-event-fx
  :example-2-delete
  (fn [_ [_ v]]
    {:firebase/multi (map #(as-> % $ (:ref $) {:path $} [:firestore/delete $])
                          (:docs v))}))

(re-frame/reg-event-fx
  :example-2-deleteall
  (fn [_ _] {:firestore/get {:path-collection [:cities]
                             :on-success [:example-2-delete]}}))

(re-frame/reg-event-db
  :example-2-updatedb
  (fn [db [_ value]]
    (assoc db :example-2-value value)))

(re-frame/reg-event-fx
  :example-2-listen
  (fn [_ _] {:firestore/on-snapshot {:path-collection [:cities]
                                     :doc-changes true
                                     :on-next #(do (.log js/console "Changes:") (pprint (:doc-changes %)))}}))

(re-frame/reg-event-fx
  :example-2-get
  (fn [_ [_ query]] {:firestore/get (assoc query :on-success [:example-2-updatedb])}))

(re-frame/reg-sub
  :example-2-value
  (fn [db _] (with-out-str (pprint (:example-2-value db)))))

(defn example-2
  []
  [:div.example
   [:h2 "Example 2"]
   [:div [:button {:on-click #(>evt [:example-2-addsamples])} "Add Samples"]
         [:button {:on-click #(>evt [:example-2-addrandom])} "Add Random"]
         [:button {:on-click #(>evt [:example-2-deleteall])} "Delete All"]
         [:button {:on-click #(>evt [:example-2-listen])} "Start listening to changes (check console)"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities]}])} "Get all"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities]
                                                     :where [[:capital :== true]]}])}
                  "Get capitals"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities]
                                                     :where [["state" "==" "CA"]
                                                             [:population :> 1000000]]}])}
                  "Get large in California"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities]
                                                     :order-by [[:population :desc]]
                                                     :limit 2}])}
                  "Get two largest populations"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities]
                                                     :order-by [[:name] [:state]]
                                                     :start-at ["Springfield" "Missouri"]}])}
                  "Get starting at Springfield, Missouri / ordering by [:name] and then [:state] (both ascendant by default)"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities] :expose-objects true}])}
                  "Get all exposing objects"]
         [:button {:on-click #(>evt [:example-2-get {:path-document [:cities "LA"] :expose-objects true}])}
                  "Get LA exposing objects"]
         [:button {:on-click #(>evt [:example-2-get {:path-collection [:cities] :expose-objects true}])}
                  "Get all "]]
   [:div (code "clojure" (<sub [:example-2-value]) "Query results will appear here (click \"Get all\" to start):")]])

;; Entry Point
(defn user-info
  []
  [:div (<sub [:user])])

(defn ui
  []
  [:div.container
   [:h1 "See below a bunch of examples inspired by the firestore docs."]
   (user-info)
   (example-1)
   (example-2)])

(defn ^:export run
  []
  (firebase/init :firebase-app-info firebase-app-info
                 :get-user-sub      [:user]
                 :set-user-event    [:set-user])
  (reagent/render [ui]
                  (js/document.getElementById "app")))
