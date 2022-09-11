#!/bin/bash

output="./website/content/changelog.md"

if [ -f "$output" ] ; then
    rm "$output"
fi

echo -e '+++\ntitle = "What'\''s New"\nweight = 0\n+++\n' >> "$output"
cat ./CHANGELOG.md >> "$output"
