#!/bin/bash


## Uses a bview file (such as http://data.ris.ripe.net/rrc00/latest-bview.gz),
## i.e. not updates but the entire view, and generates ncol files which can
## be used as graphs from that.

get_neighbors() {
    # assuming ASpath input, i.e. "10 21 34 213" and we want the pairs
    # i.e.
    # 10 21
    # 21 34
    # 34 213
    while read a; do
	wa=($a)
	len=${#wa[@]}
	# go through and print the neighbors (assuming ASpath input)
	for (( i=1; i<$len; i++ )); do
	    ## order it, is very useful for debugging
	    ## also we must check that we don't have duplicates
	    if [ "${wa[$i-1]}" != "${wa[$i]}" ]; then
		if [ "${wa[$i-1]}" -lt "${wa[$i]}" ]; then
		    echo "${wa[$i-1]} ${wa[$i]}" ;
		else
		    echo "${wa[$i]} ${wa[$i-1]}" ;
		fi
		
	    fi
	done
    done
}

get_pairs() {
    # assuming prefix + AS which all should match prefix
    # i.e 1.1.1.1/24 12 13 14 15
    # ->
    # 1.1.1.1/24 12
    # 1.1.1.1/24 13
    # 1.1.1.1/24 14
    # 1.1.1.1/24 15
    while read a; do
	# go through and pair all with first
	wa=($a)
	len=${#wa[@]}
	for (( i=1; i<$len; i++ )); do
	    ## start outputting!
	    ## but only if they are different
	    if [ "${wa[0]}" !=  "${wa[$i]}" ]; then
		echo "${wa[0]} ${wa[$i]}" ;
	    fi
	done
    done
}

## First argument is bview-file. The rest is automatic
filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"
asas=${filename}.as_as.ncol
preas=${filename}.prefix_as.ncol
export ncol=${filename}.full.ncol

echo "Parsing $1 for ASpath-info (i.e. AS -> AS routing)"
time (zcat $1 | bgpdump -m - | cut -d '|' -f 7 |\
	  sed -e 's/[{},]/ /g' | get_neighbors | sort | uniq -c |\
	  awk '{$1=$1};1' | awk '{print $2 " " $3 " " $1}' | sort -n > $asas)

echo "Parsing $1 for prefix-info (i.e. AS -> prefix routing)"
time (zcat $1 | bgpdump -m - | cut -d '|' -f 6-7 |\
	  sed "s/|/ /" | awk -F' ' '{print $1 " " $NF}' | awk '{$1=$1};1' |\
	  sed -e 's/[{},]/ /g' | get_pairs | sort | uniq -c |\
	  awk '{print $2 " " $3 " " $1}' | sort -n > $preas)
      
echo "Combining $asas and $preas into $ncol"
time (cat $asas $preas > $ncol)

