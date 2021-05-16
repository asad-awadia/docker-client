#!/usr/bin/env sh

docker build -t test:load-image .
docker save test:load-image -o load-from-file.tar
