(ns picture-gallery.components.login
  (:require [ajax.core :as ajax]
            [reagent.core :refer [atom]]
            [reagent.session :as session]
            [picture-gallery.components.common :as c]))

(def timeout-ms (* 1000 60 30))

(defn session-timer []
  (when (session/get :identity)
    (if (session/get :user-event)
      (do
        (session/remove! :user-event)
        (js/setTimeout #(session-timer) timeout-ms))
      (session/remove! :identity))))

(defn login! [fields errors]
  (reset! errors nil)
  (ajax/POST "/login"
             {:params @fields
              :handler #(do
                         (session/remove! :modal)
                         (session/put! :identity (:id @fields))
                         (js/setTimeout session-timer timeout-ms)
                         (reset! fields nil))
              :error-handler #(reset! errors
                                      {:error (get-in % [:response :message])})}))

(defn login-form []
  (let [fields (atom {})
        errors (atom nil)]
    (fn []
      [c/modal
       [:div "Picture Gallery Login"]
       [:div
        [:div.well.well-sm
         [:strong " ✱ required field"]]
        [c/text-input "name" :id "your user ID" fields]
        [c/password-input "password" :pass "your password" fields]
        (for [error (vals @errors)]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(login! fields errors)}
         "Login"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancel"]]])))

(defn login-button []
  [:a.btn
   {:on-click #(session/put! :modal login-form)}
   "Login"])