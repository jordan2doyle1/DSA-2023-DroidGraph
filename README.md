## DroidGraph

DroidGraph: a framework to generate a comprehensive control flow model of Android applications using traditional static analysis and efficient systematic exploratory tests

Please note that this repository is not maintained regularly.

Publication:
```
@inproceedings{doyle2023modelling,
  title={Modelling Android applications through static analysis and systematic exploratory testing},
  author={Doyle, Jordan and Laurent, Thomas and Ventresque, Anthony},
  booktitle={2023 International Conference on Dependable Systems and Their Applications (DSA)},
  year={2023},
  note={Status: accepted and presented at DSA.}
}
```
## Installation

Simply download the artefact [here](https://github.com/jordan2doyle1/DSA-2023-DroidGraph/archive/877dde05c49f4ab217980ac73d5ea60e764c981e.zip) and apply the following environment configuration: 

### Environment Configration

For simplisity, it is highly recommended that you use the Docker image provided. The code was last used with Docker Desktop v4.

## Usage
    ./test_apps.sh --docker <input-directory> <output-directory>

where input-directory is the path to a directory containing all the subject apk under test and output-directory is the path to a directory where results will be stored.

### Output
Outputs for each APK provided are stored in a sub-directory (APK file name) with the following content:

    /Droid_Instrument - Instrumented APK file.
    /Droid_Traversal - Traversal results.
    /AndroGuard - Androguard output GML file.
    /Droid_Graph - Final app model in JSON format.
    /Monkey_All - Monkey (all interactions) average results (sub-directories for individual test results).
    /Monkey_Click - Monkey (click interactions) average results (sub-directories for individual test results).

Each subdirectory contains all related support files and logs. All results are provided as a PNG line graph, TEX graph and TXT table.

## Contact
<jordan.doyle@ucdconnect.ie>
