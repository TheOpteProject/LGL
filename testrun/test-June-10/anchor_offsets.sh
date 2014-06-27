#!/usr/bin/env bash

anchor_coords_file=$1
result_coords_file=$2

if [ $# -ne 2 -o "$anchor_coords_file" == "" -o "$result_coords_file" == "" ]; then
	echo "Usage: $0 <anchors-coords-file> <result-coords-file>" >&2
	exit 2
fi

function calc
{
	echo -e "scale=5\n$1" | bc -q
}

while read l; do
	name=`echo $l | awk '{print $1}'`
	x0=`echo $l | awk '{print $2}'`
	y0=`echo $l | awk '{print $3}'`
	result_line="`grep \"^$name \" $result_coords_file`"
	x=`echo $result_line | awk '{print $2}'`
	y=`echo $result_line | awk '{print $3}'`
	dx=`calc $x-$x0`
	dy=`calc $y-$y0`
	echo $name $dx $dy `calc sqrt\(\($dx\)*\($dx\)+\($dy\)*\($dy\)\)`
done < $anchor_coords_file
