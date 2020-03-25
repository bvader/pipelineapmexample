#!/bin/bash
AGENT_VERSION=1.13.0
curl -O https://repo1.maven.org/maven2/co/elastic/apm/elastic-apm-agent/${AGENT_VERSION}/elastic-apm-agent-${AGENT_VERSION}.jar
./gradlew clean assemble
./run-processor.sh 1 Source 7070,7071,7072 &
./run-processor.sh 2 Process1 7070,7071,7072 &
./run-processor.sh 3 Process2 7070,7071,7072 &
./run-processor.sh 4 Sink 7070,7071,7072 &


