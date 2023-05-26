#
# Author Jordan Doyle.
#

import argparse
import os
import sys

import matplotlib.pyplot as mpl
import numpy as np
import pandas as pd
import tikzplotlib

SCRIPT_NAME = os.path.basename(sys.argv[0])

arg_parser = argparse.ArgumentParser()
arg_parser.add_argument("-d", "--directory", type=str, required=True, help="The test results base directory.")
args = arg_parser.parse_args()


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


def plot_app_comparison(coverage_type, base_directory):
    x_axis_keys = []
    dynamic_y_axis_values = []
    monkey_click_y_values = []
    monkey_all_y_values = []

    figure, axis = mpl.subplots(figsize=(6.4, 6.4))
    figure.subplots_adjust(bottom=0.25)

    for directory in os.listdir(base_directory):
        if not os.path.isdir(os.path.join(base_directory, directory)):
            continue

        x_axis_keys.append(directory[:directory.rfind('_')].replace('_', ''))

        dynamic_value = 0
        dynamic_directory = os.path.join(base_directory, directory, 'Droid_Traversal')
        if os.path.isdir(dynamic_directory):
            dynamic_data_file = os.path.join(dynamic_directory, "results_raw.csv")
            if os.path.isfile(dynamic_data_file) and not invalid_csv(dynamic_data_file):
                dynamic_frame = pd.read_csv(dynamic_data_file)
                dynamic_value = dynamic_frame[[coverage_type.lower()]].max().max()
        dynamic_y_axis_values.append(dynamic_value)

        monkey_click_value = 0
        monkey_click_directory = os.path.join(base_directory, directory, 'Monkey_Click')
        if os.path.isdir(monkey_click_directory):
            monkey_click_data_file = os.path.join(monkey_click_directory, "average_results_raw.csv")
            if os.path.isfile(monkey_click_data_file) and not invalid_csv(monkey_click_data_file):
                monkey_click_frame = pd.read_csv(monkey_click_data_file)
                monkey_click_value = monkey_click_frame[[coverage_type.lower()]].max().max()
        monkey_click_y_values.append(monkey_click_value)

        monkey_all_value = 0
        monkey_all_directory = os.path.join(base_directory, directory, 'Monkey_All')
        if os.path.isdir(monkey_all_directory):
            monkey_all_data_file = os.path.join(monkey_all_directory, "average_results_raw.csv")
            if os.path.isfile(monkey_all_data_file) and not invalid_csv(monkey_all_data_file):
                monkey_all_frame = pd.read_csv(monkey_all_data_file)
                monkey_all_value = monkey_all_frame[[coverage_type.lower()]].max().max()
        monkey_all_y_values.append(monkey_all_value)

    x_axis_indexes = np.arange(len(x_axis_keys))
    bar_width = 0.30

    axis.bar(x_axis_indexes, dynamic_y_axis_values, width=bar_width, color='orange', label="ET", hatch="/")
    axis.bar(x_axis_indexes + bar_width, monkey_click_y_values,width=bar_width, color='green', label="Monkey Click")
    axis.bar(x_axis_indexes + (bar_width * 2), monkey_all_y_values, width=bar_width, color='cyan', label="Monkey All",
             hatch="\\")

    # axis.set_title("Dynamic VS Monkey " + coverage_type + " Interaction Coverage")
    mpl.yticks(np.arange(0, 110, step=10), np.arange(0, 110, step=10))
    axis.set_xticks(x_axis_indexes + bar_width, x_axis_keys, rotation=90)
    mpl.legend(loc='upper right')

    output_file = os.path.join(base_directory, coverage_type.lower() + "_results_bar_plot")
    mpl.savefig(output_file + '.png')
    tikzplotlib_fix_ncols(figure)
    tikzplotlib.save(output_file + '.tex')
    mpl.close('all')


