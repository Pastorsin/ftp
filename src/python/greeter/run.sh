#!/bin/bash
PYTHON=$1

PORT="50051"

PWD=$(pwd)
ROOT=$(dirname $0)

cd $ROOT

$PYTHON server.py -p $PORT &
SERVER_PID=$!
echo "INFO - Starting the server [PID=$SERVER_PID]"

until [ $(netstat -tulpn | grep $PORT | wc -l) -ge 1 ]; do
    echo "INFO - Waiting to the server"
    sleep 1
done

echo "INFO - Starting client"
$PYTHON client.py -p $PORT

kill -9 $SERVER_PID
cd $PWD