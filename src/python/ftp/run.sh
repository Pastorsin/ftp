#!/bin/bash

PYTHON=$1

PORT="50051"
FILE="test.pdf"

PWD=$(pwd)
ROOT=$(dirname $0)
SERVER_DB="db/server"
CLIENT_DB="db/client"

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

$PYTHON server.py --port $PORT &
SERVER_PID=$!
echo "INFO - Starting the server [PID=$SERVER_PID]"

until [ $(netstat -tulpn | grep $PORT | wc -l) -ge 1 ]; do
    echo "INFO - Waiting to the server"
    sleep 1
done

echo "INFO - Read '$FILE' from server"
$PYTHON client.py --read $FILE --port $PORT
test $FILE

echo "INFO - Write '$FILE' to server"
$PYTHON client.py --write $FILE --port $PORT
test $FILE

kill -9 $SERVER_PID
cd $PWD