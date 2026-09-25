#!/bin/bash
set -e

docker build -f Dockerfile.dev -t ledjr-frontend .
docker run -p 5173:5173 -v $(pwd):/app -v /app/node_modules frontend-dev