#!/bin/bash


## Uses a bview file (such as http://data.ris.ripe.net/rrc00/latest-bview.gz),
## i.e. not updates but the entire view, and generates ncol files which can
## be used as graphs from that.

zcat=zcat # start with normal zcat on path

# if we have gzcat (gnu-version), lets use it
if hash gzcat 2>/dev/null; then
	zcat=gzcat
fi

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

echo "in $(pwd)"
echo "zcat $@"

echo "Parsing $@ for ASpath-info (i.e. AS -> AS routing)"

time ($zcat $@ | bgpdump -m - | cut -d '|' -f 7 |\
	  sed -e 's/[{},]/ /g' | ./get_neigh.py | sort -S1G --parallel=24 | uniq -c |\
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
time ($zcat $@ | bgpdump -m - | cut -d '|' -f 6-7 |\
	  sed "s/|/ /" | awk -F' ' '{print $1 " " $NF}' | awk '{$1=$1};1' |\
	  sed -e 's/[{},]/ /g' | ./get_pairs.py | sort -S1G --parallel=24 | uniq -c |\
	  awk '{print $2 " " $3 " " $1}' | sort -n -S1G --parallel=24 > $preas)
      
echo "Combining $asas and $preas into $ncol"
time (cat $asas $preas > $ncol)

