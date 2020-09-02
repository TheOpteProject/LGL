#!/bin/bash

## Get the current folder and use it for naming
folder=$(basename $(pwd))

# for run.sh
export ncol=${folder}.full.ncol

echo "Running bootstrap for $@ (using ${folder} as base)"

./parsebview.sh $@

echo "Converted and got ncol-file $ncol"


if [ -z "$lgltest" ]
then
      echo "$lgltest is empty"
else
      echo "$lgltest is set, quitting"
      exit
fi

# run.sh uses $ncol variable
## Skip for now, testing
./run.sh -C

echo "Sleeping for a couple of second to wait in image generation"
sleep 5

echo "Moving images, png to '${rundir}' and '../../resource/images/' for easy commit."
# take care of the images
cp tmp/*/0.coords*transparent.png ${folder}_transparent.png
cp tmp/*/0.coords*light.png ${folder}_light.png
cp tmp/*/0.coords*dark.png ${folder}_dark.png
cp ${folder}_transparent.png ../../resources/images/.

echo "Moving graph-components (lgl and coords)"
cp tmp/*/0.lgl ${folder}.lgl
cp tmp/*/0.coords ${folder}.coords

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    xdg-open .
elif [[ "$OSTYPE" == "darwin"* ]]; then
    open .
    # Mac OSX
fi