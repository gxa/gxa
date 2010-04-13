#!/bin/bash
# Migrating from AEW to ATLAS2

source migration-routines.sh

if [ $# -ne 3 ]; then
        echo "Usage: $0 AEW_CONNECTION ATLAS_CONNECTION ATLAS_INDEX_TABLESPACE"
        exit;
fi

AEW_CONNECTION=$1
ATLAS_CONNECTION=$2
ATLAS_INDEX_TABLESPACE=$3

DATA_FOLDER=Data
CTL_FOLDER=Ctl

if [ -d $DATA_FOLDER ]; then
	echo "$DATA_FOLDER directory already exists, will assume data export is already in it!"
else
    mkdir $DATA_FOLDER
    echo "Exporting data for migration..."
    export_data_for_migration $AEW_CONNECTION $DATA_FOLDER
fi

echo "Dropping all objects..."
sqlplus -S $ATLAS_CONNECTION @drop_all.sql &> drop_all.log
sqlplus -S $ATLAS_CONNECTION @drop_all.sql &> drop_all.log
sqlplus -S $ATLAS_CONNECTION @drop_all.sql &> drop_all.log
sqlplus -S $ATLAS_CONNECTION @drop_all.sql &> drop_all.log

echo "Creating schema..."
create_schema $ATLAS_CONNECTION $ATLAS_INDEX_TABLESPACE

echo "Loading data..."
load_data $ATLAS_CONNECTION $DATA_FOLDER $CTL_FOLDER

echo "Migration complete. Please migrate NetCDFs separately."
