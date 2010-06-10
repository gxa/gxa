#!/bin/bash
# Installation of packaged data release

source install-routines.sh

################################## start ##############################

if [ $# -eq 0 ]; then
        echo "Usage: $0 ATLAS_CONNECTION [ATLAS_INDEX_TABLESPACE] [Schema|Data]"
        exit;
fi

ATLAS_CONNECTION=$1
ATLAS_INDEX_TABLESPACE=$2
INSTALL_MODE=$3

if [ -z "$INSTALL_MODE" ]; then
    INSTALL_MODE=Data
fi

echo "Dropping all objects..."
sqlplus -S $ATLAS_CONNECTION @drop_all.sql &> drop_all.log

echo "Creating schema..."
create_schema $ATLAS_CONNECTION $ATLAS_INDEX_TABLESPACE

echo "Loading data..."
load_data $ATLAS_CONNECTION Data Data $INSTALL_MODE

echo "Installation complete. Please install the NetCDFs separately."
