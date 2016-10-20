(ns picture-gallery.routes.services.gallery
  (:require [picture-gallery.layout :refer [error-page]]
            [picture-gallery.db.core :as db]
            [ring.util.http-response :refer :all]))

(defn get-image [name]
  (if-let [{:keys [data type]} (db/get-image name)]
    (-> data
        ok
        (content-type type))
    (error-page {:status 404
                 :title "image not found"})))

(defn list-thumbnails [owner]
  (ok (db/list-thumbnails owner)))

(defn list-galleries []
  (ok (db/select-gallery-previews)))