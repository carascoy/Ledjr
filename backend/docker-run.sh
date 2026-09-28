#!/bin/bash
set -e

docker build -t ledjr-backend .
docker run -q --network host --env-file .env ledjr-backend
