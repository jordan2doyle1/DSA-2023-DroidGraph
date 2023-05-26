#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters: 
#   - Script File Name : The name of the script file to execute in Docker.
#   - Input APK File : The apk file on which to run tests. 
#   - Output Directory : Directory in which to save the output files.
#
# Optional Parameters:
#   - Docker Container Name (Default=android-container)
#   - Docker Mount Directory (Default=~/DockerTests)
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

# shellcheck disable=SC2317
function exit_cleanup {
    printf "[INFO] (%s) - Stopping Docker container... " "$script"
    docker stop "$container_name"
    printf "[INFO] (%s) - Removing Docker container... " "$script"
    docker rm "$container_name"
}

container_name="android-container"
if [[ "$1" == "--container-name="* ]]; then
    container_name=$(echo "$1" | cut -d "=" -f 2)
    echo "[INFO] ($script) - Container name set as $container_name."
    shift
fi

local_mount_directory="$HOME/DockerTests"
if [[ "$1" == "--mount-dir="* ]]; then
    local_mount_directory=$(echo "$1" | cut -d "=" -f 2)
    echo "[INFO] ($script) - Mount directory set as $local_mount_directory."
    shift
fi

if [ $# -lt 1 ]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 2
fi

if [ ! -d "$local_mount_directory" ]; then
    echo "[ERROR] ($script) - Mount directory ($local_mount_directory) does not exist."
    exit 3
fi

script_name="$1"
test_script=$local_mount_directory/$script_name
if [ ! -f "$test_script" ]; then
    echo "[ERROR] ($script) - Test script ($test_script) does not exist."
    exit 4
fi

if [ ! "$(command -v docker)" ]; then 
    echo "[ERROR] ($script) - Docker not installed."
    exit 5
fi

image_name="jordan/docker-android-x86-11.0"
if ! docker image ls | grep -q "$image_name"; then
    echo "[INFO] ($script) - Building docker image $image_name."
    docker build -t $image_name "$PWD"
fi

trap exit_cleanup EXIT

docker_mount_directory="/root/$(basename "$local_mount_directory")"
if ! docker container ls -all | grep -q "$container_name"; then
    printf "[INFO] (%s) - Running docker container... " "$script"
    docker run --privileged -d -p 6080:6080 -v "$local_mount_directory":"$docker_mount_directory" -e DEVICE="Samsung Galaxy S10" --name "$container_name" $image_name
fi

if ! docker container ls | grep -q "$container_name"; then
    printf "[INFO] (%s) - Starting docker container... " "$script"
    docker start "$container_name"
fi

if ! docker exec "$container_name" test -d "$docker_mount_directory"; then
    echo "[ERROR] ($script) Mount directory ($docker_mount_directory) not found in Docker container."
    exit 6
fi

test_script=$docker_mount_directory/$script_name
if ! docker exec "$container_name" test -f "$test_script"; then
    echo "[ERROR] ($script) - Test script ($test_script) not found in Docker container."
    exit 7
fi

echo "[INFO] ($script) - Running $script_name in container $container_name... "
if [[ "$test_script" == *".py"* ]]; then
    docker exec --workdir "$docker_mount_directory" "$container_name" "/root/python_venv/bin/python" "$test_script" "$2" "$3"
    exit $?
else
    docker exec --workdir "$docker_mount_directory" "$container_name" "$test_script" "$2" "$3"
    exit $?
fi
