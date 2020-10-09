#!/usr/bin/python3
'''
Runs a PaSe search
'''

import json
from urllib.parse import quote
from urllib.request import urlopen
from sys import argv

def pase_search(patch):
    response = urlopen('http://localhost:4567/search?patch=' + quote(patch)).read()
    return json.loads(response)

patch = open(argv[1]).read()
results = pase_search(patch)

print("%d results found" % len(results))
for result in results:
    print(result["path"])
    print("  score %f" % result["score"])