def table_app_comparison(coverage_type, base_directory):
    table_lines = []

    for directory in os.listdir(base_directory):
        if not os.path.isdir(os.path.join(base_directory, directory)):
            continue

        line = directory[:directory.rfind('_')].replace('_', '')

        dynamic_value = 0
        dynamic_directory = os.path.join(base_directory, directory, 'Droid_Traversal')
        if os.path.isdir(dynamic_directory):
            dynamic_data_file = os.path.join(dynamic_directory, "results_raw.csv")
            if os.path.isfile(dynamic_data_file) and not invalid_csv(dynamic_data_file):
                dynamic_frame = pd.read_csv(dynamic_data_file)
                dynamic_value = dynamic_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(dynamic_value)

        monkey_click_value = 0
        monkey_click_directory = os.path.join(base_directory, directory, 'Monkey_Click')
        if os.path.isdir(monkey_click_directory):
            monkey_click_data_file = os.path.join(monkey_click_directory, "average_results_raw.csv")
            if os.path.isfile(monkey_click_data_file) and not invalid_csv(monkey_click_data_file):
                monkey_click_frame = pd.read_csv(monkey_click_data_file)
                monkey_click_value = monkey_click_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_click_value)

        monkey_min_value = 0
        monkey_click_directory = os.path.join(base_directory, directory, 'Monkey_Click')
        if os.path.isdir(monkey_click_directory):
            monkey_min_data_file = os.path.join(monkey_click_directory, "minimum_results_raw.csv")
            if os.path.isfile(monkey_min_data_file) and not invalid_csv(monkey_min_data_file):
                monkey_min_frame = pd.read_csv(monkey_min_data_file)
                monkey_min_value = monkey_min_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_min_value)

        monkey_max_value = 0
        monkey_click_directory = os.path.join(base_directory, directory, 'Monkey_Click')
        if os.path.isdir(monkey_click_directory):
            monkey_max_data_file = os.path.join(monkey_click_directory, "maximum_results_raw.csv")
            if os.path.isfile(monkey_max_data_file) and not invalid_csv(monkey_max_data_file):
                monkey_max_frame = pd.read_csv(monkey_max_data_file)
                monkey_max_value = monkey_max_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_max_value)

        monkey_all_value = 0
        monkey_all_directory = os.path.join(base_directory, directory, 'Monkey_All')
        if os.path.isdir(monkey_all_directory):
            monkey_all_data_file = os.path.join(monkey_all_directory, "average_results_raw.csv")
            if os.path.isfile(monkey_all_data_file) and not invalid_csv(monkey_all_data_file):
                monkey_all_frame = pd.read_csv(monkey_all_data_file)
                monkey_all_value = monkey_all_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_all_value)

        monkey_min_value = 0
        monkey_all_directory = os.path.join(base_directory, directory, 'Monkey_All')
        if os.path.isdir(monkey_all_directory):
            monkey_min_data_file = os.path.join(monkey_all_directory, "minimum_results_raw.csv")
            if os.path.isfile(monkey_min_data_file) and not invalid_csv(monkey_min_data_file):
                monkey_min_frame = pd.read_csv(monkey_min_data_file)
                monkey_min_value = monkey_min_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_min_value)

        monkey_max_value = 0
        monkey_all_directory = os.path.join(base_directory, directory, 'Monkey_All')
        if os.path.isdir(monkey_all_directory):
            monkey_max_data_file = os.path.join(monkey_all_directory, "maximum_results_raw.csv")
            if os.path.isfile(monkey_max_data_file) and not invalid_csv(monkey_max_data_file):
                monkey_max_frame = pd.read_csv(monkey_max_data_file)
                monkey_max_value = monkey_max_frame[[coverage_type.lower()]].max().max()
        line = line + " & " + str(monkey_max_value) + " \\\\"

        table_lines.append(line)

    output_file = os.path.join(base_directory, coverage_type.lower() + "_results_table.txt")
    with open(output_file, 'w') as fp:
        for item in table_lines:
            fp.write("%s\n" % item)


print("[INFO] (" + SCRIPT_NAME + ") - Plotting data for app comparison bar chart.")
plot_app_comparison('Control', args.directory)
plot_app_comparison('Method', args.directory)

print("[INFO] (" + SCRIPT_NAME + ") - Tabling data for app comparison.")
table_app_comparison('Control', args.directory)
table_app_comparison('Method', args.directory)
