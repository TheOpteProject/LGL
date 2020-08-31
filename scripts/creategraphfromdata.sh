#!/bin/bash


## Creates a graph from a date (i.e. YYYY MM)
## $1 is the year
## $2 is the month
## and takes the first day of the month

fetch_date="$1.$2"

get_bview_url(){

    folderurl="http://data.ris.ripe.net/rrc${1}/${2}/"

    if curl --head --silent --fail $fullurl 2> /dev/null;
    then
        filename=$(curl --silent $1 | grep "bview.[0-9]" | sed -E 's/.*>(bview\.[0-9]*\.[0-9]*\.gz).*/\1/' | tail -n 1)

        fullurl="${folderurl}/${filename}"

        #"http://data.ris.ripe.net/rrc${rrc}/${y}/" "rrc${rrc}" "${y}"

        mkdir -p "bview_${2}"

        #echo $fullurl
        if curl --head --silent --fail $fullurl 2> /dev/null;
        then
            wget -b $fullurl -O "bview_${2}/bview_$3_$2_$filename"
        else
            echo "$fullurl does not exist."
        fi
    else
	    echo "$folderurl does not exist."
    fi
}

get_rrcs(){
    for i in {00..24}
    do
	echo "$i"
    done
}

for rrc in $(get_rrcs)
do
    ## Forking doesn't really work, so take it slow
    get_bview_url $rcc $fetch_date
done

### Just go through all arguments
echo -e "\n -- Using '$url' as source for graph -- \n" 

filename=$(basename -- "$1")
extension="${filename##*.}"
filename="${filename%.*}"

echo -e "\n -- File name as '$filename' and extension '$extension'  -- \n"

## set up the folder
echo -e "\n -- Setting up folders -- \n"
./create_run.sh $filename
cd ../testrun/$filename
echo -e "\n -- In $(pwd), folders setup complete -- \n "

## download the data
echo -e "\n -- Starting download... -- \n"
wget $url
echo -e "\n -- Bootstrapping the bgpdump -- \n "
./bootstrap.sh ${filename}.${extension}

## go back to scripts
cd ../../scripts

