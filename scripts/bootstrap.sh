#!/bin/bash



filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"
asas=${filename}.as_as.ncol
preas=${filename}.prefix_as.ncol
export ncol=${filename}.full.ncol

echo "Running bootstrap for $@ (using $filename as base)"

./parsebview.sh $@

echo "Converted and got ncol-file $ncol"
# run.sh uses $ncol variable
./run.sh -C

echo "Sleeping for a couple of second to wait in image generation"
sleep 5

echo "Moving images, png to '${rundir}' and '../../resource/images/' for easy commit."
# take care of the images
cp tmp/*/0.coords*.png ${filename}.png
cp ${filename}.png ../../resources/images/.

echo "Moving graph-components (lgl and coords)"
cp tmp/*/0.lgl ${filename}.lgl
cp tmp/*/0.coords ${filename}.coords
