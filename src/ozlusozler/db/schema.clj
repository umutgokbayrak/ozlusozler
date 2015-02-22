(ns ozlusozler.db.schema
  (:require [clojure.java.jdbc :as sql]
            [clojure.java.io :refer [file]]
            [noir.io :as io]))

(def db-store (str (.getName (file ".")) "/ozlusozler.db"))

(def db-spec {:classname "org.h2.Driver"
              :subprotocol "h2"
              :subname db-store
              :user "sa"
              :password ""
              :make-pool? true
              :naming {:keys clojure.string/lower-case
                       :fields clojure.string/upper-case}})


(defn initialized?
  "checks to see if the database schema is present"
  []
  (.exists (file (str db-store ".mv.db"))))


(defn- drop-tables []
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS users")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS quotes")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS categories")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS authors")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS quote_categories")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS users_quotes")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS user_blacklist")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS ip_blacklist")
  (sql/db-do-prepared db-spec "DROP TABLE IF EXISTS stats_by_date"))


(defn- create-users-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :users
    [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:hash_string "char(32) NOT NULL"]
    [:visit_count :int]
    [:first_visit_date :timestamp]
    [:last_visit_date :timestamp]
    [:fast_forward_count :int]))
  (sql/db-do-prepared db-spec "CREATE UNIQUE INDEX users_hash_string ON users (hash_string)"))


(defn- create-authors-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :authors
    [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:author_name "varchar(255) NOT NULL"]
    [:author_info "varchar(4000) NOT NULL DEFAULT ''"]
    [:wiki_url "varchar(400) NOT NULL"]
    [:photo_data :blob]))
  (sql/db-do-prepared db-spec "CREATE UNIQUE INDEX author_name_index ON authors (author_name)"))


(defn- create-categories-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :categories
    [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:category_name "varchar(30) NOT NULL"]))
  (sql/db-do-prepared db-spec "CREATE UNIQUE INDEX category_name_index ON categories (category_name)"))


(defn- create-quotes-table []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :quotes
    [:id "INTEGER PRIMARY KEY AUTO_INCREMENT"]
    [:quote_hash "char(32) NOT NULL"]
    [:quote "varchar(4000) NOT NULL DEFAULT ''"]
    [:author_id :int]
    [:publish_date :timestamp]
    [:boosted :boolean]
    [:skip_count :int]
    [:display_count :int]
    [:share_count :int]
    [:like_count :int]
    [:report_count :int]
    ["FOREIGN KEY (author_id) REFERENCES AUTHORS(id) ON DELETE CASCADE"]))
  (sql/db-do-prepared db-spec "CREATE UNIQUE INDEX quote_index ON quotes (quote_hash)"))


(defn- create-quote-categories-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :quote_categories
    [:quote_id :int]
    [:category_id :int]
    ["FOREIGN KEY (quote_id) REFERENCES QUOTES(id) ON DELETE CASCADE"]
    ["FOREIGN KEY (category_id) REFERENCES CATEGORIES(id) ON DELETE CASCADE"]))
  (sql/db-do-prepared db-spec "CREATE UNIQUE INDEX quote_category_index ON quote_categories (quote_id, category_id)"))


(defn- create-users-quotes-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :users_quotes
    [:user_id :int]
    [:quote_id :int]
    [:skip_flag :boolean]
    [:like_flag :boolean]
    [:share_flag :boolean]
    [:report_flag :boolean]
    ["FOREIGN KEY (user_id) REFERENCES USERS(id) ON DELETE CASCADE"]
    ["FOREIGN KEY (quote_id) REFERENCES QUOTES(id) ON DELETE CASCADE"]))
   (sql/db-do-prepared db-spec "CREATE INDEX user_id_index ON users_quotes (user_id)"))

(defn- create-stats-by-date-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :stats_by_date
    [:quote_id :int]
    [:display_date :date]
    [:skip_count :int]
    [:display_count :int]
    [:share_count :int]
    [:like_count :int]
    [:report_count :int]
    ["FOREIGN KEY (quote_id) REFERENCES QUOTES(id) ON DELETE CASCADE"])))


(defn- create-ip-blacklist-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :ip_blacklist
    [:banned_ip "varchar(15) NOT NULL"]
    [:banned_at :timestamp])))


(defn- create-user-blacklist-table
  []
  (sql/db-do-commands
   db-spec
   (sql/create-table-ddl
    :user_blacklist
    [:banned_user_id :int]
    [:banned_at :timestamp]
    ["FOREIGN KEY (banned_user_id) REFERENCES USERS(id) ON DELETE CASCADE"])))


(defn- create-tables
  "creates the database tables used by the application"
  []
;  (drop-tables)
  (create-users-table)
  (create-authors-table)
  (create-categories-table)
  (create-quotes-table)
  (create-quote-categories-table)
  (create-users-quotes-table)
  (create-stats-by-date-table)
  (create-ip-blacklist-table)
  (create-user-blacklist-table))

; (create-tables)
