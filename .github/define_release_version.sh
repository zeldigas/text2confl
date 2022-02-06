#!/usr/bin/env bash

set -eu

input_version=${1:-}

if [[ "${GITHUB_REF:-}" == refs/tags/v* ]]; then
  echo "Git tag is found using it for version"
  version=${GITHUB_REF#refs/tags/v}
elif [[ ! -z "$input_version" ]]; then
  echo "Version provided explicitly, using it"
  version=$input_version
else
  echo "Version not found, using simple build"
  version=""
fi

echo "Version: $version"

echo "RELEASE_VERSION=$version" >> $GITHUB_ENV
if [[ -z "$version" ]]; then
echo "RELEASE_VERSION_SUFFIX=" >> $GITHUB_ENV
echo "RELEASE_DOCKER_TAG=latest" >> $GITHUB_ENV
else
echo "RELEASE_VERSION_SUFFIX=-$version" >> $GITHUB_ENV
echo "RELEASE_DOCKER_TAG=$version" >> $GITHUB_ENV
fi