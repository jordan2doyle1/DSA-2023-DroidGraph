#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run DroidGraph2.0.
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
output_directory="$2"

jar_file="DroidGraph2.0-2.0-SNAPSHOT-jar-with-dependencies.jar"
graph_file="$output_directory/AndroGuard/$app_name.gml"
if [[ ! -f "$jar_file" ]] || [[ ! -f $graph_file ]]; then
    echo "[ERROR] ($script) - Droid graph jar file or AndroGuard call graph file does not exist."
    exit 5
fi

output_directory="$2/Droid_Graph"
control_flow_graph_file="$output_directory/app_control_flow_graph.json"
if [[ -f "$control_flow_graph_file" ]]; then
    echo "[INFO] ($script) - Control flow graph for $app_name already exists. Skipping."
    exit 0
fi

[[ -d "$output_directory" ]] && rm -r "$output_directory"
mkdir -p "$output_directory"

trap exit_cleanup EXIT

echo "[INFO] ($script) Generating Droid graph for $app_name..."
timeout 1h java -Xms4g -jar "$jar_file" -s -m -cf -a "$apk_file" -i "$graph_file" -o "$output_directory" &> "$output_directory/cmd.log"
graph_exit_code=$?

if [[ $graph_exit_code -eq 124 ]]; then
    echo "[ERROR] ($script) - Droid graph generation on $app_name is taking too long (1h). Timed out."
    exit $((graph_exit_code))
fi

if [[ $graph_exit_code -eq 0 ]]; then
    echo "[INFO] ($script) - Droid graph generation for $app_name finished."
    exit $((graph_exit_code))
else
    echo "[ERROR] ($script) - Droid graph generation for $app_name failed (Err:$graph_exit_code)."
    exit $((graph_exit_code))
fi
