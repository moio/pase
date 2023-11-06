# PaSe

Pa(tch)Se(arch) is an experimental search engine for code allowing search by patch.

It will return files by applicability of a specified patch.

## Indexing

To index a source directory:
```
java -jar pase.jar index <source_path> <index_path>
```

## Searching

Once indexing has finished, you can search:

- **on the command line**: use `java -jar pase.jar search <index_path> <patch_path>`
- **via API**:
  - use `java -jar pase.jar serve <index_path>` to start the PaSe Server
  - query the URL `http://localhost:4567/search?patch=URL_ENCODED_PATCH` to get results as JSON (see `utils/example_client.py` for a full example)
- **via the Web UI**:
  - use `java -jar pase.jar serve <index_path>` to start the PaSe Server
  - visit [http://localhost:4567](http://localhost:4567) with your browser

![Screen capture of PaSe's Web UI](doc/pase-webui.gif)

## Building and running as Docker containers

To ease experimentation with PaSe, use the bundled `Dockerfile` and `compose.yaml`
to build and run a pair of Docker containers (one for the source indexer, one for the web server).

Edit the paths to the directory containing the sources (`$SRCDIR`) and to where the index will be
written (`$INDEXDIR`) on the host in file `.env`, then run `docker compose up`.

This will build and run a pair of containers named `pase-indexer-1` and `pase-server-1`.
The indexer will scan and index the directory containing the sources, then exit.
The server will listen indefinitely for requests targeting the index at http://localhost:4567/.
