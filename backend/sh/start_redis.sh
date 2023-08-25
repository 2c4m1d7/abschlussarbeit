#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <port> <path>"
    exit 1
fi


port=$1
path=$2

chmod 600 $path

redis-server $path --port $port > /dev/null 2>&1 &


max_attempts=5
interval=0.2  
attempt=1


while [ $attempt -le $max_attempts ]; do
  if redis-cli -h $REDIS_HOSTNAME -p $port ping > /dev/null 2>&1; then
    echo "Redis instance is up and running."
    exit 0
  fi

  sleep $interval
  ((attempt++))
done

echo "Failed to establish connection to Redis instance within the given timeout."
exit 1
