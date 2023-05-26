#
# Author Jordan Doyle.
#

import argparse
import os
import re
import sys

import matplotlib.pyplot as mpl
import numpy as np
import pandas as pd
import tikzplotlib

SCRIPT_NAME = os.path.basename(sys.argv[0])

arg_parser = argparse.ArgumentParser()
arg_parser.add_argument("-d", "--directory", type=str, required=True, help="The app test results base directory.")
args = arg_parser.parse_args()


def get_test_type(base_directory):
    if "All" in base_directory:
        return "All"
    elif "Click" in base_directory:
        return "Click"
    else:
        return ""


def is_test_directory(directory_name):
    pattern = re.compile("^Test_\\d{1,2}$")
    return pattern.match(directory_name)


def get_test_number(directory_name):
    pattern = re.compile("^Test_(\\d{1,2})$")
    match = pattern.match(directory_name)
    if match:
        return match.group(1)
    return None


def invalid_csv(input_file):
    data_frame = pd.read_csv(input_file)

    for index, row in data_frame.iterrows():
        if not (isinstance(row['interaction'], float) and isinstance(row['control'], float) and isinstance(
                row['method'], float)):
            print("[ERROR] (" + SCRIPT_NAME + ") - Invalid data in " + input_file + ".")
            return True

    return False


def tikzplotlib_fix_ncols(obj):
    """
    workaround for matplotlib 3.6 renamed legend's _ncol to _ncols, which breaks tikzplotlib
    """
    if hasattr(obj, "_ncols"):
        # noinspection PyProtectedMember
        obj._ncol = obj._ncols
    for child in obj.get_children():
        tikzplotlib_fix_ncols(child)


def plot(title, input_file, output_file):
    data_frame = pd.read_csv(input_file)

    figure, axis = mpl.subplots()
    axis.plot(data_frame['interaction'], data_frame['control'], label='Interface')
    axis.plot(data_frame['interaction'], data_frame['method'], label='Method')

    axis.set_title(title)
    axis.set_xlabel('Number of Interactions')
    axis.set_ylabel('Percentage Coverage')

    mpl.xticks(np.arange(0, 501, step=50), np.arange(0, 501, step=50))
    y_axis_max_value = data_frame[['control', 'method']].max().max()
    mpl.yticks(np.arange(0, y_axis_max_value + 5, step=5), np.arange(0, y_axis_max_value + 5, step=5))

    mpl.legend(loc='lower right')

    mpl.savefig(output_file + '.png')
    tikzplotlib_fix_ncols(figure)
    tikzplotlib.save(output_file + '.tex')
    mpl.close('all')


def plot_monkey_results(base_directory):
    test_type = get_test_type(base_directory)

    for test_directory in os.listdir(base_directory):
        if os.path.isdir(os.path.join(base_directory, test_directory)) and is_test_directory(test_directory):
            test_number = get_test_number(test_directory)
            data_file = os.path.join(base_directory, test_directory, "results_raw_" + test_number + ".csv")

            if os.path.isfile(data_file):
                if invalid_csv(data_file):
                    continue

                output_file = os.path.join(base_directory, test_directory, "results_plot_" + test_number)
                plot("Monkey " + test_type + " Interaction Test " + test_number + " Coverage", data_file, output_file)

    average_data_file = os.path.join(base_directory, "average_results_raw.csv")
    average_output_file = os.path.join(base_directory, "average_plot")
    if os.path.isfile(average_data_file):
        if invalid_csv(average_data_file):
            return

        plot("Monkey " + test_type + " Interaction Average Coverage", average_data_file, average_output_file)


monkey_click_directory = os.path.join(args.directory, "Monkey_Click")
if os.path.isdir(monkey_click_directory):
    print("[INFO] (" + SCRIPT_NAME + ") - Plotting data for Monkey click interaction tests.")
    plot_monkey_results(monkey_click_directory)
else:
    print("[ERROR] (" + SCRIPT_NAME + ") - Monkey click interaction test data not found.")


