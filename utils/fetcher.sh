#!/bin/bash

rsync -rlptH rsync://fr2.rpmfind.net/linux/opensuse/source/distribution/leap/15.2/repo/oss/src/ . --delete-after -hi --stats
