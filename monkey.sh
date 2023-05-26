#!/bin/bash
#
# Author: Jordan Doyle.
#
# Optional Parameters:
#   - --no-install : Run without installing app. Use exisitng installation. 
#   - --click-only : Run with click insterations only. 
#
# Required Parameters:
#   - Input APK File : The apk file on which to run Monkey tests. 
#   - Output Directory : Directory in which to save the output files.
#   - Number Of Tests : Number of Monkey tests to run.
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

function verify_test_completed() { # $1 is the test directory e.g. Test_1
    test_id="$(basename "$1" | tr '_' ' ' | tr '[:upper:]' '[:lower:]')"
    if [[ -d "$1" ]]; then
        if ! ls "$1"/logcat_*.log "$1"/monkey_*.log > /dev/null 2>&1 || ! grep -q -e "Events injected: $number_of_events" -e "// Monkey finished" "$1"/monkey_*.log; then
            echo -e "[ERROR] ($script) - Monkey $test_type interaction $test_id did not complete $number_of_events interactions on $app_name".
        fi
    fi

    monkey_started=0
    while read -r line; do
        monkey_log="Calling main entry com.android.commands.monkey.Monkey"
        if echo "$line" | grep -q "$monkey_log"; then
            monkey_started=1
            break
        fi

        if [[ monkey_started -eq 0 ]] && echo "$line" | grep -q "Monkey"; then
            echo "[ERROR] ($script) - Monkey commands found before Monkey started in $test_id on $app_name."
            break
        fi
    done < "$1"/logcat_*.log 
}

function is_test_completed() { # $1 is the test directory e.g. Test_1
    test_id="$(basename "$1" | tr '_' ' ' | tr '[:upper:]' '[:lower:]')"
    if [[ -d "$1" ]]; then
        if ls "$1"/logcat_*.log "$1"/monkey_*.log > /dev/null 2>&1 && grep -q -e "Events injected: $number_of_events" -e "// Monkey finished" -e "** Monkey aborted due to error." -e "** System appears to have crashed" "$1"/monkey_*.log; then
            echo 1
            return
        fi
    fi
    echo 0
}

function check_monkey_tests() {
    counter=1
    while [[ $counter -le $number_of_tests ]]; do
        test_output="$output_directory/Test_$counter"

        if [[ $1 == "is_test_completed" ]]; then
            completed=$(is_test_completed "$test_output")

            if [[ $completed -eq 0 ]]; then
                tests_completed=0
                break
            fi
        elif [[ $1 == "verify_test_completed" ]]; then
            verify_test_completed "$test_output"
        fi

        counter=$((counter+1))
    done
}

function close_app() {
    printf "[INFO] (%s) - Closing app... " "$script"
    adb shell input keyevent KEYCODE_HOME
    adb shell pm clear "$package"
}

function exit_cleanup {
    echo "[INFO] ($script) - Killing Monkey..."
    adb shell ps | awk '/com\.android\.commands\.monkey/ { system("adb shell kill " $2) }'
    
    close_app
    
    if [[ $install_app == 0 ]]; then
        if adb shell pm list packages | grep -q "$package"; then
            adb uninstall "$package" > /dev/null 2>&1 
        fi
    fi
}

install_app=0 
if [[ "$1" == "--no-install" ]]; then
    echo "[INFO] ($script) - Install disabled, using existing app."
    install_app=1
    shift
fi

click_only=1
test_directory="Monkey_All"
test_type="all"
if [[ "$1" == "--click-only" ]]; then
    click_only=0
    test_directory="Monkey_Click"
    test_type="click"
    shift
fi

if [[ ! -d "$ANDROID_HOME" ]]; then
    echo "[ERROR] ($script) - Android home environment variable not set."
    exit 2
fi

