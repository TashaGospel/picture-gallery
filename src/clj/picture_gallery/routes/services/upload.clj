(ns picture-gallery.routes.services.upload
  (:require [picture-gallery.db.core :as db]
            [ring.util.http-response :refer :all]
            [clojure.tools.logging :as log])
  (:import [java.awt.image AffineTransformOp BufferedImage]
           [java.io ByteArrayOutputStream File]
           java.awt.geom.AffineTransform
           javax.imageio.ImageIO))

(def thumb-size 150)
(def thumb-prefix "thumb_")

(defn scale [^BufferedImage img ratio width height]
  (let [scale (AffineTransform/getScaleInstance
                (double ratio) (double ratio))
        transform-op (AffineTransformOp.
                       scale AffineTransformOp/TYPE_BILINEAR)]
    (.filter transform-op img
             (BufferedImage. width height (.getType img)))))

(defn scale-image [file thumb-size]
  (let [^BufferedImage img (ImageIO/read ^File file)
        img-width (.getWidth img)
        img-height (.getHeight img)
        ratio (/ thumb-size img-height)]
    (scale img ratio (int (* img-width ratio)) thumb-size)))

(defn image->byte-array [img]
  (let [baos (ByteArrayOutputStream.)]
    (ImageIO/write ^BufferedImage img "png" baos)
    (.toByteArray baos)))

(defn save-image! [user {:keys [tempfile filename content-type]}]
  (try
    (let [db-file-name (str user (.replaceAll ^String filename "[^a-zA-Z0-9-_\\.]" ""))]
      (db/save-file! {:owner user
                      :type  content-type
                      :name  db-file-name
                      :data  tempfile})
      (db/save-file! {:owner user
                      :type  "image/png"
                      :data  (image->byte-array
                               (scale-image tempfile thumb-size))
                      :name  (str thumb-prefix db-file-name)}))
    (ok {:result :ok})
    (catch Exception e
      (log/error e)
      (internal-server-error "error"))))