#!/usr/bin/env bash
set -e

# Script to start file server for development purposes.

SCRIPT_PATH=$(readlink -f "${0}")
SCRIPT_DIR=$(dirname "${SCRIPT_PATH}")

# File server configuration options. Set as environment variable before starting this script to override the default values.

# Uses local storage by default
: ${AERIUS_FILE_PROFILES_ACTIVE:="local"}
# Location the files are stored
: ${AERIUS_FILE_STORAGE_LOCATION:="/tmp/aeriusupload"}
# Port the file server is running on
: ${AERIUS_FILE_SERVER_PORT:=8083}

cd "${SCRIPT_DIR}/../source/file-server"

JAVA_HOME=${JAVA_17_HOME:=${JAVA_HOME}} \
  mvn spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=${AERIUS_FILE_SERVER_PORT} --aerius.file.storage.location=${AERIUS_FILE_STORAGE_LOCATION}" \
  -Dspring-boot.run.profiles=${AERIUS_FILE_PROFILES_ACTIVE}
