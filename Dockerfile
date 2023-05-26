# syntax=docker/dockerfile:1
   
FROM budtmo/docker-android-x86-11.0

RUN apt-get -y update
RUN apt-get -y install python3.8
RUN apt-get -y install python3-pip

RUN pip3 install virtualenv
RUN virtualenv --python="/usr/bin/python3.8" "python_venv"
ENV PATH="python_venv/bin:$PATH"

RUN pip3 install numpy==1.23.5
RUN pip3 install networkx==2.3
RUN pip3 install androguard==3.3.5

RUN pip3 install matplotlib
RUN pip3 install pandas
RUN pip3 install tikzplotlib