(ns picture-gallery.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [mount.core :refer [defstate]]
            [picture-gallery.config :refer [env]]))

(defstate db*
  :start (-> env :mongodb-uri mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defn get-user [query]
  (mc/find-one-as-map db "users" query))

(defn get-all-users []
  (mc/find-maps db "users" {}))

(defn create-user! [user]
  (if (get-user (select-keys user [:id]))
    (throw (ex-info "Duplicate ID" {:desc "User ID already exists"}))
    (mc/insert db "users" user)))

(defn delete-user! [query]
  (mc/remove db "users" query))

(defn update-user! [id first-name last-name email]
  (mc/update db "users" {:_id id}
             {$set {:first_name first-name
                    :last_name  last-name
                    :email      email}}))