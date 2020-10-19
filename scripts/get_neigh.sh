#!/bin/bash

# assuming ASpath input, i.e. "10 21 34 213" and we want the pairs
# i.e.
# 10 21
# 21 34
# 34 213

while read a; do
	set -- $a
	if [ $# -gt 1 ]; then
		while (($#)); do
			#echo "args: $@"
			if [ $1 != $2 ]; then
				if [ $1 -lt $2 ]; then
					printf "$1 $2\n" ;
				else
					printf "$2 $1\n" ;
				fi
			fi
			
			if [ -z "$3" ]; then
				#echo "extra shift!"
				shift 2
			fi
			shift
		done
	fi
done