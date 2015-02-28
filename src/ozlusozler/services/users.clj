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


(defn land-on-site [& arr]
  (let [user-hash
        (if (or (nil? arr) (empty? arr))
          (let [lhash (util/generate-hash)]
            (new-user-with-hash lhash)
            lhash)
          (do
            (let [tmp_hash (first arr)]
              (when (nil? (user-id-by-hash tmp_hash))
                (new-user-with-hash tmp_hash))
              tmp_hash)))
        user        (first (find-user-by-hash db-spec user-hash))
        visit-count (+ 1 (:visit_count user))
        now         (util/sql-now)]
    (update-user-by-hash! db-spec visit-count now (:fast_forward_count user) user-hash)))


(defn set-displayed-for-user! [quote-id user-hash]
  "Quote kullanici icin goruntulendiginde bu quote bu kullanici icin
  tekrar goruntulenmemek uzere kayit altina alinir."
  (let [user-id     (user-id-by-hash user-hash)
        item-exists (not= 0 (:num (first (count-users-quotes db-spec user-id quote-id))))]
    ; Eger bu pair daha once kaydedildiyse tekrar kaydetmeye calismaya gerek yok.
    (when (not item-exists)
      (save-users-quotes! db-spec user-id quote-id false false false false))))


(defn set-skip-flag-for-quote! [quote-id user-hash]
  "users_quotes icerisinde bu quote'un skip edildigi notlanir"
  (let [user-id (user-id-by-hash user-hash)
        user-quote (first (find-users-quotes db-spec user-id quote-id))]
    (update-users-quotes!
     db-spec user-id quote-id
     (true (:like_flag user-quote) (:share_flag user-quote) (:report_flag user-quote)))))


(defn set-share-flag-for-quote! [quote-id user-hash]
  "users_quotes icerisinde bu quote'un share edildigi notlanir"
  (let [user-id (user-id-by-hash user-hash)
        user-quote (first (find-users-quotes db-spec user-id quote-id))]
    (update-users-quotes!
     db-spec user-id quote-id
     ((:skip_flag user-quote) (:like_flag user-quote) true (:report_flag user-quote)))))


(defn set-like-flag-for-quote! [quote-id user-hash]
  "users_quotes icerisinde bu quote'un like edildigi notlanir"
  (let [user-id (user-id-by-hash user-hash)
        user-quote (first (find-users-quotes db-spec user-id quote-id))]
    (update-users-quotes!
     db-spec user-id quote-id
     ((:skip_flag user-quote) true (:share_flag user-quote) (:report_flag user-quote)))))


(defn set-report-flag-for-quote! [quote-id user-hash]
  "users_quotes icerisinde bu quote'un like edildigi notlanir"
  (let [user-id (user-id-by-hash user-hash)
        user-quote (first (find-users-quotes db-spec user-id quote-id))]
    (update-users-quotes!
     db-spec user-id quote-id
     ((:skip_flag user-quote) (:like_flag user-quote) (:share_flag user-quote) true))))
