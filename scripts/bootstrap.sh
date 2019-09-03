#!/bin/bash

echo "Running bootstrap for $1"

filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"
asas=${filename}.as_as.ncol
preas=${filename}.prefix_as.ncol
export ncol=${filename}.full.ncol

./parsebview.sh $1

echo "Converted and got ncol-file $ncol"
# run.sh uses $ncol variable
./run.sh -C
