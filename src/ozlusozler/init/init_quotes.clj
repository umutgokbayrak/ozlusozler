(ns ozlusozler.init.init-quotes
  (:use     [clojure.java.io])
  (:require [ozlusozler.services.quotes :as quotes]))


(def category-atom (atom "Genel"))

(defn import-quotes []
  (with-open [rdr (reader "sozler.txt")]
    (doseq [line (line-seq rdr)]
      (when (not (empty? (.trim line)))
      (if (not (.startsWith line "\""))
        (reset! category-atom (.trim line))
        (let [arr       (clojure.string/split line #"~" )
              raw       (.trim (first arr))
              left-trim (if (.startsWith raw "\"")
                          (.substring raw 1)
                          raw)
              quote     (.trim
                         (if (.endsWith left-trim "\"")
                           (.substring left-trim 0 (- (.length left-trim) 1))
                           left-trim))
              author    (.trim (second arr))
              category  (.trim @category-atom)]
          (quotes/save-quote! quote author @category-atom)
      ))))))


; (import-quotes)
