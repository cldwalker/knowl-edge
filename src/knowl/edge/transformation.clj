; Copyright (c) 2012 Jochen Rau
; 
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
; 
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
; 
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.

(ns
  ^{:doc "This namespace provides functionailty to transform a given resource recursively into a representation. It is part of the know:ledge management system."
    :author "Jochen Rau"}  
  knowl.edge.transformation
  (:refer-clojure :exclude [namespace])
  (:use knowl.edge.store
        knowl.edge.model)
  (:require
    [clj-time.format :as time]
    [net.cgrand.enlive-html :as template]))

(def ^:dynamic *template* (template/html-resource (java.io.File. "resources/private/templates/page.html")))
(def store (knowl.edge.store.Endpoint. "http://localhost:3030/ds/query" {}))

;; Predicates

(defn- type= [resource]
  (template/attr= :typeof (identifier resource)))

(defn- property= [resource]
  (template/attr= :property (identifier resource)))

;; Context

(defprotocol ContextHandling
  "Functions for dealing with a transformations context (like render depth, the web request, or the current selector chain)."
  (conj-selector [this selector] "Appends a selector to the selector-chain"))

(defrecord Context [depth selector-chain]
  ContextHandling
  (conj-selector
    [this selector]
    (update-in this [:selector-chain] #(into % selector))))

;; Transformations

(defprotocol Transformer
  "Provides functions to transform the given subject into a different representation."
  (transform [this context] "Transforms the subject."))
(extend-protocol Transformer
  java.lang.String
  (transform [this context] this)
  nil
  (transform [this context] nil))

(defmulti transform-literal (fn [literal context] (-> literal datatype value)))
(defmethod transform-literal :default [literal context] (value literal))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#dateTime" [literal context]
  (time/unparse (time/formatters :rfc822) (time/parse (time/formatters :date-time-no-ms) (value literal))))

(defn transform-resource [resource context]
  (if-let [statements (seq (find-by-subject store resource))]
    (let [context (conj-selector context [(type= (first (find-types-of store resource)))])
          snippet (template/select *template* (:selector-chain context))
          grouped-statements (group-by #(predicate %) statements)]
      (loop [snippet (template/transform snippet [template/root] (template/set-attr :resource (identifier resource)))
             grouped-statements grouped-statements]
        (if-not (seq grouped-statements)
          snippet
          (recur
            (template/transform
              snippet [(property= (ffirst grouped-statements))]
              (template/clone-for
                [statement (second (first grouped-statements))]
                (template/do->
                  (template/content (transform (object statement) context))
                  #_(if (isa? (-> statement object) knowl.edge.model/Literal)
                    (if-let [datatype (-> statement object datatype)]
                      (template/do->
                        (template/set-attr :datatype (value datatype))
                        (template/set-attr :content (-> statement object value)))
                      identity)
                    identity))))
            (rest grouped-statements)))))))

;; Entry Point

(defn dereference [resource]
  (if-let [result (transform resource (Context. 0 []))]
    (template/emit* result)))