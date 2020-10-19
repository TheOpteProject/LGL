#!/bin/bash

## Gets the percentage of edges which are prefixes (i.e. connecting to edge in some sense)
prefixline="%60s %10s %10s %10s\n" 
printf "$prefixline" File Pct Edges Nodes

for file; do
    ## All lines with "/" contain a prefix, i.e. 192.168.0.1/24 and so on
    prefixlines=$(cat $file | grep "/" | wc -l)
    alllines=$(cat $file | wc -l)
    nodes=$(cat $file | cut -f1-2 -d ' ' | tr -s ' ' '\n' | sort -n | uniq | wc -l)
    #echo $prefixlines/$alllines
    if [ "$alllines" != "0" ]; then
        pct=$(echo "scale=2; $prefixlines / $alllines" | bc)
        printf "$prefixline" $file $pct $alllines $nodes
    fi
done
