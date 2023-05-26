#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input APK File : The apk file on which to run tests. 
#   - Output Directory : Directory in which to save the output files.
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

function exit_cleanup {
    printf "[INFO] (%s) - Changing file permisions... " "$script"
    chown -R 1006:1009 "$output_directory"
    echo "Done."
}

function wait_for_emulator_bootup() {
    running_output=true
    wait_counter=0

    while true; do
        if adb shell getprop sys.boot_completed > /dev/null 2>&1; then
            if $running_output; then
                echo "[INFO] ($script) - Android emulator running."
                running_output=false
            fi

            if adb shell getprop init.svc.bootanim | grep -q "stopped"; then
                echo "[INFO] ($script) - Android emulator finished booting."
                break
            fi
        fi

        sleep 1
        wait_counter=$((wait_counter+1))

        if [[ $wait_counter -eq  300 ]]; then
            echo "[ERROR] ($script) - Android emulator failed to boot. Timed out (5m)."
            exit 5
        fi
    done
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
output_directory="$2/$app_name"
[[ ! -d "$output_directory" ]] && mkdir -p "$output_directory"

trap exit_cleanup EXIT

./instrument.sh "$apk_file" "$output_directory"
instrument_exit=$?
[[ $instrument_exit -ne 0 ]] && exit $((instrument_exit)) 
apk_file="$output_directory/Droid_Instrument/$app_name.apk"

if [[ ! -f /.dockerenv ]]; then
    ./python_venv.sh
    python_venv_exit=$?
    [[ $python_venv_exit -ne 0 ]] && exit $((python_venv_exit)) 
fi

./androguard.sh "$apk_file" "$output_directory" "$HOME/python_venv"
androguard_exit=$?
[[ $androguard_exit -ne 0 ]] && exit $((androguard_exit))

./droid_graph.sh "$apk_file" "$output_directory"
droid_graph_exit=$?
[[ $droid_graph_exit -ne 0 ]] && exit $((droid_graph_exit))

# wait_for_emulator_bootup

./monkey.sh --click-only "$apk_file" "$output_directory" 10
monkey_click_exit=$?
[[ $monkey_click_exit -ne 0 ]] && exit $((monkey_click_exit)) 

./monkey.sh "$apk_file" "$output_directory" 10
monkey_all_exit=$?
[[ $monkey_all_exit -ne 0 ]] && exit $((monkey_all_exit))

./traversal.sh "$apk_file" "$output_directory"
traversal_exit=$?
[[ $traversal_exit -ne 0 ]] && exit $((traversal_exit))

import_file="$output_directory/Droid_Graph/app_control_flow_graph.json"
if [[ -f "$import_file" ]]; then
    ./coverage.sh "$apk_file" "$import_file" "$output_directory"
    coverage_exit=$?
    [[ $coverage_exit -ne 0 ]] && exit $((coverage_exit))
fi

# shellcheck source=/dev/null
source "$HOME/python_venv/bin/activate"
python3 data_plot.py -d "$output_directory"
data_plot_exit=$?

exit $((data_plot_exit))