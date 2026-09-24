#!/bin/bash
set -e

mvn -q clean package
java -jar target/Ledjr-1.0-SNAPSHOT.jar