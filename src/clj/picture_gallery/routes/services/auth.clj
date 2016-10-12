(ns picture-gallery.routes.services.auth
  (:require [picture-gallery.db.core :as db]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [picture-gallery.validation :refer [registration-errors]]
            [clojure.tools.logging :as log]))

(defn register! [{:keys [session]} user]
  (if (registration-errors user)
    (response/precondition-failed
      {:result :error
       :message "Invalid parameters"})
    (try
      (-> user
          (dissoc :pass-confirm)
          (update :pass hashers/encrypt)
          db/create-user!)
      (-> {:result :ok}
          response/ok
          (assoc :session (assoc session :identity (:id user))))
      (catch Exception e
        (if-let [error-data (ex-data e)]
          (response/precondition-failed
            {:result :error
             :message (:desc error-data)})
          (do (log/error e)
              (response/internal-server-error
                {:result  :error
                 :message "Server error occurred while registering user"})))))))

(defn authenticate [{:keys [id pass]}]
  (when-let [user (db/get-user {:id id})]
    (hashers/check pass (:pass user))))

(defn login! [{:keys [session]} user]
  (if (authenticate user)
    (-> {:result :ok}
        response/ok
        (assoc :session (assoc session :identity (:id user))))
    (response/unauthorized {:result :unauthorized
                            :message "login failure"})))

(defn logout! []
  (-> {:result :ok}
      response/ok
      (assoc :session nil)))