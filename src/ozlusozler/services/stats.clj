(ns ozlusozler.services.stats
  (:require [ozlusozler.db.schema :refer :all]
            [ozlusozler.util :as util]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/stats.sql")


(defn inc-display-count! [quote-id]
  (let [date  (util/day-string (util/sql-now))
        stats (first (find-stats-for-quote db-spec quote-id date))
        display-new (if (nil? stats) 1 (+ 1 (:display_count stats)))]
    (if (nil? stats)
      (insert-daily-stats! db-spec quote-id date 0 display-new 0 0 0)
      (update-daily-stats!
       db-spec
       (:skip_count stats)
       display-new
       (:share_count stats)
       (:like_count stats)
       (:report_count stats)
       quote-id
       date))))

(defn inc-skip-count! [quote-id]
  (let [date  (util/day-string (util/sql-now))
        stats (first (find-stats-for-quote db-spec quote-id date))
        skip-new (+ 1 (:skip_count stats))]
    (update-daily-stats!
     db-spec
     skip-new
     (:display_count stats)
     (:share_count stats)
     (:like_count stats)
     (:report_count stats)
     quote-id
     date)))


(defn inc-share-count! [quote-id]
  (let [date  (util/day-string (util/sql-now))
        stats (first (find-stats-for-quote db-spec quote-id date))
        share-new (+ 1 (:share_count stats))]
    (update-daily-stats!
     db-spec
     (:skip_count stats)
     (:display_count stats)
     share-new
     (:like_count stats)
     (:report_count stats)
     quote-id
     date)))


(defn inc-like-count! [quote-id]
  (let [date  (util/day-string (util/sql-now))
        stats (first (find-stats-for-quote db-spec quote-id date))
        like-new (+ 1 (:like_count stats))]
    (update-daily-stats!
     db-spec
     (:skip_count stats)
     (:display_count stats)
     (:share_count stats)
     like-new
     (:report_count stats)
     quote-id
     date)))


(defn inc-report-count! [quote-id]
  (let [date  (util/day-string (util/sql-now))
        stats (first (find-stats-for-quote db-spec quote-id date))
        report-new (+ 1 (:report_count stats))]
    (update-daily-stats!
     db-spec
     (:skip_count stats)
     (:display_count stats)
     (:share_count stats)
     (:like_count stats)
     report-new
     quote-id
     date)))
