(ns ozlusozler.services.categories
  (:require [ozlusozler.db.schema :refer :all]
            [ozlusozler.util :as util]
            [yesql.core :refer [defqueries]]))

(defqueries "sql/categories.sql")

(defn id-by-name [category]
  (let [id (:id (first (find-category-by-name db-spec category)))]
    (if (nil? id)
      (first (vals (save-category<! db-spec category)))
      id)))


(defn save-quote-category-pair! [quote-id category-id]
  (let [not-exists? (empty? (find-quote-category-pair
                             db-spec
                             quote-id category-id))]
    (when not-exists?
      (new-quote-category! db-spec quote-id category-id))))


(defn assign-category-to-quote [quote-id category]
  "Bir quote bir kategoriye giriyorsa, onu o kategoriye assign eder"
  (let [category_id (id-by-name category)]
    (save-quote-category-pair! quote-id category_id)))
