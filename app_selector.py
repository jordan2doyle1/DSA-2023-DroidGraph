#
# Author Jordan Doyle.
#

import json
import os
import random
import sys
from datetime import datetime

import wget

APP_INDEX_FILE = 'input/app_index.json'
SCRIPT_NAME = os.path.basename(sys.argv[0])
OUTPUT_DIRECTORY = 'output/'

SEED_VALUES = [29, 147, 5, 86, 24, 61, 55, 44, 88, 32, 27, 1, 121, 14, 31, 17]

MAX_APP_AGE = 10
MIN_SDK_VERSION = 16
MAX_SDK_VERSION = 29

FILTERED_CATEGORIES = {
    "Games"  # Many gaming apps use frameworks such as unity which cannot be analysed by FlowDroid.
}

FILTERED_APPS = [
    "com.androidfromfrankfurt.workingtimealert",  # App would not install on the Android emulator.
    "click.dummer.yidkey",  # Keyboard app not suitable for our interface tests, no isolation.
    "org.retroshare.android.qml_app",  # App would not install on the Android emulator.
    "pl.net.szafraniec.NFCTagmaker",  # App fails to launch on the Android emulator.
    "com.diblui.fullcolemak",  # App would not install on the Android emulator.
    "de.cketti.dashclock.k9",  # Not a standard app, appears to be an extension of some sort.
    "se.manyver",  # App would not install on the Android emulator.
    "de.devmil.muzei.bingimageofthedayartsource",  # Not a standard app, appears to be an extension of some sort.
    "info.tangential.cone",  # App would not install on the Android emulator.
    "org.weilbach.splitbills",  # App can't be instrumented. Code is obfuscated.
    "io.lbry.browser",  # App would not install on the Android emulator.
    "org.bitbucket.watashi564.combapp",  # App can't be instrumented. Code is obfuscated.
    "org.dash.electrum.electrum_dash",  # App would not install on the Android emulator.
    "com.mmazzarolo.breathly",  # App can't be instrumented. Code is obfuscated.
]


def write_json_file(file_name, data):
    with open(file_name, 'w') as json_file:
        json_file.write(json.dumps(data, indent=4))


app_index_file = open(APP_INDEX_FILE)
package_data = json.load(app_index_file)

# Run below line once to format the original index file.
# write_json_file(APP_INDEX_FILE, package_data)

base_url = package_data["repo"]["address"]

categories = list(package_data["repo"]["categories"].keys())
categories = [category for category in categories if category not in FILTERED_CATEGORIES]
write_json_file(OUTPUT_DIRECTORY + 'categories.json', categories)

print("[INFO] (" + SCRIPT_NAME + ") - Index contains " + str(len(package_data["packages"])) + " packages.")


def get_latest_version(last_updated, versions):
    latest_version = None
    for package_version in versions.values():
        if package_version["added"] == last_updated:
            latest_version = package_version
            break
        elif latest_version is None or latest_version["added"] < package_version["added"]:
            latest_version = package_version

    return latest_version


def get_package_dictionary(package, metadata, version):
    return {"name": metadata["name"]["en-US"], "package": package, "versionCode": version["manifest"]["versionCode"],
            "lastUpdated": metadata["lastUpdated"], "categories": metadata["categories"],
            "url": base_url + version["file"]["name"], "minSdkVersion": version["manifest"]["usesSdk"]["minSdkVersion"],
            "targetSdkVersion": version["manifest"]["usesSdk"]["targetSdkVersion"]}


manual_count = category_count = age_count = sdk_count = 0


def filtered(package, metadata, version):
    global manual_count, category_count, age_count, sdk_count

    if package in FILTERED_APPS:
        manual_count += 1
        return True

    if not set(metadata["categories"]).isdisjoint(FILTERED_CATEGORIES):
        category_count += 1
        return True

    last_updated = datetime.fromtimestamp(metadata["lastUpdated"] / 1000.0)
    number_of_years = (datetime.now() - last_updated).days / 365
    if number_of_years > MAX_APP_AGE:
        age_count += 1
        return True

    if "usesSdk" in version["manifest"]:
        if not MIN_SDK_VERSION <= version["manifest"]["usesSdk"]["minSdkVersion"] <= MAX_SDK_VERSION or \
                not MIN_SDK_VERSION <= version["manifest"]["usesSdk"]["targetSdkVersion"] <= MAX_SDK_VERSION:
            sdk_count += 1
            return True
    else:
        sdk_count += 1
        return True

    return False


def list_filtered_packages():
    packages = {}

    for package_name, package_details in package_data["packages"].items():
        package_metadata = package_details["metadata"]
        package_version = get_latest_version(package_metadata["lastUpdated"], package_details["versions"])

        if not filtered(package_name, package_details["metadata"], package_version):
            packages[package_name] = get_package_dictionary(package_name, package_metadata, package_version)

    return packages


filtered_packages = list_filtered_packages()
write_json_file(OUTPUT_DIRECTORY + 'packages.json', filtered_packages)

print("[INFO] (" + SCRIPT_NAME + ") - Filtered " + str(manual_count) + " packages manually.")
print("[INFO] (" + SCRIPT_NAME + ") - Filtered " + str(category_count) + " packages with category filter.")
print("[INFO] (" + SCRIPT_NAME + ") - Filtered " + str(age_count) + " packages above max age.")
print("[INFO] (" + SCRIPT_NAME + ") - Filtered " + str(sdk_count) + " packages with unsuitable SDK version.")
print("[INFO] (" + SCRIPT_NAME + ") - " + str(len(filtered_packages)) + " packages remain after filtering.")


def list_category_packages(category):
    packages = {}

    for package_name, package_details in filtered_packages.items():
        if category in package_details["categories"]:
            packages[package_name] = package_details

    return packages


category_packages = {}
for current_category in categories:
    category_packages[current_category] = list_category_packages(current_category)
write_json_file(OUTPUT_DIRECTORY + 'category_packages.json', category_packages)


def download_apk_file(package_details):
    file_name = package_details["name"].title().replace(' ', '_') + "_" + str(package_details["versionCode"]) + '.apk'
    if os.path.isfile('apk/' + file_name):
        print("[INFO] (" + SCRIPT_NAME + ") - Download already exists.")
    else:
        print("[INFO] (" + SCRIPT_NAME + ") - Downloading " + file_name + "...", end=" ")
        wget.download(package_details["url"], 'apk/' + file_name)
        print("Done.")


def get_random_app_per_category(download=True):
    random_packages = {}
    selected_packages = []
    seed_index = 0

    for category, packages in category_packages.items():
        if len(packages) == 0:
            print("[ERROR] (" + SCRIPT_NAME + ") - No packages with category " + category)
            continue

        random_package = random_number = None
        while random_package is None or random_package in selected_packages:
            random.seed(SEED_VALUES[seed_index])
            random_number = random.randint(1, len(packages))
            random_package = list(packages.keys())[random_number - 1]

        package_details = packages[random_package]
        random_packages[category] = package_details
        selected_packages.append(random_package)
        seed_index += 1

        print("[INFO] (" + SCRIPT_NAME + ") - Selected '{0}' app {1} from {2}."
              .format(package_details["name"], random_number, len(packages)))
        if download:
            download_apk_file(package_details)

    return random_packages


random_app_per_category = get_random_app_per_category()
write_json_file(OUTPUT_DIRECTORY + 'random_app_per_category.json', random_app_per_category)
