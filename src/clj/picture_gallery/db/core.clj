(ns picture-gallery.db.core
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.conversion :refer [from-db-object]]
            [monger.operators :refer :all]
            [mount.core :refer [defstate]]
            [picture-gallery.config :refer [env]]
            [monger.gridfs :as gfs :refer [store-file make-input-file filename content-type metadata]])
  (:import (com.mongodb.gridfs GridFSDBFile)))

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

(defn delete-user! [id]
  (mc/remove db "users" {:id id}))

(defn get-image [filename]
  (when-let [gfsfile (gfs/find-one fs {:metadata.name filename})]
    (assoc (:metadata (from-db-object gfsfile true))
      :data (.getInputStream ^GridFSDBFile gfsfile))))

(defn save-file! [file]
  (if (gfs/find-one fs {:metadata.name (:name file)})
    (throw (ex-info "Duplicate file name" {:desc "File name already exists"}))
    (store-file (make-input-file fs (:data file))
      (metadata (dissoc file :data)))))

(defn list-thumbnails [owner]
  (map #(select-keys (:metadata %) [:name :owner])
       (gfs/find-maps fs {:metadata.owner owner
                          :metadata.name  {$regex "^thumb_"}})))

(defn- get-one-thumbnail [owner]
  (-> fs
      (gfs/find-one-as-map {:metadata.owner owner
                            :metadata.name  {$regex "^thumb_"}})
      :metadata
      (select-keys [:name :owner])))

(defn select-gallery-previews []
  (let [users (get-all-users)]
    (filter not-empty (map #(get-one-thumbnail (:id %)) users))))

(defn delete-file! [owner name]
  (gfs/remove fs {:metadata.owner owner
                  :metadata.name name}))

(defn delete-user-images! [owner]
  (gfs/remove fs {:metadata.owner owner}))

(defn delete-account! [id]
  (delete-user! id)
  (delete-user-images! id))