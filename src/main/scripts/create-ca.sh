#!/bin/bash

set -euo pipefail

DATA_DIR=$(dirname $0)/../../../data
mkdir -pv $DATA_DIR
JKS_FILENAME=$DATA_DIR/ca.p12
JKS_PASSWORD=changeit
DNAME="CN=Test CA, OU=ESY, O=Nordix Foundation, L=Athlone, C=IE"

keytool -v -keystore $JKS_FILENAME -storepass $JKS_PASSWORD -storetype pkcs12 \
    -genkeypair -alias ca -keyalg rsa -keysize 2048 \
    -dname "$DNAME" \
    -validity 365242 -ext BC=ca:true -ext KU=keyCertSign,cRLSign
