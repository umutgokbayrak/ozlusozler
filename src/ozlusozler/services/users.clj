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


(defn new-user-with-hash [user-hash]
  (first
   (vals
    (save-user<!
     db-spec
     user-hash 1 (util/sql-now) (util/sql-now) 0))))


(defn user-id-by-hash [user-hash]
  "Finds user by hash"
  (:id (first (find-user-by-hash db-spec user-hash))))


(defn quotes-by-user-hash [user-hash]
  (let [quotes-seen (get-quotes-seen db-spec (user-id-by-hash user-hash))]
    (if (empty? quotes-seen)
      []
      (map #(:quote_id %) quotes-seen))))


(defn land-on-site [user-hash]
  "TODO: users tablosunda visit_count ve last_visit'i update et."
  )


(defn skip-quote [quote-id user-hash]
  "TODO: implement this"
  )


(defn share-quote [quote-id user-hash]
  "TODO: implement this"
  )


(defn like-quote [quote-id user-hash]
  "TODO: implement this"
  )


(defn report-quote [quote-id user-hash]
  "TODO: implement this"
  )

(defn set-displayed-for-user! [quote-id user-hash]
  "Quote kullanici icin goruntulendiginde bu quote bu kullanici icin
  tekrar goruntulenmemek uzere kayit altina alinir."
  (let [user-id     (user-id-by-hash user-hash)
        item-exists (not= 0 (:num (first (count-users-quotes db-spec user-id quote-id))))]
    ; Eger bu pair daha once kaydedildiyse tekrar kaydetmeye calismaya gerek yok.
    (when (not item-exists)
      (save-users-quotes! db-spec user-id quote-id false false false false))))
