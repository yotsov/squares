# Squares

Tiny web app illustrating WebSocket duplex communication. You are a square: move around and bump into other people's squares.

To start locally, you need to have a recent JDK and Leiningen installed, and then:

lein do clean, uberjar, cljsbuild once prod ; java -jar target/squares-1.0.0-standalone.jar

Then open (in several different tabs for maximum effect):

http://localhost:3000/index.html
