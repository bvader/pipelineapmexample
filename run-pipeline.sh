#!/bin/bash
./gradlew clean assemble
./run-processor.sh 1 Source 7070,7071,7072 &
./run-processor.sh 2 Process1 7070,7071,7072 &
./run-processor.sh 3 Process2 7070,7071,7072 &
./run-processor.sh 4 Sink 7070,7071,7072 &
