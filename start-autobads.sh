#!/bin/bash

# Start Auto-BADS in test mode
cd /Users/univers/projects/agentmesh/Auto-BADS
mvn spring-boot:run -Dspring-boot.run.profiles=test
