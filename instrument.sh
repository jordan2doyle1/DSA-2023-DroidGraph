#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run Droid Instrument.
#   - Output Directory : Directory in which to save the output files. 
# 

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

# shellcheck disable=SC2317
function exit_cleanup {
    target_directory="target"
    [[ -d "$target_directory" ]] && mv "$target_directory" "$output_directory"
}

if [ $# -ne 2 ]; then
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
output_directory="$2/Droid_Instrument"

jar_file="DroidInstrument-1.0-SNAPSHOT-jar-with-dependencies.jar"
if [[ ! -f "$jar_file" ]] || [[ ! -f "sign.sh" ]] || [[ ! -f "key" ]]; then
    echo "[ERROR] ($script) - Droid instrument jar file, signing script, or keystore file does not exist."
    exit 5
fi

instrumented_apk_file="$output_directory/$app_name.apk"
signing_log_file="$output_directory/sign.log"
if [[ -f "$instrumented_apk_file" ]] && [[ -f "$signing_log_file" ]] && grep -q "The app is signed" "$signing_log_file"; then
    echo "[INFO] ($script) - $app_name already instrumented. Skipping."
    exit 0
fi

[[ -d "$output_directory" ]] && rm -r "$output_directory"
mkdir -p "$output_directory"

trap exit_cleanup EXIT

echo "[INFO] ($script) - Instrumenting $app_name... "
timeout 5m java -Xms4g -jar $jar_file -a "$apk_file" -o "$output_directory" &> "$output_directory/cmd.log"
instrument_exit_code=$?

if [[ $instrument_exit_code -eq 124 ]]; then
    echo "[INFO] ($script) - Instrumentation on $app_name is taking too long (5m). Timed out."
    exit $((instrument_exit_code))
fi

if [[ -f "$instrumented_apk_file" ]] && [[ $instrument_exit_code -eq 0 ]]; then
    ./sign.sh "$instrumented_apk_file" key >> "$signing_log_file" 2>&1
    signing_exit_code=$?
    if [[ $signing_exit_code -eq 0 ]]; then
        echo "[INFO] ($script) - Instrumenting $app_name finished."
        exit $((instrument_exit_code))
    else
        echo "[ERROR] ($script) - Failed to sign $app_name APK (Err:$signing_exit_code)."
        exit $((signing_exit_code))
    fi
else
    echo "[INFO] ($script) - Instrumenting $app_name failed (Err:$instrument_exit_code)."
    exit $((instrument_exit_code))
fi
