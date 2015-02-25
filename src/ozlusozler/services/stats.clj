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

