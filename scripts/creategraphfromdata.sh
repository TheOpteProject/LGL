#!/bin/bash


## Creates a graph from a date (i.e. YYYY MM)
## $1 is the year
## $2 is the month
## and takes the first day of the month

fetch_date="$1.$2"
folder="bview_${fetch_date}"

get_bview_url(){

    folderurl="http://data.ris.ripe.net/rrc${1}/${2}/"

    #echo "Trying $folderurl ..."

    if curl --head --silent --fail $folderurl > /dev/null;
    then
        filename=$(curl --silent $folderurl | grep "bview.[0-9]" | sed -E 's/.*>(bview\.[0-9]*\.[0-9]*\.gz).*/\1/' | tail -n 1)

        fullurl="${folderurl}/${filename}"

        #"http://data.ris.ripe.net/rrc${rrc}/${y}/" "rrc${rrc}" "${y}"

        mkdir -p "${folder}"

        #echo $fullurl
        if curl --head --silent --fail $fullurl > /dev/null;
        then
            ## potentially add -b for fork, except collection is too hard in bash
            echo "Downloading $fullurl to $folder"
            wget $fullurl -O "${folder}/bview_$1_$2_$filename" 2> /dev/null
        else
           #echo "$fullurl does not exist."
           :
        fi
    else
	    #echo "$folderurl does not exist."
        :
    fi
}

get_rrcs(){
    for i in {00..24}
    do
	echo "$i"
    done
}


### Just go through all arguments
echo -e "\n -- Using '$folder' as source for graph -- \n" 

filename=${folder/\./_}
extension="${filename##*.}"
filename="${filename%.*}"

echo -e "\n -- Using files in '$folder'  -- \n"

## set up the folder
echo -e "\n -- Setting up folders -- \n"
./create_run.sh $filename
cd ../testrun/$filename
echo -e "\n -- In $(pwd), folders setup complete -- \n "


echo -e "\n -- Downloading dumps (${fetch_date}) -- \n "
for rrc in $(get_rrcs)
do
    ## Forking doesn't really work, so take it slow
    get_bview_url "$rrc" "$fetch_date"
done

echo -e "\n -- Bootstrapping the bgpdumps in ${folder} -- \n "
./bootstrap.sh ${folder}/bview*.gz

## go back to scripts
cd ../../scripts

