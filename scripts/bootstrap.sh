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

echo "Moving images, png to '${rundir}' and '../../resource/images/' for easy commit."
# take care of the images
cp tmp/*/*.png ${filename}.png
cp ${filename}.png ../../resources/images/.

echo "Moving graph-components (lgl and coords)"
cp tmp/*/0.lgl ${filename}.lgl
cp tmp/*/0.coords ${filename}.coords
