(ns ozlusozler.services.users
  (:require [ozlusozler.db.schema :refer :all]
            [ozlusozler.util :as util]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/users.sql")

(defn new-user []
  (let [hash (util/generate-hash)]
    (first
     (vals
      (save-user<!
       db-spec
       hash 1 (util/sql-now) (util/sql-now) 0)))))


(defn quotes-by-user-hash [user-hash]
  (let [quotes-seen (get-quotes-seen db-spec (:id (first (find-user-by-hash db-spec user-hash))))]
    (if (empty? quotes-seen)
      []
      (map #(:quote_id %) quotes-seen))))

(defn land-on-site [user-hash]
  "TODO: users tablosunda visit_count ve last_visit'i update et."
  )
