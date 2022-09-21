#!/bin/bash

sh /opt/ctk/$CTK_SCRIPT_FOLDER/$CTK_RUN_SCRIPT

cp /opt/ctk/$CTK_SCRIPT_FOLDER/htmlResults.html /results/htmlResults.html
cp /opt/ctk/$CTK_SCRIPT_FOLDER/jsonResults.json /results/jsonResults.json