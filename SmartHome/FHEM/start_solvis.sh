#!/bin/sh
set -x
#sleep 60

java -jar SolvisSmartHomeServer.jar --server-path=/media/data/fhem &> /media/data/fhem/SolvisStdErr.log &
