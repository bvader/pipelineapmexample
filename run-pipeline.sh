#!/bin/bash
# set -x

export TAG
AGENT_VERSION=${TAG}
AGENT_FILE=elastic-apm-agent-${AGENT_VERSION}.jar

if [ ! -f "${AGENT_FILE}" ]; then
  curl -O  https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/${AGENT_VERSION}/elastic-apm-agent-${AGENT_VERSION}.jar
fi

./gradlew clean assemble
./run-processor.sh 1 Source 7070,7071,7072 &
./run-processor.sh 2 Process1 7070,7071,7072 &
./run-processor.sh 3 Process2 7070,7071,7072 &
./run-processor.sh 4 Sink 7070,7071,7072 &


