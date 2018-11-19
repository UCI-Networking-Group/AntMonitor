#!/bin/bash
# if you are running this on windows, pls use cygwin
# This script will read in a file with package names. It will then loop through each package to do monkey tests on them. Configure how the tests will be run below. 

if [[ -z $1 ]]; then
  echo "Please pass in a file that has lists of packages to tests per line"
  exit 1
fi

# first argument is the package file name
PACKAGE_FILE=$1
# example: /cygdrive/c/Users/levanhieu/AppData/Local/Android/sdk/platform-tools/adb.exe
ADB=""

# monkey params
EVENTS_PER_APP=1000
# delay by millis
THROTTLE=100
# percent of EVENTS_PER_APP that will be touch events
PCT_TOUCH=30
# percent of EVENTS_PER_APP that will be motion events
PCT_MOTION=0
# percent of EVENTS_PER_APP that will be sys events like home, back, volumn controls
PCT_SYSKEYS=0
# percent of EVENTS_PER_APP that are not mentioned in other variables (catch-all)
PCT_ANY=0

# output a list of packages that exist on the device
# will use this later
# $ADB shell 'pm list packages -f' | sed -e 's/.*=//' | sort > "devicePackages.txt"

# put packages into an array
mapfile -t packages_to_test < $PACKAGE_FILE

# loop through packages to test
for i in "${packages_to_test[@]}"
do
    echo "Running autotests on $i"
    # line should be a package name
    $ADB shell monkey -p $i --throttle $THROTTLE --pct-touch $PCT_TOUCH --pct-motion $PCT_MOTION  --pct-anyevent $PCT_ANY --ignore-crashes --ignore-timeouts --pct-syskeys $PCT_SYSKEYS -v $EVENTS_PER_APP 
    echo "Done with $i"
    echo

done
