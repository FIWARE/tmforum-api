#!/bin/bash

elements=""
while read -r i; do
  module=$(echo $i | jq -c -r '.module');
  url=$(echo $i | jq -c -r '.url');
  mkdir -p ../api/tm-forum/$module
  result=$(curl -s -o /dev/null -w "%{http_code}" $url)
  if [ $result != 200 ]; then
    elements="${elements} ${i}"
  else
    echo "Download" $module
    wget -O api.json $url
    mv api.json ../api/tm-forum/$module
  fi
  echo $elements
done <<< $(jq -c -r '.[]' ./spec.json)

json='[]'
for e in $elements
do
  module=$(echo $e | jq -c -r '.module');
  url=$(echo $e | jq -c -r '.url');
  json=$(jq --arg modVar "$module" --arg urlVar "$url" '. |= . + [{"module":$modVar, "url":$urlVar}]' <<< $json)
done

echo $json > not-available.json
