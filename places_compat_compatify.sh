#!/usr/bin/env bash

# Run this script to convert an Android Studio project from using the old Google Play Services
# Places library to the new Standalone Places Compat Library.
#  $1 = Directory to the Android Studio project
#  $2 = Maven version of the Compat Library to use, e.g 1.0.0

# Recursively replaces all occurrences of a given string in the specified files.
# Files in the build, .idea, .git, and .svn dirs are ignored.
#  $1 = Directory to run findReplace on.
#  $2 = Regexp expression of filename to run findReplace on.
#  $3 = Source string to search for (may be regexp).
#  $4 = Replacement string.
function findReplace {
  find "${1}" -type f -regex "${2}" -not -regex ".*/\(build\|\.idea\|\.git\|\.svn\)/.*" | xargs sed -i "s/${3}/${4}/g"
}

if [[ "$#" -ne 2 ]]; then
    echo -e "ERROR: Invalid parameters, excepted parameters:\n\t$0 <path to project> <compat version>"
    exit 1
fi

GRADLE_FILE_REGEXP=".*/build\.gradle"
JAVA_FILES_REGEXP=".*\.java"
XML_FILES_REGEXP=".*\.xml"
JAVA_AND_XML_FILES_REGEXP=".*\.\(java\|xml\)"

# 1. Replace the Google Play Services Places client in gradle file with the Compat library.
OLD_PLACES_DEPENDENCY="com.google.android.gms:play-services-places:[0-9]*.[0-9]*.[0-9]*"
NEW_COMPAT_PLACES_DEPENDENCY="com.google.android.libraries.places:places-compat:${2}"
findReplace "${1}" "${GRADLE_FILE_REGEXP}" "${OLD_PLACES_DEPENDENCY}" "${NEW_COMPAT_PLACES_DEPENDENCY}"

# 2. Find and replace all Google Play Services Places packages with the Compat equivalents.
OLD_PLACES_PACKAGE="com.google.android.gms.location.places"
NEW_COMPAT_PLACES_PACKAGE="com.google.android.libraries.places.compat"
findReplace "${1}" "${JAVA_AND_XML_FILES_REGEXP}" "${OLD_PLACES_PACKAGE}" "${NEW_COMPAT_PLACES_PACKAGE}"

# 3. Update the powered_by_google assets:
# matches: R.drawable.powered_by_google_dark (and light)
OLD_PLACES_ASSET_JAVA="\(R\.drawable\.\)\(powered_by_google_\)\(light\|dark\)\([^a-zA-Z0-9_\$]\)"
NEW_PLACES_ASSET_JAVA="\1places_\2\3\4"
findReplace "${1}" "${JAVA_FILES_REGEXP}" "${OLD_PLACES_ASSET_JAVA}" "${NEW_PLACES_ASSET_JAVA}"

# matches: "@drawable/powered_by_google_dark" (and light)
OLD_PLACES_ASSET_XML="\"\(@drawable\/\)\(powered_by_google_\)\(light\|dark\)\""
NEW_PLACES_ASSET_XML="\"\1places_\2\3\""
findReplace "${1}" "${XML_FILES_REGEXP}" "${OLD_PLACES_ASSET_XML}" "${NEW_PLACES_ASSET_XML}"