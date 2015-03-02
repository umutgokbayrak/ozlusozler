(ns ozlusozler.routes.home
  (:require
   [clojure.pprint :refer [pprint]]
   [noir.cookies :as cookies]
   [noir.response :refer [edn redirect]]
   [compojure.core :refer :all]
   [ozlusozler.util :as util]
   [ozlusozler.services.users :as users]
   [ozlusozler.services.quotes :as quotes]
   [ozlusozler.layout :as layout]))

(defn about-page []
  (layout/render
   "about.html" {:docs (util/md->html "/md/about.md")}))


(defn- land-on-page []
  (let [user-hash (cookies/get :user-hash)]
    (if (nil? user-hash)
      (let [user-hash-new (users/land-on-site)]
        (cookies/put! :user-hash {:value user-hash-new :expires (util/cookie-expire)})
        user-hash-new)
      user-hash)))


(defn- get-quote [user-hash category-id author-id]
  (let [quote-hash (cookies/get :quote-hash)]
    (if (or (nil? quote-hash) (= quote-hash ""))
      (quotes/get-quote
       user-hash
       {:category-id (try (Integer/parseInt category-id) (catch Exception e nil))
        :author-id (try (Integer/parseInt author-id) (catch Exception e nil))})
      (quotes/quote-by-hash quote-hash))))


(defn quote-page [category-id author-id]
  (let [user-hash (land-on-page)
        quote     (get-quote user-hash category-id author-id)]
    (cookies/put! :quote-hash (:quote_hash quote))
    (layout/render "app.html" {:quote quote})))


(defn like-page [category-id author-id]
  (let [user-hash (land-on-page)
        quote-hash (cookies/get :quote-hash)
        quote (quotes/quote-by-hash quote-hash)]
    (quotes/like-quote (:id quote) user-hash)
    (cookies/put! :quote-hash "")
    (redirect "/")))


(defn dislike-page [category-id author-id]
  (let [user-hash (land-on-page)
        quote-hash (cookies/get :quote-hash)
        quote (quotes/quote-by-hash quote-hash)]
    (quotes/skip-quote (:id quote) user-hash)
    (cookies/put! :quote-hash "")
    (redirect "/")))


(defroutes home-routes
  (GET "/" [cat author] (quote-page cat author))
  (GET "/like" [cat author] (like-page cat author))
  (GET "/dislike" [cat author] (dislike-page cat author)))
