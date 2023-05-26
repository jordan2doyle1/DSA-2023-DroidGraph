#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run DroidDynaSearch.
#   - Graph Import File: The JSON file containing the CFG import graph.
#   - Output Directory : Directory in which to save the output files. 
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

# shellcheck disable=SC2317
function exit_cleanup {
    target_file="target"
    [[ -d "$target_file" ]] && mv "$target_file" "$output_directory"
}

if [[ $# -ne 3 ]]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 2
fi

if [[ ! -f "$1" ]] || [[ "$1" != *.apk ]]; then
    echo "[ERROR] ($script) - Input file ($1) does not exist."
    exit 3
fi
apk_file="$1"
app_name=$(basename "$apk_file" .apk)

if [[ ! -f "$2" ]]; then
    echo "[ERROR] ($script) - Import file ($2) does not exist."
    exit 4
fi
import_file="$2"

if [[ ! -d "$3" ]]; then
    echo "[ERROR] ($script) - Output directory ($3) does not exist."
    exit 5
fi
output_directory="$3"

jar_file="DroidCoverage-1.0-SNAPSHOT-jar-with-dependencies.jar"
graph_file="$output_directory/AndroGuard/$app_name.gml"
if [[ ! -f "$jar_file" ]] || [[ ! -f $graph_file ]]; then
    echo "[ERROR] ($script) - Monkey coverage jar file or AndroGuard call graph file does not exist."
    exit 6
fi

callbacks="$output_directory/Droid_Graph/flow_droid_callbacks"
if [[ ! -f "$callbacks" ]]; then
    echo "[WARN] ($script) - Existing FlowDroid callbacks file not found."
fi

traversal="$output_directory/Droid_Traversal"
monkey_all="$output_directory/Monkey_All"
monkey_click="$output_directory/Monkey_Click"

output_directory="$3/DroidCoverage"

if [[ -f "$output_directory/cmd.log" ]]; then
    echo "[INFO] ($script) - Coverage for $app_name already calculated. Skipping."
    exit 0
fi

[[ -d "$output_directory" ]] && rm -r "$output_directory"
mkdir -p "$output_directory"

arguments=(-a "$apk_file" -i "$graph_file" -o "$output_directory")
[[ -f "$callbacks" ]] && arguments+=(-z) && arguments+=("$callbacks")
[[ -f "$import_file" ]] && arguments+=(-l) && arguments+=("$import_file")
[[ -d "$traversal" ]] && arguments+=(-t) && arguments+=("$traversal")
[[ -d "$monkey_click" ]] && arguments+=(-mc) && arguments+=("$monkey_click")
[[ -d "$monkey_all" ]] && arguments+=(-ma) && arguments+=("$monkey_all")

trap exit_cleanup EXIT

echo "[INFO] ($script) - Calculating coverage for $app_name..."
timeout 1h java -Xms4g -jar "$jar_file" "${arguments[@]}" &> "$output_directory/cmd.log"
coverage_exit_code=$?

if [[ $coverage_exit_code -eq 124 ]]; then
    echo "[ERROR) ($script) - Calculating coverage for $app_name is taking too long (1h). Timed out."
    exit $((coverage_exit_code))
fi

if [[ $coverage_exit_code -eq 0 ]]; then
    echo "[INFO] ($script) - Done calculating coverage for $app_name."
    exit $((coverage_exit_code))
else
    echo "[ERROR] ($script) - Failed to calculate coverage for $app_name (Err:$coverage_exit_code)."
    exit $((coverage_exit_code))
fi
