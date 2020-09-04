#!/bin/bash

# assuming prefix + AS which all should match prefix
# i.e 1.1.1.1/24 12 13 14 15
# ->
# 1.1.1.1/24 12
# 1.1.1.1/24 13
# 1.1.1.1/24 14
# 1.1.1.1/24 15
while read a; do
    set -- $a
    # go through and pair all with first

    prefix=$1
    shift
    for pos; do
        printf "$prefix $pos\n"
    done
done
