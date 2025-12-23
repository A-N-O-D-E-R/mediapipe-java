#!/usr/bin/env bash

BITBUCKET_DOWNLOAD_URL="https://api.bitbucket.org/2.0/repositories/${BITBUCKET_REPO_FULL_NAME}/downloads"

VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

# Check if TAG contains "SNAPSHOT"
if [[ "$VERSION" =~ SNAPSHOT ]]; then
  echo "VERSION ${VERSION} is a SNAPSHOT, exiting."
  exit 0
fi

curl https://img.shields.io/badge/latest_release-${VERSION}-green -o latest_release.svg

curl --fail -X POST \
  -H "Authorization: Bearer ${BITBUCKET_APP_PASSWORD}" \
  -F 'files=@latest_release.svg' \
  -H 'content-type: multipart/form-data' \
  $BITBUCKET_DOWNLOAD_URL

if [ $? -eq 0 ]; then
    echo "Badge uploaded"
else
    echo "Error, badge not uploaded"
fi