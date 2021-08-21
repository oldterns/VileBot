#!/bin/sh
DOCKER=''
if command -v docker &> /dev/null
then
  DOCKER='docker'
fi
if command -v podman &> /dev/null
then
  DOCKER='podman'
fi
if [ -z "$DOCKER" ]
then
  echo "Could not find docker or podman. Aborting"
  exit 1
fi

echo "Starting test redis docker container..."
$DOCKER run -it --rm=true --memory-swappiness=0 --name redis_quarkus_test -p 6379:6379 redis:5.0.6
