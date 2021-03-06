(ns squares.frontend
  (:require [reagent.core :as reagent]
            [reagent.dom :as dom]
            [malli.core :as m]))

(defn parse-json
  "Helper function that parses a stringified json into a data structure."
  [json-as-string]
  (js->clj (.parse js/JSON json-as-string) :keywordize-keys true))

(defn encode-json
  "Helper function that encodes a data structure into a stringified json."
  [data-structure]
  (.stringify js/JSON (clj->js data-structure)))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

;; The state of the frontend is stored in this data structure.
(defonce state (reagent/atom {:you "White" :all []}))

(defonce state-schema
  (m/schema [:map {:closed true}
             [:you :string]
             [:all [:sequential [:map {:closed true}
                                 [:x :int]
                                 [:y :int]
                                 [:color :string]]]]]))

(defn validate-state []
  (when-not (m/validate state-schema @state)
    (js/alert "State validation failed!")))

(def ws-connection "WebSocket connection to the server." nil)

(defn send-message
  "Send a message to the server using the WebSocket connection."
  [data-structure]
  (when ws-connection
    (.send ws-connection (encode-json data-structure))))

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
    [:span {:style {:color (:you @state) :font-size "150%"}} "???"]
    [:span " ??? MOVE USING ???????????? OR CLICK ANYWHERE TO TELEPORT"]]])

;; We render the webpage using React.
(dom/render [page] (.-body js/document))

(validate-state)

;; We create an event listener that sends arrow key presses
;; to the server via the WebSocket connection.
(.addEventListener js/document "keydown"
                   (fn [event]
                     (let [keycode (.-keyCode event)]
                       (when (in? [37 38 39 40] keycode)
                         (send-message {:type "key-press"
                                        :keycode keycode})))))

;; We establish a WebSocket connection to the server.
(set! ws-connection (js/WebSocket. "ws://localhost:3000/websockets/"))
(set! (.-binaryType ws-connection) "arraybuffer")

;; When a message arrives from the server we update the frontent state based on it.
(set! (.-onmessage ws-connection) (fn [e]
                                    (let [received (.-data e)]
                                      (if (= "full" received)
                                        (do
                                          (js/alert "Server cannot accept more connections. Refresh later.")
                                          (.close ws-connection 1000 "Closed by creator")
                                          (set! ws-connection nil))
                                        (do
                                          (reset! state (parse-json received))
                                          (validate-state))))))

;; Handle getting disconnected from the server.
(set! (.-onclose ws-connection)
      (fn [e]
        (set! ws-connection nil)
        (js/alert (str "Please refresh, the connection to the server was lost due to "
                       (.-reason e) " code " (.-code e) "."))))
