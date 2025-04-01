#!/bin/sh
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 username/PSSC"
    exit 1
fi

docker login
docker push "$1:latest"
