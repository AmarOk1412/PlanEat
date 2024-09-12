FROM tensorflow/tensorflow:2.13.0-gpu

RUN apt-get update -y
RUN apt-get upgrade -y
RUN apt-get -y install libsndfile1
RUN apt-get -y install libusb-1.0-0-dev
RUN rm -rf /var/lib/apt/lists/*
RUN pip install "numpy>=1.17.3,<1.23.4"
RUN pip install "scann==1.2.6"
RUN pip install wai.pycocotools
RUN pip install "tflite-model-maker>=0.4.3,<0.5.0" "tensorflow>=2.11.0,<2.14.0" pyyaml opex python-image-complete redis
RUN rm -Rf /root/.cache/pip
RUN rm -rf /var/lib/apt/lists/*

# https://stackoverflow.com/questions/59290386/runtimeerror-at-cannot-cache-function-shear-dense-no-locator-available-fo
ENV NUMBA_CACHE_DIR=/tmp

WORKDIR /