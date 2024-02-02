#!/bin/bash

echo "The following api specs are unavailable, please check if there are updates required." > message.md
echo "" >> message.md
echo "| Module | Url |" >> message.md
echo "|--------|-----|" >> message.md

while read -r i; do
  module=$(echo $i | jq -c -r '.module');
  url=$(echo $i | jq -c -r '.url');
  echo "| $module | $url |" >> message.md
done <<< $(jq -c -r '.[]' ./not-available.json)
