#!/bin/sh
set -u
set -e
set -f

: "${VB_REDIS_CONF_PATH:=cfg/redis.conf}"
: "${VB_REDIS_DB_LOCATION:=db}"

case $VB_REDIS_CONF_PATH in
  /*) : ;;
  *) VB_REDIS_CONF_PATH=$(pwd)/$VB_REDIS_CONF_PATH ;;
esac

case $VB_REDIS_DB_LOCATION in
  /*) : ;;
  *) VB_REDIS_DB_LOCATION=$(pwd)/$VB_REDIS_DB_LOCATION ;;
esac

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


mkdir -p "$VB_REDIS_DB_LOCATION"
mkdir -p log

echo "Starting production redis docker container..."
$DOCKER run -it --rm=true --name redis_quarkus_prod -p 6379:6379 -v "$VB_REDIS_CONF_PATH":/usr/local/etc/redis/redis.conf:ro -v "$VB_REDIS_DB_LOCATION":/data:z redis:5.0.6
