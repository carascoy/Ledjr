#!/bin/bash
set -e

docker build -t ledjr-backend .
docker run -q ledjr-backend