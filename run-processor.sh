#!/bin/bash
java "-javaagent:./elastic-apm-agent-1.13.0.jar" \
-Delastic.apm.service_name=$2 \
-Delastic.apm.server_urls="http://localhost:8200" \
-Delastic.apm.secret_token="" \
-Delastic.apm.application_packages="org.pipelineexample.apm.processor" \
-jar ./build/libs/pipelineapmexample-1.0-SNAPSHOT.jar $1 $3 false
