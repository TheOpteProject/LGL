#!/bin/bash

## creates a testrun named $1

mkdir -p ../testrun/$1

## copy and make executable
cp run.sh parsebview.sh bootstrap.sh ../testrun/$1/
chmod +x ../testrun/$1/*.sh

echo "Now 'cd ../testrun/$1', copy in bgp-dump and run './bootstrap.sh routingfile.gz'"

