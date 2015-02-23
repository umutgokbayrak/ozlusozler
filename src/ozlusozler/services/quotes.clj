(ns ozlusozler.services.quotes
  (:require [ozlusozler.db.schema :refer :all]
            [environ.core :refer [env]]
            [ozlusozler.util :as util]
            [ozlusozler.services.users :as users]
            [ozlusozler.services.authors :as authors]
            [ozlusozler.services.categories :as categories]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/quotes.sql")


;; PRIVATE FUNCTIONS

(defn- quote-by-hash [quote-hash]
  "hash'ten quote'u getirir."
  (first (find-quote-by-hash db-spec quote-hash)))


(defn- fn-quotes []
  "memorize ile cache'lemek icin oncelikle fn tanimliyoruz"
  (get-quotes db-spec))
(def memo-quotes (memoize fn-quotes))


(defn- report-ratio [report-count display-count]
  "100 kisiden fazla kisiye gosterildiginde %kac kisi sikayet dugmesine basmis"
  (if (and (> report-count 0) (> display-count 100))
    (/ report-count display-count)
    0))


(defn- filter-reported [quotes]
  "Gosterildigi kisilerde %10'den fazla sikayet alan quote'lar discard edilir"
  (filter
   #(let [report-count (:report_count %)
          display-count (:display_count %)
          report-ratio (report-ratio report-count display-count)]
      (< report-ratio 0.1))
   quotes))


(defn- filter-seen [quotes user-hash]
  (let [user-seen (users/quotes-by-user-hash user-hash)
        filtered  (filter #(not (contains? (set user-seen) (:id %))) quotes)]
    (if (empty? filtered)
      quotes
      filtered)))


(defn- choose-best [quotes]
  "TODO: Bu metod basit bir recommendation engine ile replace edilmeli"
  (if (empty? quotes)
    []
    (rand-nth quotes)))


(defn- populate-author [quote]
  "Reads the author_id from the quote and appends an author node to the quote"
  (let [author (authors/author-by-id (:author_id quote))]
    (if (not (nil? author))
      (dissoc (conj quote {:author author}) :author_id)
      quote)))


(defn- populate-categories [quote]
  (let [category-ids (categories/categories-by-quote (:id quote))
        categories   (map #(categories/category-by-id (:category_id %)) category-ids)]
    (if (not (nil? categories))
      (conj quote {:categories categories})
      categories)))


(defn- set-display-stats! [quote user-hash]
  "Bu quote'un bu kullanici icin gosterim yapildigina dair kayit atar"
  ;; TODO: quotes/display_count + 1
  ;; TODO: users_quotes tablosuna yeni bir satir ekle
  ;; TODO: stats_by_date tablosuna yeni bir satir ekle veya satir zaten varsa :display_count + 1 yap
  )

;; PUBLIC FUNCTIONS

(defn save-quote! [quote author category]
  "Saves the quote arranging the author, category etc..."
  (let [quote-hash (util/generate-hash quote)
        not-exists? (empty? (find-quote-by-hash db-spec quote-hash))
        author_id (authors/id-by-name author)]
    (when not-exists?
      (new-quote! db-spec quote-hash quote author_id (util/sql-now) false 0 0 0 0 0))
    (let [quote-id (:id (first (find-quote-by-hash db-spec quote-hash)))]
      (categories/assign-category-to-quote quote-id category))))


(defn get-quote [user-hash]
  "Find a quote to be displayed for this specific user"

  ;; TODO: bu ip ve hash mevzuyu abuse ediyor mu kontrol et, gerekliyse yasakla

  (let [unreported   (filter-reported (if (env :dev) (fn-quotes) (memo-quotes)))
        unseen       (filter-seen unreported user-hash)
        quote        (choose-best unseen)
        _            (set-display-stats! quote user-hash)
        author-added (populate-author quote)
        cats-added   (populate-categories author-added)]
    cats-added))


; (get-quote "hash")

; (save-quote! "There is no spoon 6" "Master Yoda 2" "Wisdom")
