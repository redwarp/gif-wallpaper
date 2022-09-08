#!/bin/bash

output="./website/content/pages/privacy.md"

if [ -f "$output" ] ; then
    rm "$output"
fi

echo -e '+++\ntitle = "Privacy policy"\npath = "privacy"\ntemplate = "page.html"\n+++\n' >> "$output"
cat ./docs/privacy.md >> "$output"