(ns squares.frontend
  (:require-macros [cljs.core.async.macros :as macros])
  (:require [cljs.core.async :as async]
            [haslett.client :as ws]
            [reagent.core :as reagent]
            [reagent.dom :as dom]))

(defn parse-json
  "Helper function that parses a stringified json into a data structure."
  [json-as-string]
  (js->clj (.parse js/JSON json-as-string) :keywordize-keys true))

(defn encode-json
  "Helper function that encodes a data structure into a stringified json."
  [data-structure]
  (.stringify js/JSON (clj->js data-structure)))

;; The state of the frontend is stored in this data structure.
(defonce state (reagent/atom {:you "White" :all []}))

(def ws-connection "WebSocket connection to the server." nil)

(defn send-message
  "Send a message to the server using the WebSocket connection."
  [data-structure]
  (if ws-connection
    (macros/go (async/>! (:sink ws-connection)
                         (encode-json data-structure)))))

(defn cell-color
  "Helper function that calculates the color of every square in the grid."
  [x y]
  (let [matches (filter #(and (= x (:x %)) (= y (:y %))) (:all @state))]
    (if (empty? matches)
      "White"
      (:color (first matches)))))

(defn page
  "We define the webpage as a function of the frontend state."
  []
  [:div
   [:table {:style {:border-collapse :collapse} :cellSpacing "0"}
    [:tbody
     (doall (for [y (range 14)]
              [:tr
               {:key (str "tr-" y)} ;; React requires such keys.
               (doall (for [x (range 14)]
                        [:td {:style {:background-color (cell-color x y)
                                      :width "40px"
                                      :height "40px"}
                              :key (str "td-" x "-" y)
                              :on-click #(send-message {:type "click"
                                                        :x x
                                                        :y y})}]))]))]]
   [:div
    [:span "YOU ARE "]
    [:span {:style {:color (:you @state) :font-size "150%"}} "■"]
    [:span " — MOVE USING ↑→↓← OR CLICK ANYWHERE TO TELEPORT"]]])

;; We render the webpage using React.
(dom/render [page] (.-body js/document))

;; We create an event listener that sends arrow key presses
;; to the server via the WebSocket connection.
(.addEventListener js/document "keydown"
                   (fn [event]
                     (let [keycode (.-keyCode event)]
                       (send-message {:type "key-press"
                                      :keycode keycode}))))

;; We establish a WebSocket connection to the server.
(macros/go
  (set! ws-connection (async/<! (ws/connect "ws://localhost:3000/websockets/")))

  ;; In a loop we listen to WebSocket messages that arrive from the server
  ;; and when they arrive we update the frontent state based on them.
  (while ws-connection
    (let [received (async/<! (:source ws-connection))]
      (if (nil? received) ;; When the connection closes, we receive a null message.
        (do
          (js/alert (str "Please refresh. The connection to the server was lost due to: "
                         (encode-json (async/<! (:close-status ws-connection)))))
          (set! ws-connection nil))
        (reset! state (parse-json received))))
    (async/<! (async/timeout 1)))) ;; The timeout makes the loop behave a bit better.
