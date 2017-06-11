#!/usr/bin/env bash

echo "> Unpack/prepare"

mkdir -p work
#zcat articles.gz | head -20 > work/articles
cat articles > work/articles
cat work/articles | sed s/.html$//g | tr -s '_()-,' ' ' | tr '[:upper:]' '[:lower:]' > work/articles.clean

echo "> Anazyle titles"
env JAVA_OPTS="-Xms256m -Xmx8G" ./util/wordAnalyze.scala > work/analyzed.json.seq

echo "> Add word indexes (N: $(cat work/analyzed.json.seq | wc -l), this will take a while)"
cat work/analyzed.json.seq | jq -rc '.[1]' > work/analyzed.entries.json.seq
cat work/analyzed.json.seq | jq -rc '.[0]' > work/analyzed.keys.seq
./util/addLines.scala work/analyzed.entries.json.seq | sort -n -k1 | cut -f2 -d ' ' | jq -rc --raw-input '{"/": .}' > work/indexes.cids

echo "> Add search hashmap"
./util/putHashmap.scala work/analyzed.keys.seq work/indexes.cids


