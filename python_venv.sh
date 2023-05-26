#!/bin/bash
#
# Author: Jordan Doyle.
#

script=$(echo "$0" | rev | cut -d "/" -f1 | rev)

if ! pyenv --version > /dev/null 2>&1; then
	echo "[ERROR] ($script) - PyEnv is not installed."
	exit 2
fi

if ! pyenv versions | grep -q "3.8.13"; then 
	echo "[ERROR] ($script) -  Python version 3.8.13 is not installed."
	exit 3
fi

python_binary="$HOME/.pyenv/versions/3.8.13/bin/python3"
if [[ ! -f "$python_binary" ]]; then
	echo "[ERROR] ($script) - Python version 3.8.13 binary not found."
	exit 4
fi

if ! virtualenv --version > /dev/null 2>&1; then
	echo "[ERROR] ($script) - Python package virtualenv is not installed."
	exit 5
fi

env_directory="$HOME/python_venv"

if [[ ! -f "$env_directory/bin/activate" ]]; then
	virtualenv --python="$python_binary" "$env_directory" > /dev/null 2>&1
	virtualenv_exit=$?
	if [[ virtualenv_exit -ne 0 ]]; then
		echo "[ERROR] ($script) - Failed to create virtual environment (Err:$virtualenv_exit)."
		exit 6
	fi
fi

# shellcheck source=/dev/null
source "$env_directory/bin/activate"
source_exit=$?
if [[ source_exit -ne 0 ]]; then
	echo "[ERROR] ($script) - Failed to activate virtual environment (Err:$source_exit)."
	exit 7
fi

if ! python3 --version | grep -q "Python 3.8.13"; then
	echo "[ERROR] ($script) - Using $(python3 --version), should be using Python 3.8.13."
	exit 8
fi

if ! pip list --disable-pip-version-check | grep numpy | grep -q 1.23.5; then
	pip install numpy==1.23.5 --disable-pip-version-check
	pip_install_exit=$?
	if [[ $pip_install_exit -ne 0 ]]; then
		echo "[ERROR] ($script) - Failed to install numpy 1.23.5 (Err:$pip_install_exit)"
		exit 9
	fi
fi

if ! pip list --disable-pip-version-check | grep networkx | grep -q 2.3; then
	pip install networkx==2.3 --disable-pip-version-check
	pip_install_exit=$?
	if [[ $pip_install_exit -ne 0 ]]; then
		echo "[ERROR] ($script) - Failed to install networkx 2.3 (Err:$pip_install_exit)"
		exit 10
	fi
fi

if ! pip list --disable-pip-version-check | grep androguard | grep -q 3.3.5; then
pip install androguard==3.3.5 --disable-pip-version-check
	pip_install_exit=$?
	if [[ $pip_install_exit -ne 0 ]]; then
		echo "[ERROR] ($script) - Failed to install androguard 3.3.5 (Err:$pip_install_exit)"
		exit 11
	fi
fi

echo "[INFO] ($script) - Python virtual environment created successfully."
