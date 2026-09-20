#!/bin/bash

if [ $# -eq 0 ]; then
  echo "You need to pass the version! (and nothing else)"
  exit 1
fi

# Steps:
# verify it's all working with a
./gradlew clean build -x test|| { echo "Build failed, not releasing"; exit 1; }

# update the version in build.gradle.kts
ver=$(./gradlew -q printVersion)

echo currrent: $ver

echo updating to: $1
# updating the README.md, CLAUDE.md and gradle
# only touch the version declarations: escape regex metachars in the old version and sed specials in the new one, and
# anchor on the surrounding syntax, so other text containing the version (e.g. numbers in examples) is left alone.
# -i.bak works with both GNU and BSD (macOS) sed
ver_re=$(printf '%s' "$ver" | sed 's/[.[\*^$/]/\\&/g')
new_rep=$(printf '%s' "$1" | sed 's/[&/\]/\\&/g')
sed -i.bak "s/version = \"$ver_re\"/version = \"$new_rep\"/" build.gradle.kts
sed -i.bak -e "s/<version>$ver_re<\/version>/<version>$new_rep<\/version>/" -e "s/:kondor-core:$ver_re'/:kondor-core:$new_rep'/" README.md
sed -i.bak "s/^Current version: $ver_re$/Current version: $new_rep/" CLAUDE.md
rm -f build.gradle.kts.bak README.md.bak CLAUDE.md.bak

if ! grep -qF "version = \"$1\"" build.gradle.kts || ! grep -qF "<version>$1</version>" README.md; then
  echo "Version not updated to $1 in build.gradle.kts/README.md, not releasing"
  exit 1
fi

# launch:
./gradlew publish -p kondor-outcome
./gradlew publish -p kondor-core
./gradlew publish -p kondor-auto
./gradlew publish -p kondor-tools
./gradlew publish -p kondor-mongo
./gradlew publish -p kondor-jackson


# Make sure you are authenticated on https://central.sonatype.com/publishing before launching this

# Read Central token username/password from Gradle properties or environment
# Expected property names: nexusUsername / nexusPassword
# You may also set CENTRAL_NAMESPACE to your Portal namespace (as shown at
# https://central.sonatype.com/publishing/namespaces). If not set, we'll try to
# infer it from the groupId.

NEXUS_USERNAME=${NEXUS_USERNAME:-$(./gradlew -q properties | awk -F': ' '/^nexusUsername:/ {print $2}')}
NEXUS_PASSWORD=${NEXUS_PASSWORD:-$(./gradlew -q properties | awk -F': ' '/^nexusPassword:/ {print $2}')}

# Fallback to Gradle properties via grep if the above didn't find them
if [ -z "$NEXUS_USERNAME" ] || [ -z "$NEXUS_PASSWORD" ]; then
  if [ -f gradle.properties ]; then
    NEXUS_USERNAME=${NEXUS_USERNAME:-$(grep '^nexusUsername=' gradle.properties | head -n1 | cut -d'=' -f2-)}
    NEXUS_PASSWORD=${NEXUS_PASSWORD:-$(grep '^nexusPassword=' gradle.properties | head -n1 | cut -d'=' -f2-)}
  fi
fi

if [ -z "$NEXUS_USERNAME" ] || [ -z "$NEXUS_PASSWORD" ]; then
  echo "Missing Central token credentials. Set nexusUsername/nexusPassword in ~/.gradle/gradle.properties or export NEXUS_USERNAME/NEXUS_PASSWORD."
  echo "You can generate a token at: https://central.sonatype.com/account"
  exit 2
fi

# Determine namespace: use CENTRAL_NAMESPACE if provided, else infer from group
GROUP_ID=$(./gradlew -q printVersion >/dev/null 2>&1; ./gradlew -q properties | awk -F': ' '/^group:/ {print $2}' | head -n1)
CENTRAL_NAMESPACE=${CENTRAL_NAMESPACE:-$GROUP_ID}

echo "Using Central namespace: $CENTRAL_NAMESPACE"

# Build Bearer token: base64(username:password)
AUTH_BEARER=$(printf '%s:%s' "$NEXUS_USERNAME" "$NEXUS_PASSWORD" | base64 -w 0 2>/dev/null || printf '%s:%s' "$NEXUS_USERNAME" "$NEXUS_PASSWORD" | base64)

echo "Triggering transfer to Central Publisher Portal..."
RESP_CODE=$(curl -s -o /tmp/central_upload_resp.json -w '%{http_code}' \
  -X POST "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/${CENTRAL_NAMESPACE}" \
  -H "Authorization: Bearer ${AUTH_BEARER}" \
  -H "Content-Type: application/json")

if [ "$RESP_CODE" != "200" ] && [ "$RESP_CODE" != "202" ]; then
  echo "Manual upload trigger failed with HTTP $RESP_CODE"
  echo "Response body:"
  cat /tmp/central_upload_resp.json
  echo
  echo "If you continue to see issues, you can inspect repositories via:"
  echo "  GET https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=${CENTRAL_NAMESPACE}"
  exit 3
else
  echo "Manual upload trigger succeeded (HTTP $RESP_CODE)."
  echo "Visit https://central.sonatype.com/publishing/deployments to review and release."
fi

echo "check the CHANGELOG.md"
echo "commit and push"
echo "then go to Central Portal: https://central.sonatype.com/publishing/deployments"
echo "Confirm the deployment and publish it."
