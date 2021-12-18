# format the code
lein cljfmt fix
lein cljfmt fix project.clj
find src-cljs -name "*.cljs" | xargs lein cljfmt fix

# check for updatable dependencies and for dependency conflicts
lein ancient
lein ancient :plugins
lein deps :tree > /dev/null # we are only interested in stderr
lein deps :plugin-tree > /dev/null # we are only interested in stderr

# run the linters
lein eastwood
lein kibit
