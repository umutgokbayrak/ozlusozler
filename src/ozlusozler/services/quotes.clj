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


(defn- filter-by-category [quotes category-id]
  "Quote listesi icerisinde bu kategoriden olanlari filtreler
  TODO: performansini toparlamak gerekiyor."
  (if (not (nil? category-id))
    (filter (fn [quote]
              (let [category-ids (map #(:category_id %) (categories/categories-by-quote (:id quote)))]
                (contains? (set category-ids) category-id )))
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
        categories   (map #(categories/category-by-id (:category_id %)) category-ids)]
    (if (not (nil? categories))
      (conj quote {:categories categories})
      categories)))


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

    ; TODO: stats_by_date tablosuna yeni bir satir ekle veya satir zaten varsa :display_count + 1 yap

    ; display_count bir artti. Bu yeni sayiyi geri donduruyoruz
    (assoc quote :display_count display-count-new)))


(defn- choose-best [quotes fallback-quotes]
  "Bu quote'lardan en yuksek begeni almis olanina gore sort et ve birini sec"
  (if (not (empty? quotes))
    (do
      (let [sorted-quotes
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

  ;; TODO: category id verdiyse buna gore filtrele
  ;; TODO: author id verdiyse buna gore filtrele
  ;; TODO: bu ip ve hash mevzuyu abuse ediyor mu kontrol et, gerekliyse yasakla

  (let [all-quotes   (if (env :dev) (fn-quotes) (memo-quotes))
        fallback     (filter-params all-quotes (:category-id (first params)) (:author-id (first params)))
        unreported   (filter-reported fallback)
        unseen       (filter-seen unreported user-hash)
        quote        (choose-best unseen fallback)
        quote-new    (set-display-stats! quote user-hash)
        author-added (populate-author quote-new)
        cats-added   (populate-categories author-added)]
    cats-added))

; (get-quote "hash2" {:category-id 6})



; (repeatedly 10 #(get-quote "hash2"))

; (save-quote! "There is no spoon 6" "Master Yoda 2" "Wisdom")
