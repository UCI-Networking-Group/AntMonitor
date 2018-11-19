#!/bin/bash
# if you are running this on windows, pls use cygwin
# This script will read in a file with package names. It will then uninstall the apps corresponding to those package names.

if [[ -z $1 ]]; then
  echo "Please pass in a file that has lists of packages to uninstall line"
  exit 1
fi

# first argument is the package file name
PACKAGE_FILE=$1
# example: /cygdrive/c/Users/levanhieu/AppData/Local/Android/sdk/platform-tools/adb.exe
ADB=""

# put packages into an array
mapfile -t packages_to_test < $PACKAGE_FILE

# loop through packages to test
for i in "${packages_to_test[@]}"
do
	exist=$($ADB shell pm list packages | grep $i)
	if [ -n "$exist" ]; then
		echo "Uninstalling $i..."
		# line should be a package name
		$ADB uninstall $i -v
		echo "Done with uninstall $i"
		echo
    else
		echo "Package not found to uninstall: $i"
	fi

done
