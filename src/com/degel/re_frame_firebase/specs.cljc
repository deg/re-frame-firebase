;;; Author: David Goldfarb (deg@degel.com)
;;; Copyright (c) 2017, David Goldfarb

(ns com.degel.re-frame-firebase.specs
  (:require
   [clojure.spec.alpha :as s]))

(s/def :firebase/fb-path (s/coll-of (s/or :string string? :keyword keyword?) :into []))
