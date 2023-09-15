#!/usr/bin/env bash
set -e

# Script to start file server for development purposes.

SCRIPT_PATH=$(readlink -f "${0}")
SCRIPT_DIR=$(dirname "${SCRIPT_PATH}")

# Test is file server is found at assumed parent and environment variable not set.
# If so echo message and exit
if [[ ! -d "${FILE_SERVER_DIR}" ]]; then
  echo Could not find the file-server project source code
  echo Set an enviroment variable FILE_SERVER_DIR to the root of the file-server project
  exit 1
fi

# When running multiple Java versions, and Java 17 is not the standard version
# you can set an environment variable JAVA_17_HOME to the Java 17 installation.
: ${JAVA_17_HOME:=${JAVA_HOME}}
export JAVA_HOME=${JAVA_17_HOME}

# File server configuration options. Set as environment variable before starting this script to override the default values.

# Uses local storage by default
: ${AERIUS_FILE_PROFILES_ACTIVE:="local"}
# Location the files are stored
: ${AERIUS_FILE_STORAGE_LOCATION:=/tmp/aerius_file_upload}
# Port the file server is running on
: ${AERIUS_FILE_SERVER_PORT:=8060}

cd "${SCRIPT_DIR}/../source"

mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=${AERIUS_FILE_SERVER_PORT} -Dspring-boot.run.profiles=${AERIUS_FILE_PROFILES_ACTIVE}
