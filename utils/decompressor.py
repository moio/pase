#!/usr/bin/python3
'''
Recursively decompresses archives in a directory
'''

import argparse
import multiprocessing
import os
import re
import shutil

DECOMPRESSORS = {
    ("rpm",): "rpm2cpio {} | cpio --extract --make-directories --preserve-modification-time --quiet --directory={}",
    # https://en.wikipedia.org/wiki/Tar_(computing)#Suffixes_for_compressed_files
    (".tar.bz2", ".tb2", ".tbz", ".tbz2", ".tz2", ".tar.gz", ".taz", ".tgz", ".tar.lz", ".tar.lzma", ".tlz", ".tar.lzo", ".tar.xz", ".tar.Z", ".tZ", ".taZ", ".tar.zst", ".tzst"): "tar --extract --file={} --directory={}",
    ("zip",): "unzip -o -P dummy -qq {} -d {}",
}

def main():
    parser = argparse.ArgumentParser(description='Recursively decompress archives in a directory.')
    parser.add_argument("-d", "--directory", default=".", help="the directory to process")
    parser.add_argument("-r", "--recursion-limit", type=int, default=2, help="how many subdirectories to visit")
    args = parser.parse_args()

    pool = multiprocessing.Pool(8)

    directory = args.directory
    directories = [directory]
    for _ in range(args.recursion_limit):
        directories = visit(directories, pool)

def visit(directories, pool):
    files = sorted([file for directory in directories for file in files_in(directory)])
    return [new_directory for new_directory in pool.map(decompress, files) if new_directory is not None]

def files_in(directory):
    return [os.path.join(root, file) for root, dirs, files in os.walk(directory) for file in files]

def decompress(file):
    for extensions, decompressor in DECOMPRESSORS.items():
        if (any(file.lower().endswith(extension) for extension in extensions)):
            print(file)
            destination = file + "_contents"
            shutil.rmtree(destination, ignore_errors=True)
            os.makedirs(destination, exist_ok=True)
            os.system(decompressor.format(file, destination))
            return destination

if __name__ == "__main__":
    main()
