# Steps:
# verify it's all working with a ./gradlew clean build
# remove snapshot from the version manually in build.gradle
# update the README.md
# check the CHANGELOG.md
# launch:

./gradlew uploadArchives -p kondor-core

# then go to sonatype site and login
# https://oss.sonatype.org/#nexus-search;quick~pesticide
# select Staging Repositories, and close the corresponding one (empty desc is fine)1.
# then click release and wait ~10 min to be able to download it
# and then bouncing the version with SNAPSHOT in build.gradle