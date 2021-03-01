./gradlew uploadArchives -p kondor-core


# before launching it you need to update the version manually (remove snapshot)
# and then bouncing the version with SNAPSHOT after
# then go to sonatype site and login
# https://oss.sonatype.org/#nexus-search;quick~pesticide
# select Staging Repositories, and close the corresponding one.
# then click release and wait 10 min