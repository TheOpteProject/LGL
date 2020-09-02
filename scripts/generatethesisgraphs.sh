#!/bin/bash

## This script is specific to my thesis

## Load exports
export lgldir="$(dirname $(pwd))"
export lglimage="java -jar ${lgldir}/Java/jar/ImageMaker.jar"
export runfolder="../testrun"


 threads=5 # hardcoded for my machine

 . $(which env_parallel.bash)

# Do for september (9) for the last twenty years (2000..2020)
env_parallel -P ${threads} -n 1 creategraphfromdate ::: {2000..2020} ::: 9

## Also generate nice images to scale (hardcoded)

./createthesisimages.sh