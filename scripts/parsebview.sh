#!/bin/bash


## Uses a bview file (such as http://data.ris.ripe.net/rrc00/latest-bview.gz), i.e. not updates but the entire view, and generates graphs from that.

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
}

## First argument is bview-file. The rest is automatic
filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"

echo "Parsing $1 for ASpath-info (i.e. AS -> AS routing)"
zcat $1 | bgpdump -m - | head -n 500 | cut -d '|' -f 7 |\
    sed "s/[{},]/ /" | get_neighbors | sort | uniq -c |\
    awk '{$1=$1};1' | awk '{print $2 " " $3 " " $1}' > ${filename}.as_as.ncol

echo "Parsing $1 for prefix-info (i.e. AS -> prefix routing)"
zcat $1 | bgpdump -m - | head -n 500 | cut -d '|' -f 6-7 |\
    sed "s/|]/ /" | awk -F' ' '{print $1 " " $NF}' | awk '{$1=$1};1' |\
    get_neighbors | sort | uniq -c |\
    awk '{print $2 " " $3 " " $1}' > ${filename}.prefix_as.ncol

