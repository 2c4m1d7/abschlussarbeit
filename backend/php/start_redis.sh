#!/bin/bash
port=$1
path=$2

timeout 30s redis-server --port $port --appendonly yes --dir $path --protected-mode no > /dev/null 2>&1 &

sleep 1
redis-cli -p $port ping

exit $?


