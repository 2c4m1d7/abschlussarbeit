#!/bin/bash
port=$1
path=$2
dbName=$3

# --protected-mode no
redis-server --port $port --appendonly yes --dir $path --dbfilename $dbName  > /dev/null 2>&1 &

max_attempts=10
interval=0.1  
attempt=1

while [ $attempt -le $max_attempts ]; do
  if redis-cli -p $port ping > /dev/null 2>&1; then
    echo "Redis instance is up and running."
    exit 0
  fi

  sleep $interval
  ((attempt++))
done

echo "Failed to establish connection to Redis instance within the given timeout."
exit 1
