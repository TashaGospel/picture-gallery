(ns picture-gallery.components.registration
  (:require [reagent.core :refer [atom]]
            [picture-gallery.components.common :as c]
            [ajax.core :as ajax]
            [picture-gallery.validation :refer [registration-errors]]
            [reagent.session :as session]))

(defn register! [fields errors]
  (reset! errors (registration-errors @fields))
  (when-not @errors
    (ajax/POST "/register"
               {:params        @fields
                :handler       #(do
                                 (session/put! :identity (:id @fields))
                                 (reset! fields {})
                                 (session/remove! :modal))
                :error-handler #(reset!
                                 errors
                                 {:server-error (get-in % [:response :message])})})))

(defn registration-form []
  (let [fields (atom {})
        errors (atom nil)]
    (fn []
      [c/modal
       [:div "Picture Gallery Registration"]
       [:div
        [:div.well.well-sm
         [:strong " ✱ required field"]]
        [c/text-input "name" :id "enter a user name" fields]
        [c/password-input "password" :pass "enter a password" fields]
        [c/password-input "password" :pass-confirm "re-enter the password" fields]
        (for [error (vals @errors)]
          [:div.alert.alert-danger error])]
       [:div
        [:button.btn.btn-primary
         {:on-click #(register! fields errors)}
         "Register"]
        [:button.btn.btn-danger
         {:on-click #(session/remove! :modal)}
         "Cancel"]]])))

(defn delete-account! []
  (ajax/DELETE "/delete-account"
               {:handler #(do
                           (session/remove! :identity)
                           (session/put! :page :home))}))

(defn registration-button []
  [:a.btn
   {:on-click #(session/put! :modal registration-form)}
   "Register"])

(defn delete-account-modal []
  [c/modal
   [:div.alert.alert-danger "Delete Account!"]
   [:P "Are you sure you wish to delete the account and associated gallery?"]
   [:div
    [:button.btn.btn-primary
     {:on-click (fn []
                  (delete-account!)
                  (session/remove! :modal))}
     "Delete"]
    [:button.btn.btn-danger
     {:on-click (fn [] (session/remove! :modal))}
     "Cancel"]]])