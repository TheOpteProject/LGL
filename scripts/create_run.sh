#!/bin/bash

## creates a testrun named $1

if [ -d "../testrun/${1}" ];
then
    ## bkp it
    echo "Directory ${1} exists, moving it." 
    if [ -d "../testrun/${1}_old"];
    then
        echo "Directory ${1}_old exists, removing backup (keeping one generation only)." 
        rm -rf "../testrun/${1}_old"
    fi
    mv "../testrun/${1}" "../testrun/${1}_old";
fi

mkdir -p ../testrun/$1

## copy and make executable
cp run.sh parsebview.sh bootstrap.sh ../testrun/$1/
chmod +x ../testrun/$1/*.sh

# should be done
echo "Now 'cd ../testrun/$1', copy in bgp-dump and run './bootstrap.sh routingfile.gz'"
