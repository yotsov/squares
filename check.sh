lein cljfmt fix
lein cljfmt fix project.clj
find src-cljs -name "*.cljs" | xargs lein cljfmt fix

lein ancient
lein ancient :plugins

lein deps :tree > /dev/null
lein deps :plugin-tree > /dev/null

lein eastwood
lein kibit
