# Squares

Tiny web app illustrating WebSocket duplex communication. You are a square: move around and bump into other people's squares.

To start locally, you need to have a recent JDK and Leiningen installed, and then:

lein do cljsbuild once prod, clean, uberjar; java -jar target/squares-1.0.0-standalone.jar

Then open (in several different tabs for maximum effect):

http://localhost:3000/index.html

Alternatively, to run in development mode:

1. rm -rf resources/public/js/compiled; lein trampoline figwheel dev

2. In another console: rm -rf target; (echo "(start-web-server false)"; cat <&0) | lein repl

3. Open http://localhost:3000/index.html

4. In the console from step 1.: (js/alert "greetings from figwheel")

5. In the console from step 2.: (clojure.pprint/pprint @ws-connections)
