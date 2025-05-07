#!/bin/bash

if [ $# -eq 0 ]; then
  echo "You need to pass the version! (and nothing else)"
  exit 1
fi

# Steps:
# verify it's all working with a
./gradlew clean build

# update the version in build.gradle.kts
ver=$(./gradlew -q printVersion)

echo currrent: $ver

echo updating to: $1
# update=ing the README.md and gradle
sed -i "s/$ver/$1/g" README.md build.gradle.kts

# launch:
./gradlew publish -p kondor-outcome
./gradlew publish -p kondor-core
./gradlew publish -p kondor-auto
./gradlew publish -p kondor-tools
./gradlew publish -p kondor-mongo
./gradlew publish -p kondor-jackson


echo "check the CHANGELOG.md"
echo "commit and push"
echo "then go to sonatype site and login"
echo "https://oss.sonatype.org/#nexus-search;quick~kondor"
# select Staging Repositories, and close the corresponding one (empty desc is fine)
# then click release and wait ~10 min to be able to download it
# and then bouncing the version with SNAPSHOT in build.gradle.kts
# commit new shapshot version
