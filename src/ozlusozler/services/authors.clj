(ns ozlusozler.services.authors
  (:require [ozlusozler.db.schema :refer :all]
            [ozlusozler.util :as util]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/authors.sql")

(defn id-by-name [author]
  (let [id (:id (first (find-author-by-name db-spec author)))]
    (if (nil? id)
      (first (vals (save-author<! db-spec author "" "" nil)))
      id)))