def plot_dynamic_results(base_directory):
    data_file = os.path.join(base_directory, "results_raw.csv")
    output_file = os.path.join(base_directory, "results_plot")
    if os.path.isfile(data_file):
        if invalid_csv(data_file):
            return

        plot("Dynamic Traversal Test", data_file, output_file)


dynamic_traversal_directory = os.path.join(args.directory, "Droid_Traversal")
if os.path.isdir(dynamic_traversal_directory):
    print("[INFO] (" + SCRIPT_NAME + ") - Plotting data for dynamic traversal test.")
    plot_dynamic_results(dynamic_traversal_directory)
else:
    print("[ERROR] (" + SCRIPT_NAME + ") - Dynamic traversal test data not found.")


def plot_dynamic_vs_monkey(coverage_type, dynamic_base_directory, monkey_base_directory, output_directory):
    test_type = get_test_type(monkey_base_directory)

    dynamic_data_file = os.path.join(dynamic_base_directory, "results_raw.csv")
    average_data_file = os.path.join(monkey_base_directory, "average_results_raw.csv")
    minimum_data_file = os.path.join(monkey_base_directory, "minimum_results_raw.csv")
    maximum_data_file = os.path.join(monkey_base_directory, "maximum_results_raw.csv")

    if not os.path.isfile(dynamic_data_file) or not os.path.isfile(average_data_file) or not os.path.isfile(
            minimum_data_file) or not os.path.isfile(maximum_data_file):
        return

    if invalid_csv(dynamic_data_file) or invalid_csv(average_data_file) or invalid_csv(
            minimum_data_file) or invalid_csv(maximum_data_file):
        return

    dynamic_data = pd.read_csv(dynamic_data_file)
    monkey_data = pd.read_csv(average_data_file)
    min_data = pd.read_csv(minimum_data_file)
    max_data = pd.read_csv(maximum_data_file)

    figure, axis = mpl.subplots()
    axis.plot(monkey_data['interaction'], monkey_data[coverage_type.lower()], label='Monkey Click', color='green',
              linewidth=3)
    axis.plot(dynamic_data['interaction'], dynamic_data[coverage_type.lower()], label='ET', color='orange', linewidth=3)
    axis.fill_between(monkey_data['interaction'], min_data[coverage_type.lower()], max_data[coverage_type],
                      color='silver', label="Max/Min")

    axis.set_ylabel('Percentage Coverage')
    # axis.set_title("Dynamic VS Monkey " + test_type + " Interaction " + coverage_type.capitalize() + " Coverage")
    axis.set_xlabel('Number of Interactions')

    mpl.xticks(np.arange(0, 501, step=50), np.arange(0, 501, step=50))
    y_axis_max_value = max(dynamic_data[[coverage_type.lower()]].max().max(),
                           monkey_data[[coverage_type.lower()]].max().max(),
                           min_data[[coverage_type.lower()]].max().max(),
                           max_data[[coverage_type.lower()]].max().max())
    mpl.yticks(np.arange(0, y_axis_max_value + 5, step=5), np.arange(0, y_axis_max_value + 5, step=5))

    mpl.legend(loc='lower right')

    output_file_name = coverage_type.lower() + "_results_" + test_type.lower() + "_plot"
    output_file = os.path.join(output_directory, output_file_name)
    mpl.savefig(output_file + '.png')
    tikzplotlib_fix_ncols(figure)
    tikzplotlib.save(output_file + '.tex')
    mpl.close('all')


if os.path.isdir(dynamic_traversal_directory) and os.path.isdir(monkey_click_directory):
    print("[INFO] (" + SCRIPT_NAME + ") - Plotting data for dynamic traversal vs. Monkey click interaction tests.")
    plot_dynamic_vs_monkey('control', dynamic_traversal_directory, monkey_click_directory, args.directory)
    plot_dynamic_vs_monkey('method', dynamic_traversal_directory, monkey_click_directory, args.directory)
else:
    print("[ERROR] (" + SCRIPT_NAME + ") - Dynamic traversal test data or Monkey click test data not found.")
