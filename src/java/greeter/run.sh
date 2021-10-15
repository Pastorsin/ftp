#!/bin/bash
MVN=$1

PORT="8080"

PWD=$(pwd)
ROOT=$(dirname $0)

PACKAGE=pdytr.example.grpc
SERVER_CLASS=$PACKAGE.App
CLIENT_CLASS=$PACKAGE.Client

cd $ROOT

$MVN -DskipTests package exec:java -Dexec.mainClass=$SERVER_CLASS &
SERVER_PID=$!
echo "INFO - Starting the server [PID=$SERVER_PID]"

until [ $(netstat -tulpn | grep $PORT | wc -l) -ge 1 ]; do
    echo "INFO - Waiting to the server"
    sleep 1
done

echo "INFO - Starting client"
$MVN -DskipTests package exec:java -Dexec.mainClass=$CLIENT_CLASS

kill -9 $SERVER_PID
cd $PWD