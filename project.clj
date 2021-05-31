(defproject squares "1.0.0"
  :description "Tiny web app illustrating WebSocket duplex communication.
                You are a square: move around and bump into other people's squares."

  :dependencies [;; backend:
                 [org.clojure/clojure "1.10.3"]
                 [compojure "1.6.2"] ;; library for handing http requests
                 [ring/ring-defaults "0.3.2"] ;; library for wiring up a web app
                 [info.sunng/ring-jetty9-adapter "0.14.3"] ;; contains a simple WebSocket library
                 [org.clojure/data.json "2.3.1"] ;; parsing and encoding json

                 ;; frontend:
                 [org.clojure/clojurescript "1.10.866"] ;; like Clojure but compiles to JavaScript
                 [reagent "1.0.0"] ;; ClojureScript wrapper around React
                 [haslett "0.1.6"]] ;; WebSocket library on the browser side

  :plugins [[lein-cljsbuild "1.1.8"] ;; helps compile ClojureScript to JavaScript
            [lein-cljfmt "0.7.0"]] ;; for formatting Clojure code using the command:
            ;; lein do cljfmt fix, cljfmt fix src-cljs/squares/frontend.cljs, cljfmt fix project.clj

  :aot :all
  :main squares.backend
  :clean-targets ^{:protect false} ["target" "resources/public/js/compiled"]

  :cljsbuild
  {:builds
   [{;; To start locally, you need to have a recent JDK and Leiningen installed, and then:
     ;; lein do clean, uberjar, cljsbuild once prod ; java -jar target/squares-1.0.0-standalone.jar
     ;; Then open (in several different tabs for maximum effect):
     ;; http://localhost:3000/index.html
     :id "prod"
     :source-paths ["src-cljs/"]
     ;; :figwheel false
     :compiler {:optimizations :advanced
                :output-to "resources/public/js/compiled/app.js"}}]})
