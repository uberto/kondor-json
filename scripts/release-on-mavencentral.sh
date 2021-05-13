#!/bin/bash

if [ $# -eq 0 ]; then
  echo "You need to pass the version! (and nothing else)"
  exit 1
fi

# Steps:
# verify it's all working with a
./gradlew clean build

# update the version in build.gradle
ver=$(grep "version '" build.gradle)
echo currrent: $ver
#grep from
echo updating to $1
# update the README.md
# check the CHANGELOG.md

# launch:
./gradlew uploadArchives -p kondor-core
./gradlew uploadArchives -p kondor-tools
./gradlew uploadArchives -p kondor-outcome

# then go to sonatype site and login
# https://oss.sonatype.org/#nexus-search;quick~kondor
# select Staging Repositories, and close the corresponding one (empty desc is fine)
# then click release and wait ~10 min to be able to download it
# and then bouncing the version with SNAPSHOT in build.gradle
# commit new shapshot version
