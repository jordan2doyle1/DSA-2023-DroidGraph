#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run DroidGraph2.0.
#   - Output Directory : Directory in which to save the output files. 
#   - Virtual Environment : Directory containing python virtual environment. 
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

if [ $# -ne 3 ]; then
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
output_directory="$2/AndroGuard"

if [ ! -d "$3" ]; then
    echo "[ERROR] ($script) - Virtual environment ($3) does not exist."
    exit 5
fi
virtual_environment="$3"

androguard_graph_file="$output_directory/$app_name.gml"
if [[ -f "$androguard_graph_file" ]]; then
    echo "[INFO] ($script) - AndroGuard graph for $app_name already exists. Skipping."
    exit 0
fi

[[ -d "$output_directory" ]] && rm -r "$output_directory"
mkdir -p "$output_directory"

# shellcheck source=/dev/null
source "$virtual_environment/bin/activate"

echo "[INFO] ($script) - AndroGuard analysing $app_name..."
androguard cg -o "$output_directory/$app_name.gml" "$apk_file" &> "$output_directory/cmd.log"
androguard_exit_code=$?

if [[ $androguard_exit_code ]]; then
    echo "[INFO] ($script) - AndroGuard analysis of $app_name finished."
    exit $((androguard_exit_code))
else
    echo "[ERROR] ($script) - AndroGuard analysis of $app_name failed (Err:$androguard_exit_code)."
    exit $((androguard_exit_code))
fi
