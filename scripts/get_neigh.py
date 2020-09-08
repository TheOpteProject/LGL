#!/usr/bin/env python

import sys
import shlex

# python version of get_neigh

for line in sys.stdin:
    line = shlex.split(line)
    #line = line.split(' ')
    res = []
    for i in range(len(line) - 1):
        if line[i] == line[i+1]:
            continue
        elif line[i] < line[i+1]:
            res.append("{0} {1}".format(line[i+0], line[i+1]))
        else:
            res.append("{0} {1}".format(line[i+1], line[i+0]))
    
    if len(res) > 0:
        print("\n".join(res))
