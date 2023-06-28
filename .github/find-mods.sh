#!/bin/bash

plugin_name="jib-maven-plugin"
modules_with_plugin=()

for module in $(find . -name "pom.xml" -type f); do
  if grep -q "<artifactId>$plugin_name</artifactId>" "$module"; then
    if [ $module!='./pom.xml' ]; then
      echo $module
      mod=$(mvn -f $module --also-make dependency:tree | grep maven-dependency-plugin | awk '{ print $(NF-1) }')
      modules_with_plugin+=("$mod")
    fi
  fi
done

resultList=""

if [ ${#modules_with_plugin[@]} -gt 0 ]; then
  for module in "${modules_with_plugin[@]}"; do
    resultList="${resultList} ${module}"
  done
else
  echo "No modules found with the '$plugin_name' plugin active."
fi

echo $resultList