(ns squares.frontend
  (:require-macros [cljs.core.async.macros :as macros])
  (:require [cljs.core.async :as async]
            [haslett.client :as ws]
            [reagent.core :as reagent]
            [reagent.dom :as dom]))

;; The entire state of the frontend is stored in this data structure.
(defonce state (reagent/atom {:you "White" :all []}))

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
                              :key (str "td-" x "-" y)}]))]))]]
   [:div
    [:span "YOU ARE "]
    [:span {:style {:color (:you @state) :font-size "150%"}} "■"]
    [:span " — MOVE USING ↑→↓←"]]])

;; We render the webpage using React.
(dom/render [page] (.-body js/document))

(defn parse-json
  "Helper function that parses a stringified json into a data structure."
  [json-as-string]
  (js->clj (.parse js/JSON json-as-string) :keywordize-keys true))

(defn encode-json
  "Helper function that encodes a data structure into a stringified json."
  [data-structure]
  (.stringify js/JSON (clj->js data-structure)))

;; We establish a WebSocket connection to the server.
(macros/go (let [stream (async/<! (ws/connect "ws://localhost:3000/websockets/"))]

        ;; We create an event listener that sends arrow key presses
        ;; to the server via the WebSocket connection.
             (.addEventListener js/document "keydown"
                                (fn [event]
                                  (let [keycode (.-keyCode event)]
                                    (if (and (>= keycode 37) (<= keycode 40))
                                      (macros/go (async/>! (:sink stream)
                                                           (encode-json {:keycode keycode})))))))

             (while true ;; In an infinite loop
          ;; we listen to WebSocket messages that arrive from the server
          ;; and when they arrive we update the frontent state based on them.
               (reset! state (parse-json (async/<! (:source stream))))
               (async/<! (async/timeout 1)))))
