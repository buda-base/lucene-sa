#!/usr/bin/env bash

# this script is run once for a fresh clone from the top-level of the git repo

# initialize submodules
git submodule init
git submodule update
pushd resources/sanskrit-stemming-data
git submodule init
git submodule update
popd

# build lexical resources for the main trie:
pushd resources/sanskrit-stemming-data/sandhify
python3 sandhifier.py

# build sandhi test tries
python3 generate_test_tries.py
popd

# update other test tries with lexical resources
pushd src/test/resources/tries
python3 update_tries.py
popd
