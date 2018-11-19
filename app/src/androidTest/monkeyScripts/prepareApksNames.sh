#!/bin/bash
# This script reads in current directory and change file names from spaces to underscores and get rids of certain characters. 
# Copy and paste this into a directory with apks then run it.

ls | while read -r FILE
do
    mv -v "$FILE" `echo $FILE | tr ' ' '_' | tr -d '[{}(),\!]' | tr -d "\'" | sed 's/_-_/_/g'`
done
