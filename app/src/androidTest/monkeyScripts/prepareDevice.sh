#!/bin/bash
# if you are running this on windows, pls use cygwin
# This script will read in a directory of apks. It will make a file that holds all package names derived from those apks. It will then loop through and try to install the apks on the device

# example: /cygdrive/c/Users/levanhieu/AppData/Local/Android/sdk/platform-tools/adb.exe
ADB=""
# example: /cygdrive/c/Users/levanhieu/AppData/Local/Android/sdk/build-tools/25.0.2/aapt.exe
AAPT=""

if [[ -z $1 ]]; then
  echo "Please pass in a directory that holds only apks you want to install on your device"
  exit 1
fi

shopt -s nullglob

# first argument is the package file name
APK_DIRECTORY=$1

# get an array of all apks in the directory
APKS=$(ls $APK_DIRECTORY | grep .apk | sort)

# loop through and grab packageName of each apk
packageNamesArray=()
for i in $APKS; do
  packageName=$($AAPT dump badging "$APK_DIRECTORY/$i" | awk '/package:/{gsub("name=|'"'"'","");  print $2}')
  packageNamesArray+=( "$packageName" )
done

# output the package names to a file to be used later
printf "%s\n" "${packageNamesArray[@]}" > "packageNamesFound.txt"

# install the apks if they are not present on the phone
for i in $APKS; do
  packageName=$($AAPT dump badging "$APK_DIRECTORY/$i" | awk '/package/{gsub("name=|'"'"'","");  print $2}')
  exist=$($ADB shell pm list packages | grep $packageName)
  if [ -z $exist ]; then
    $ADB install -r $APK_DIRECTORY/$i
  fi
done
