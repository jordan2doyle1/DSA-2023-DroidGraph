#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run DroidDynaSearch.
#   - Output Directory : Directory in which to save the output files. 
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

# shellcheck disable=SC2317
function exit_cleanup {
    target_file="target"
    [[ -d "$target_file" ]] && mv "$target_file" "$output_directory"
}

if [[ $# -ne 2 ]]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 2
fi

if [[ ! -f "$1" ]] || [[ "$1" != *.apk ]]; then
    echo "[ERROR] ($script) - Input file ($1) does not exist."
    exit 3
fi
apk_file="$1"
app_name=$(basename "$apk_file" .apk)

if [[ ! -d "$2" ]]; then
    echo "[ERROR] ($script) - Output directory ($2) does not exist."
    exit 4
fi
output_directory="$2/Droid_Traversal"

if [[ -f "$output_directory/traversal.log" ]]; then
    echo "[INFO] ($script) - Dynamic traversal for $app_name already exist. Skipping."
    exit 0
fi

jar_file="DroidDynaSearch-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [[ ! -f "$jar_file" ]]; then
    echo "[ERROR] ($script) - Dynamic traversal jar file does not exist."
    exit 5
fi

[[ -d "$output_directory" ]] && rm -r "$output_directory"
mkdir -p "$output_directory"

trap exit_cleanup EXIT

echo "[INFO] ($script) - Dynamic traversal running on $app_name..."
timeout 1h java -Xms4g -jar "$jar_file" -a "$apk_file" -o "$output_directory" -m 500 &> "$output_directory/cmd.log"
traversal_exit_code=$?

if [[ $traversal_exit_code -eq 124 ]]; then
    echo "[ERROR] ($script) - Dynamic traversal on $app_name is taking too long (1h). Timed out."
    exit $((traversal_exit_code))
fi

if [[ $traversal_exit_code -eq 0 ]]; then
    echo "[INFO] ($script) - Dynamic traversal on $app_name finished."
    exit $((traversal_exit_code))
else
    echo "[ERROR] ($script) - Dynamic traversal on $app_name failed (Err:$traversal_exit_code)."
    exit $((traversal_exit_code))
fi
