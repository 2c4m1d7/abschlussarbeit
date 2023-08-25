#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <port> <path>"
    exit 1
fi


port=$1
path=$2



redis-server $path --port $port > /dev/null 2>&1 &

chmod 600 $path

max_attempts=10
interval=0.1  
attempt=1

while [ $attempt -le $max_attempts ]; do
 if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null; then
    echo "Port $port is already in use."
    exit 0
fi

  sleep $interval
  ((attempt++))
done

echo "Failed to establish connection to Redis instance within the given timeout."
exit 1



# while [ $attempt -le $max_attempts ]; do
#   if redis-cli -p $port ping > /dev/null 2>&1; then
#     echo "Redis instance is up and running."
#     exit 0
#   fi

#   sleep $interval
#   ((attempt++))
# done

# echo "Failed to establish connection to Redis instance within the given timeout."
# exit 1
