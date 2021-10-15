#!/bin/bash

MVN=$1

PORT="8080"
FILE="test.pdf"

PWD=$(pwd)
ROOT=$(dirname $0)
SERVER_DB="db/server"
CLIENT_DB="db/client"

PACKAGE=pdytr.example.grpc
SERVER_CLASS=$PACKAGE.App
CLIENT_CLASS=$PACKAGE.Client

test() {
    file=$1
    DIFF=$(diff $SERVER_DB/$file $CLIENT_DB/$file)
    
    if [[ $DIFF == "" ]]
    then
        echo "OK - The files match"
    else
        echo $DIFF
        echo "ERROR - The files differ"
    fi
}

cd $ROOT

$MVN -DskipTests package exec:java -Dexec.mainClass=$SERVER_CLASS &
SERVER_PID=$!
echo "INFO - Starting the server [PID=$SERVER_PID]"

until [ $(netstat -tulpn | grep $PORT | wc -l) -ge 1 ]; do
    echo "INFO - Waiting to the server"
    sleep 1
done

$MVN -DskipTests exec:java -Dexec.mainClass=$CLIENT_CLASS -Dexec.args="-read $FILE"
test $FILE

$MVN -DskipTests exec:java -Dexec.mainClass=$CLIENT_CLASS -Dexec.args="-write $FILE"
test $FILE

kill -9 $SERVER_PID
cd $PWD