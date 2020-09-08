#!/usr/bin/env python

import sys
import shlex

# python version of get_pairs

for line in sys.stdin:
    line = shlex.split(line)
    #line = line.split(' ')
    prefix = line[0]
    res = []
    for asn in line[1:]:
        if prefix == asn:
            continue
        else:
            res.append("{0} {1}".format(prefix, asn))

    if len(res) > 0:
        print("\n".join(res))

