;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.specs
  (:require
   [clojure.spec.alpha :as s]))

;; Database
(s/def ::path (s/coll-of (s/or :string string? :keyword keyword?) :into []))

;; Firestore
(s/def ::path-collection (s/and ::path #(odd? (count %))))

(s/def ::path-document (s/and ::path #(even? (count %))))
