#!/bin/bash
#
# Author: Jordan Doyle.
#
# Required Parameters:
#   - Input Directory : Directory containing apk files. 
#   - Output Directory : Directory in which to save the output files.
#
# Optional Parameters:
#   - --docker : Run tests in a Docker container using docker.sh.
#   - --clean : Delete all previous results before executing tests.
#

docker=0
if [[ "$1" == "--docker" ]]; then
    docker=1
    shift
fi

clean=0
if [[ "$1" == "--clean" ]]; then
    clean=1
    shift
fi

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

if [ $# -ne 2 ]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 2
fi

if [ ! -d "$1" ]; then
    echo "[ERROR] ($script) - Input directory ($1) does not exist."
    exit 3
fi
input_directory="$1"

if [ ! -d "$2" ]; then
    echo "[ERROR] ($script) - Output directory ($2) does not exist."
    exit 4
fi
output_directory="$2"

if [[ $clean -eq 1 ]]; then
    echo "[INFO] ($script) - Deleting past results, running clean tests."
    [[ -d "$output_directory" ]] && rm -r "$output_directory"
    mkdir -p "$output_directory"
fi

# find "$output_directory" -type f \( -name '*.png' -o -name '*.tex' \) -delete

exit_code=0
shopt -s nullglob
for file in "$input_directory"/*.apk; do
    app_name="$(basename "$file" .apk)"
    echo "[INFO] ($script) - Testing $app_name... "

    if [[ $docker -eq 1 ]]; then
        echo "[INFO] ($script) - Running test in Docker container."
        ./docker.sh "test_app.sh" "$file" "$output_directory"
        test_exit_code=$?
    else
        ./test_app.sh "$file" "$output_directory"
        test_exit_code=$?
    fi
    
    if [[ $test_exit_code -eq 0 ]]; then
        echo "[INFO] ($script) - Testing $app_name finished."
    else
        exit_code=$test_exit_code
        echo "[ERROR] ($script) - Testing $app_name failed (Err: $exit_code)."
    fi
done

if [[ $docker -eq 1 ]]; then
    echo "[INFO] ($script) - Creating bar plot in Docker container."
    ./docker.sh "bar_plot.py" "-d" "$output_directory"
    bar_plot_exit=$?
else
    ./python_venv.sh
    # shellcheck source=/dev/null
    source "$HOME/python_venv/bin/activate"
    python3 bar_plot.py -d "$output_directory"
    bar_plot_exit=$?
fi

if [[ $bar_plot_exit -eq 0 ]]; then
    echo "[INFO ($script) - Bar plot created successfully."
else
    echo "[ERROR] ($script) - Failed to create bar plot (Err: $bar_plot_exit)."
    exit $((bar_plot_exit))
fi

exit $((exit_code))
