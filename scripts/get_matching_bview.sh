#!/bin/bash


## $1 == url

get_bview_url(){
    filename=$(curl --silent $1 | grep "bview.[0-9]" | sed -E 's/.*>(bview\.[0-9]*\.[0-9]*\.gz).*/\1/' | tail -n 1)

    fullurl="$1/$filename"

    #echo $fullurl
    if curl --head --silent --fail $fullurl 2> /dev/null;
    then
	wget -b $fullurl -O "bview_$3_$2_$filename"
    else
	echo "$fullurl does not exist."
    fi

}


get_years(){
    for i in {2000..2020}
    do
	echo "$i.09"
    done
}

get_rrcs(){
    for i in {00..24}
    do
	echo "$i"
    done
}

for y in $(get_years)
do
    for rrc in $(get_rrcs)
    do
	## Lets fork it for speed
	get_bview_url "http://data.ris.ripe.net/rrc${rrc}/${y}/" "rrc${rrc}" "${y}"
    done
done

#parallel --jobs 100 $(get_bview_url "http://data.ris.ripe.net/rrc{1}/{2}/") ::: $(get_rrcs) ::: $(get_years)
