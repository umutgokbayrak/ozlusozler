(ns ozlusozler.util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [clj-time.local :as l]
            [clj-time.coerce :as c]
            [digest :as digest]))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (md/md-to-html-string (io/slurp-resource filename)))


(defn sql-now
  "returns sql timestamp for now"
  []
  (c/to-sql-time (l/local-now)))


(defn generate-hash [& txt]
  (let [plain (if (nil? txt)
                (.toString (java.util.UUID/randomUUID))
                (first txt))]
    (digest/md5 plain)))


(defn day-string [date]
  (.format
   (java.text.SimpleDateFormat. "yyyy-MM-dd")
   (java.util.Date.
    (.getTime date))))

(defn cookie-expire []
  "Wed, 31 Oct 2089 08:50:17 GMT")
