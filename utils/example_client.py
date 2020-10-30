#!/usr/bin/python3
'''
Runs a PaSe search
'''

import json
from urllib.parse import quote
from urllib.request import urlopen
from sys import argv

def pase_search(patch):
    response = urlopen('http://localhost:4567/search', patch).read()
    return json.loads(response)

patch = open(argv[1])
results = pase_search(patch)

print("%d results found" % len(results))
for file, file_results in results.items():
    print(file)
    for i, chunk_results in enumerate(file_results):
        print("  - chunk #%d" % (i + 1))
        if chunk_results:
            for chunk_result in chunk_results:
                print("      %s (score: %d)" % (chunk_result["path"], chunk_result["score"]))
        else:
            print("      no results")
