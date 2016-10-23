(ns picture-gallery.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.api.upload :refer :all]
            [schema.core :as s]
            [picture-gallery.routes.services.auth :as auth]
            [picture-gallery.routes.services.upload :as upload]
            [picture-gallery.routes.services.gallery :as gallery]))

(s/defschema UserRegistration
  {:id           s/Str
   :pass         s/Str
   :pass-confirm s/Str})

(s/defschema UserLogin
  {:id   s/Str
   :pass s/Str})

(s/defschema Gallery
  {:name  s/Str
   :owner s/Str})

(s/defschema Result
  {:result                   s/Keyword
   (s/optional-key :message) s/Str})

(defapi service-routes
  {:swagger {:ui   "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version     "1.0.0"
                           :title       "Picture Gallery API"
                           :description "Public Services"}}}}

  (POST "/register" req
    :return Result
    :body [user UserRegistration]
    :summary "Register a new user"
    (auth/register! req user))

  (POST "/login" req
    :return Result
    :body [user UserLogin]
    :summary "Log the user in"
    (auth/login! req user))

  (POST "/logout" []
    :summary "remove user session"
    :return Result
    (auth/logout!))

  (GET "/gallery/:owner/:name" []
    :summary "display user image"
    :path-params [name :- String]
    (gallery/get-image name))

  (GET "/list-thumbnails/:owner" []
    :path-params [owner :- String]
    :summary "list thumbnails for images in the gallery"
    :return [Gallery]
    (gallery/list-thumbnails owner))

  (GET "/list-galleries" []
    :summary "lists a thumbnail for each user"
    :return [Gallery]
    (gallery/list-galleries)))

(defapi restricted-service-routes
  {:swagger {:ui   "/swagger-ui-private"
             :spec "/swagger-private.json"
             :data {:info {:version     "1.0.0"
                           :title       "Picture Gallery API"
                           :description "Private Services"}}}}

  (DELETE "/delete-image/:image-name" req
    :return Result
    :path-params [image-name :- s/Str]
    :summary "delete the specified file from the database"
    (gallery/delete-image! (:identity req) image-name))

  (POST "/upload" req
    :multipart-params [file :- TempFileUpload]
    :middleware [wrap-multipart-params]
    :summary "upload an image"
    :return Result
    (upload/save-image! (:identity req) file)))

