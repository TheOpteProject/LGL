#!/bin/bash


## Creates a graph from an url which returns a bgpdump compatible file

### Just go through all arguments
for url in "$@"
do
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
done;