tool_versions=("$ANDROID_HOME"/build-tools/*/)
build_tools=$(printf "%s\n" "${tool_versions[*]}" | sort -nr | head -n1)
if [[ ! -d "$build_tools" ]]; then
    echo "[ERROR] ($script) - Android build tools directory ($build_tools) does not exist."
    exit 3
fi

build_tools_version=$(basename "$build_tools")
aapt="$ANDROID_HOME/build-tools/$build_tools_version/aapt"
if [[ ! -f "$aapt" ]]; then
    echo "[ERROR] ($script) - aapt cannot be found in build tools $build_tools_version."
    exit 4
fi

if [ $# -ne 3 ]; then
    echo "[ERROR] ($script) - Required arguments not provided."
    exit 5
fi

if [[ ! -f "$1" ]] || [[ "$1" != *.apk ]]; then
	echo "[ERROR] ($script) -Input file ($1) does not exist."
    exit 6
fi
apk_file="$1"
app_name=$(basename "$apk_file" .apk)

if [[ ! -d "$2" ]]; then
    echo "[ERROR] ($script) - Output directory ($2) does not exist."
    exit 7
fi
output_directory="$2/$test_directory"
[[ ! -d "$output_directory" ]] && mkdir -p "$output_directory"

# find "$output_directory" -type f \( -name '*.csv' -o -name '*.txt' \) -delete

re='^[0-9]+$'
if ! [[ $3 =~ $re ]]; then
   echo "[ERROR] ($script) - Number of tests ($3) is not a positive integer."
   exit 8
fi
number_of_tests=$3
number_of_events=500

tests_completed=1
check_monkey_tests "is_test_completed"
if [[ $tests_completed -eq 1 ]]; then
    echo "[INFO] ($script) - Monkey $test_type interaction tests for $app_name already completed. Skipping."
    check_monkey_tests "verify_test_completed"
    exit 0
fi

package=$($aapt dump badging "$apk_file" | grep "package: name='" | sed "s/^[^']*'//" | sed "s/'.*//")

trap exit_cleanup EXIT

if [[ $install_app == 0 ]]; then
    if ! adb shell pm list packages | grep -q "$package"; then
        adb install -r "$apk_file" > /dev/null 2>&1 
    fi
fi

if ! adb shell pm list packages | grep -q "$package"; then
    echo "[ERROR] ($script) - App package ($package) invalid or not installed."
    exit 9
fi

use_seeds=0
seeds=(7598364 508328 5420736 8377926 102074 2617534 6305440 6211883 4468766 8377198)
if [ "$number_of_tests" -gt ${#seeds[@]} ]; then
    echo "[WARN] ($script) - Not enough seed values, running without seeds."
    use_seeds=1
fi

echo "[INFO] ($script) - Monkey $test_type interaction tests running on $app_name..."

counter=1
while [[ $counter -le $number_of_tests ]]; do
    test_output="$output_directory/Test_$counter"
    monkey_log="$test_output/monkey_$counter.log"
    logcat_log="$test_output/logcat_$counter.log"

    completed=$(is_test_completed "$test_output")
    if [[ $completed -eq 1 ]]; then
        echo "[INFO] ($script) - Monkey $test_type interaction test $counter on $app_name already completed. Skipping."
        counter=$((counter+1))
        continue
    else
       [[ -d "$test_output" ]] && rm -r "$test_output"
    fi
    mkdir "$test_output"

    echo "[INFO] ($script) - Running $test_type interaction test $counter on $app_name..."
    
    arguments=(-p "$package" -vvv --throttle 500)
    [[ $click_only == 0 ]] && arguments+=(--pct-touch 100)
    [[ $use_seeds == 0 ]] && arguments+=(-s "${seeds[$counter-1]}")
    arguments+=("$number_of_events")

    adb logcat -c
    adb shell monkey "${arguments[@]}" &> "$monkey_log"
    monkey_exit_code=$?
    close_app
    adb logcat -d > "$logcat_log"
    adb logcat -c
    
    if [[ $OSTYPE == 'darwin'* ]]; then
        sed -i '' '/entry com.android.commands.monkey.Monkey/,$!d' "$logcat_log"
    else
        sed -i '/entry com.android.commands.monkey.Monkey/,$!d' "$logcat_log"
    fi

    if [[ $monkey_exit_code -eq 0 ]]; then
        echo "[INFO] ($script) - Monkey $test_type interaction test $counter finished on $app_name."
    else
        echo "[ERROR] ($script) - Moneky $test_type interaction test $counter failed on $app_name."
    fi

    verify_test_completed "$test_output"
    counter=$((counter+1))
    sleep 1
done
