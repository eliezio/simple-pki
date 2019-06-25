#!/bin/bash
# This script will build the project.

set -euo pipefail

export TERM=dumb

SWITCHES="--stacktrace"

echo TRAVIS Environment Variables
echo ----------------------------
env | grep -E "^TRAVIS_"
echo ----------------------------

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo -e "Build Pull Request #$TRAVIS_PULL_REQUEST => Branch [$TRAVIS_BRANCH]"
  ./gradlew build $SWITCHES
elif [ -z "$TRAVIS_TAG" ]; then
  echo -e 'Build Branch with Snapshot => Branch ['$TRAVIS_BRANCH']'
  ./gradlew -Prelease.travisci=true snapshot $SWITCHES
else
  echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'
  case "$TRAVIS_TAG" in
  *-rc\.*)
    ./gradlew -Prelease.travisci=true -Prelease.useLastTag=true candidate $SWITCHES
    ;;
  *)
    ./gradlew -Prelease.travisci=true -Prelease.useLastTag=true final $SWITCHES
    ;;
  esac
fi
