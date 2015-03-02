(ns ozlusozler.services.quotes
 (:require [ozlusozler.db.schema :refer [db-spec]]
           [ozlusozler.util :as util]
           [ozlusozler.services [users :as users]
                                [authors :as authors]
                                [categories :as categories]
                                [stats :as stats]]
           [yesql.core :refer [defqueries]]))

(defqueries "sql/quotes.sql")

;; PRIVATE FUNCTIONS

(defn- quote-by-id [quote-id]
  "id'ten quote'u getirir."
  (first (find-quote-by-id db-spec quote-id)))


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


(defn- filter-by-category [quotes category-id]
  "Quote listesi icerisinde bu kategoriden olanlari filtreler"

  (if (not (nil? category-id))
    (filter (fn [quote]
              (let [categories (categories/categories-by-quote (:id quote))]
                (contains? categories category-id)))
            quotes)
    quotes))


(defn- filter-by-author [quotes author-id]
  "Quote listesi icerisinde bu yazardan olanlari filtreler"
  (if (not (nil? author-id))
    (filter #(= (:author_id %) author-id) quotes)
    quotes))


(defn- filter-params [quotes category-id author-id]
  "Filters quotes with the specified ctegory and author id"
  (filter-by-category (filter-by-author quotes author-id) category-id))


(defn- populate-author [quote]
  "Reads the author_id from the quote and appends an author node to the quote"
  (let [author (authors/author-by-id (:author_id quote))]
    (if (not (nil? author))
      (dissoc (conj quote {:author author}) :author_id)
      quote)))


(defn- populate-categories [quote]
  (let [category-ids (categories/categories-by-quote (:id quote))
        categories   (map #(categories/category-by-id %) category-ids)]
    (if (not (nil? categories))
      (conj quote {:categories categories})
      categories)))


(defn- inc-skip-count! [quote-id]
  (let [quote (quote-by-id quote-id)
        skip-new (+ 1 (:skip_count quote))]
    (update-quote-skip-count! db-spec skip-new (:id quote))))


(defn- inc-share-count! [quote-id]
  (let [quote (quote-by-id quote-id)
        share-new (+ 1 (:share_count quote))]
    (update-quote-share-count! db-spec share-new (:id quote))))


(defn- inc-like-count! [quote-id]
  (let [quote (quote-by-id quote-id)
        like-new (+ 1 (:like_count quote))]
    (update-quote-like-count! db-spec like-new (:id quote))))


(defn- inc-report-count! [quote-id]
  (let [quote (quote-by-id quote-id)
        report-new (+ 1 (:report_count quote))]
    (update-quote-report-count! db-spec report-new (:id quote))))


(defn- set-display-stats! [quote user-hash]
  "Bu quote'un bu kullanici icin gosterim yapildigina dair kayit atar"

  (let [display-count-new (+ (:display_count quote) 1)
        user-id (users/user-id-by-hash user-hash)]
    ; eger bu user id yoksa, henuz user yaratilmamistir.
    (when (nil? user-id)
      (users/new-user-with-hash user-hash))

    ; quote'un gosterim sayisi bu kullanici icin bir arttirilir.
    (users/set-displayed-for-user! (:id quote) user-hash)

    ; quote'un gosterim sayisi tum kullanicilar icin bir arttirilir.
    (update-quote-display-count! db-spec display-count-new (:id quote))

    ; stats_by_date tablosuna yeni bir satir ekle veya satir zaten varsa :display_count + 1 yap
    (stats/inc-display-count! (:id quote))

    ; display_count bir artti. Bu yeni sayiyi geri donduruyoruz
    (assoc quote :display_count display-count-new)))


(defn- choose-best [quotes fallback-quotes]
  "Bu quote'lardan en yuksek begeni almis olanina gore sort et ve birini sec"
  (if (not (empty? quotes))
    (do
      (let
        [sorted-quotes
         (sort-by
          #(let [like-point (* (:like_count %) 2)
                 display-point (:display_count %)
                 boosted-point (if (:boosted %) 5 0)
                 share-point (* (:share_count %) 3)
                 skip-point (* (:skip_count %) -1)
                 report-point (* (:report_count %) -3)]
             (+ like-point display-point boosted-point share-point skip-point report-point))
          > (shuffle quotes))]
        (rand-nth (take 15 sorted-quotes))))
    fallback-quotes))


;; PUBLIC FUNCTIONS

(defn quote-by-hash [quote-hash]
  "hash'ten quote'u getirir."
  (first (find-quote-by-hash db-spec quote-hash)))


(defn skip-quote [quote-id user-hash]
  "Kullanici x saniyeden az sure bir quote'a bakarsa skip etmistir."
  ; stats_by_date icinde skip_count + 1
  (stats/inc-skip-count! quote-id)

  ; quotes icinde skip_count + 1
  (inc-skip-count! quote-id)

  ; users_quotes icinde skip_flag = true
  (users/set-skip-flag-for-quote! quote-id user-hash))


(defn share-quote [quote-id user-hash]
  "Kullanici bu quote'u bir sosyal yontemle paylastiysa"
  ; stats_by_date icinde share_count + 1
  (stats/inc-share-count! quote-id)

  ; quotes icinde share_count + 1
  (inc-share-count! quote-id)

  ; users_quotes icinde share_flag = true
  (users/set-share-flag-for-quote! quote-id user-hash))


(defn like-quote [quote-id user-hash]
  "Kullanici bu quote'u begendiyse"
  ; stats_by_date icinde like_count + 1
  (stats/inc-like-count! quote-id)

  ; quotes icinde like_count + 1
  (inc-like-count! quote-id)

  ; users_quotes icinde like_flag = true
  (users/set-like-flag-for-quote! quote-id user-hash))


(defn report-quote [quote-id user-hash]
  "Kullanici bu quote'u sikayet ettiyse"
  ; stats_by_date icinde report_count + 1
  (stats/inc-report-count! quote-id)

  ; quotes icinde report_count + 1
  (inc-report-count! quote-id)

  ; users_quotes icinde report_flag = true
  (users/set-report-flag-for-quote! quote-id user-hash))


(defn save-quote! [quote author category]
  "Saves the quote arranging the author, category etc..."
  (let [quote-hash (util/generate-hash quote)
        not-exists? (empty? (find-quote-by-hash db-spec quote-hash))
        author_id (authors/id-by-name author)]
    (when not-exists?
      (new-quote! db-spec quote-hash quote author_id (util/sql-now) false 0 0 0 0 0))
    (let [quote-id (:id (first (find-quote-by-hash db-spec quote-hash)))]
      (categories/assign-category-to-quote quote-id category))))


(defn get-quote [user-hash & params]
  "Find a quote to be displayed for this specific user"

  ; TODO: bu ip ve hash mevzuyu abuse ediyor mu kontrol et, gerekliyse yasakla

  (let [fallback     (filter-params (memo-quotes) (:category-id (first params)) (:author-id (first params)))
        unreported   (filter-reported fallback)
        unseen       (filter-seen unreported user-hash)
        quote        (choose-best unseen fallback)
        quote-new    (set-display-stats! quote user-hash)
        author-added (populate-author quote-new)
        cats-added   (populate-categories author-added)]
    cats-added))

; (get-quote "hash2" {:category-id nil :author-id 54})

; (get-quote "80bd18e84c6c2947849b8d8f07ffdb12" {:category-id  nil :author-id 54 })
