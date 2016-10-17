(ns picture-gallery.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.operators :refer :all]
            [mount.core :refer [defstate]]
            [picture-gallery.config :refer [env]]
            [monger.gridfs :as gfs :refer [store-file make-input-file filename content-type metadata]]))

(defstate db*
  :start (-> env :mongodb-uri mg/connect-via-uri)
  :stop (-> db* :conn mg/disconnect))

(defstate db
  :start (:db db*))

(defstate fs
  :start (mg/get-gridfs (:conn db*) (re-find #"\w+$" (:mongodb-uri env))))

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

(defn save-file! [file]
  (store-file (make-input-file fs (:data file))
    (metadata (dissoc file :data))))




