#!/bin/bash


## Uses a bview file (such as http://data.ris.ripe.net/rrc00/latest-bview.gz),
## i.e. not updates but the entire view, and generates ncol files which can
## be used as graphs from that.

# get_neighbors() {
#     # assuming ASpath input, i.e. "10 21 34 213" and we want the pairs
#     # i.e.
#     # 10 21
#     # 21 34
#     # 34 213
#     while read a; do
# 		wa=($a)
# 		len=${#wa[@]}
# 		# go through and print the neighbors (assuming ASpath input)
# 		for (( i=1; i<$len; i++ )); do
# 			## order it, is very useful for debugging
# 			## also we must check that we don't have duplicates
# 			if [ "${wa[$i-1]}" != "${wa[$i]}" ]; then
# 				if [ "${wa[$i-1]}" -lt "${wa[$i]}" ]; then
# 					echo "${wa[$i-1]} ${wa[$i]}" ;
# 				else
# 					echo "${wa[$i]} ${wa[$i-1]}" ;
# 				fi
# 			fi
# 		done
#     done
# }

# get_pairs() {
#     # assuming prefix + AS which all should match prefix
#     # i.e 1.1.1.1/24 12 13 14 15
#     # ->
#     # 1.1.1.1/24 12
#     # 1.1.1.1/24 13
#     # 1.1.1.1/24 14
#     # 1.1.1.1/24 15
#     while read a; do
# 	# go through and pair all with first
# 	wa=($a)
# 	len=${#wa[@]}
# 	for (( i=1; i<$len; i++ )); do
# 	    ## start outputting!
# 	    ## but only if they are different
# 	    if [ "${wa[0]}" !=  "${wa[$i]}" ]; then
# 		echo "${wa[0]} ${wa[$i]}" ;
# 	    fi
# 	done
#     done
# }

## Get the current folder and use it for naming
folder=$(basename $(pwd))
asas=${folder}.as_as.ncol
preas=${folder}.prefix_as.ncol
export ncol=${folder}.full.ncol

## Quicker sort
export LC_ALL=C

## Rough outline aspath info
## zcat -- reads multiple files (bgpdump only takes one)
## bgpdump -- format into table format
## cut -d '|' -f 7 -- takes the relevant info out (i.e. ASpath)
## sed -e ... -- get rid of commas and stuff
## get_neighbors -- see above, get all pairs of neighbors in order
## sort -- sorts pairs
## uniq -c -- counts unique occcurences
## awk and awk -- reformat into the format we want
## sort -n -- a last numerical sort after reformatting

echo "Parsing $@ for ASpath-info (i.e. AS -> AS routing)"
time (zcat $@ | bgpdump -m - | cut -d '|' -f 7 |\
	  sed -e 's/[{},]/ /g' | get_neighbors | sort -S1G --parallel=24 | uniq -c |\
	  awk '{$1=$1};1' | awk '{print $2 " " $3 " " $1}' | sort -n -S1G --parallel=24 > $asas)

## rough ooutline mapping prefixes
## zcat -- reads multiple files (bgpdump only takes one)
## bgpdump -- format into table format
## cut -d '|' -f 6-7 -- takes the relevant info out (i.e. ASpath and prefix)
## sed -e ... -- get rid of commas and stuff
## awk and awk -- formatting
## get_pairs -- see above, get all pairs of neighbors in order
## sort -- sorts pairs
## uniq -c -- counts unique occcurences
## awk and awk -- reformat into the format we want
## sort -n -- a last numerical sort after reformatting

echo "Parsing $@ for prefix-info (i.e. AS -> prefix routing)"
time (zcat $@ | bgpdump -m - | cut -d '|' -f 6-7 |\
	  sed "s/|/ /" | awk -F' ' '{print $1 " " $NF}' | awk '{$1=$1};1' |\
	  sed -e 's/[{},]/ /g' | get_pairs | sort -S1G --parallel=24 | uniq -c |\
	  awk '{print $2 " " $3 " " $1}' | sort -n -S1G --parallel=24 > $preas)
      
echo "Combining $asas and $preas into $ncol"
time (cat $asas $preas > $ncol)

