(ns squares.backend
  (:require [compojure.core :as compojure]
            [ring.middleware.defaults :as ring]
            [ring.adapter.jetty9 :as jetty]
            [ring.adapter.jetty9.websocket :as ws]
            [clojure.data.json :as json])
  (:gen-class)
  (:import (org.eclipse.jetty.server Server)))

(defonce colors #{"Black" "Blue" "Brown" "Chartreuse" "Chocolate" "Crimson" "Cyan" "Gray" "Teal"
                  "Green" "BlueViolet" "Khaki" "LimeGreen" "Magenta" "Maroon" "Navy" "Yellow"
                  "Olive" "Orange" "Orchid" "Pink" "Plum" "Purple" "Red" "Salmon" "DarkTurquoise"})

;; Here we store the connections to all the connected WebSocket clients.
;; We also store in here the global application state (where each square is located).
(defonce ws-connections (atom {}))

(defn broadcast
  "This function sends the latest global application state to all connected WebSocket clients."
  []
  (let [current-connections (vals @ws-connections)
        global-app-state (map #(dissoc % :ws) current-connections)]
    (doseq [ws-connection current-connections]
      (ws/send!
       (:ws ws-connection)
       (json/write-str {:you (:color ws-connection) :all global-app-state})))))

(defn detected-collision?
  "Helper function that checks if [x y] is inaccessible: out of the grid or already occupied."
  [x y]
  (let [matches (filter #(and (= x (:x %)) (= y (:y %))) (vals @ws-connections))]
    (or (not-empty matches) (>= x 14) (>= y 14) (< x 0) (< y 0))))

(defn find-free-place []
  "Helper function that finds a random free place on the grid for a new square."
  (let [x (rand-nth (range 14))
        y (rand-nth (range 14))]
    (if (detected-collision? x y) (find-free-place) {:x x :y y})))

(defonce ws-handler
  ;; When a new client connects via WebSocket, we assign them a random color and free location.
  ;; We add their WebSocket connection to ws-connections so that in the future we can broadcast to them.
  {:on-connect
   (fn [ws]
     (let [free-color (try
                        (rand-nth (into [] (clojure.set/difference
                                            colors
                                            (into #{} (map :color (vals @ws-connections))))))
                        (catch Exception _ nil)) ;; if we ran out of colors after index.html was rendered
           free-place (find-free-place)]
       (when free-color
         (swap! ws-connections assoc (str ws) {:color free-color
                                               :x (:x free-place)
                                               :y (:y free-place)
                                               :ws ws})
         (broadcast))))

   ;; When a client closes, goes to another web page or refreshes, we automatically receive this event.
   :on-close
   (fn [ws _ _]
     (swap! ws-connections dissoc (str ws))
     (broadcast))

   ;; Here we handle the key presses of arrow keys that we receive, and update the global state accordingly.
   :on-text
   (fn [ws text-message]
     (let [json-message (json/read-str text-message :key-fn keyword)
           ws-id (str ws)
           x (:x (get @ws-connections ws-id))
           y (:y (get @ws-connections ws-id))]
       (case (:keycode json-message)
         38 (if (not (detected-collision? x (dec y))) (swap! ws-connections update-in [ws-id :y] dec))
         40 (if (not (detected-collision? x (inc y))) (swap! ws-connections update-in [ws-id :y] inc))
         37 (if (not (detected-collision? (dec x) y)) (swap! ws-connections update-in [ws-id :x] dec))
         39 (if (not (detected-collision? (inc x) y)) (swap! ws-connections update-in [ws-id :x] inc))))
     (broadcast))})

;; Here we serve the index.html page.
(compojure/defroutes web-app-routes
  (compojure/GET "/index.html" []
    (str "<!DOCTYPE html>\n
          <html>\n
          <head><meta charset=\"utf-8\"><title>Squares</title></head>\n
          <body>"
         (if (< (count (vals @ws-connections)) (count colors))
           (str "</body>\n
                 <script type=\"text/javascript\">\n\n"
                (slurp "resources/public/js/compiled/app.js")
                "\n\n</script></html>")
           ;; If we have run out of square colors, instead of the JavaScript we return the message below:
           "<div>We have reached the maximum number of connections. Please try later.</div></body></html>"))))

;; We define the web app and the web server
(defonce web-app (ring/wrap-defaults web-app-routes ring/site-defaults))
(defonce web-server (atom nil))

(defn stop-web-server
  "Function for stopping the web server, which is something that can be useful when using a REPL."
  []
  (when-let [jetty @web-server]
    (.stop ^Server jetty)
    (reset! web-server nil)))

(defn start-web-server
  "When using a REPL, we want to set join? to false so that the web server remains in a separate thread."
  [join?]
  (stop-web-server)
  (reset! web-server (jetty/run-jetty web-app {:port 3000
                                               :join? join?
                                               :websockets {"/websockets/" ws-handler}})))

(defn -main []
  (start-web-server true))
