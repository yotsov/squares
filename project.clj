(defproject squares "1.0.0"
  :description "Tiny web app illustrating WebSocket duplex communication.
                You are a square: move around and bump into other people's squares."

  :dependencies [;; backend:
                 [org.clojure/clojure "1.10.3"]
                 [compojure "1.6.2"] ;; library for handing http requests
                 [ring/ring-defaults "0.3.3"] ;; library for wiring up a web app
                 [info.sunng/ring-jetty9-adapter "0.16.0"] ;; contains a simple WebSocket library
                 [org.clojure/data.json "2.4.0"] ;; parsing and encoding json

                 ;; frontend:
                 [org.clojure/clojurescript "1.10.896"] ;; like Clojure but compiles to JavaScript
                 [reagent "1.1.0"] ;; ClojureScript wrapper around React
                 [cljsjs/react "17.0.2-0"]
                 [cljsjs/react-dom "17.0.2-0"]
                 [haslett "0.1.6"]] ;; WebSocket library on the browser side

  :plugins [[lein-cljsbuild "1.1.8"] ;; helps compile ClojureScript to JavaScript

            ;; optional plugins:

            [lein-ancient "1.0.0-RC3"] ;; finds updatable dependencies by running:
            ;; lein ancient ; echo "plugins:" ; lein ancient :plugins

            [lein-cljfmt "0.8.0"]] ;; for formatting Clojure code using the command:
            ;; lein do cljfmt fix, cljfmt fix src-cljs/squares/frontend.cljs, cljfmt fix project.clj

  :aot :all
  :main squares.backend
  :clean-targets ^{:protect false} ["target" "resources/public/js/compiled"]

  :cljsbuild
  {:builds
   [{:id "prod"
     :source-paths ["src-cljs/"]
     ;; :figwheel false
     :compiler {:optimizations :advanced
                :output-to "resources/public/js/compiled/app.js"}}]})
