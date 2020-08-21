# PaSe

Pa(tch)Se(arch) is an experimental search engine for code allowing search by patch.

It will return files by applicability of a specified patch.

## Usage
To index a source directory:
```
java -jar pase.jar index <source_path> <index_path>
```

To look for a file by patch in an index:
```
java -jar pase.jar search <index_path> <patch_path>
```
