#!/bin/bash
set -e

echo "Building uberjar..."
lein uberjar

echo "Building native image..."
native-image \
  --report-unsupported-elements-at-runtime \
  --initialize-at-build-time \
  --no-server \
  --no-fallback \
  -H:+ReportExceptionStackTraces \
  -jar ./target/uberjar/sri-0.1.0-SNAPSHOT-standalone.jar \
  -H:Name=./target/sri

echo "Native binary created at: ./target/sri"