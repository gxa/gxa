#!/bin/bash

if [ $# -ne 5 ]; then
	echo "Usage: $0 SRC_ATLAS_CONNECTION TARGET_ATLAS_CONNECTION PREV_DATA_RELEASE ADMIN_PWD ERROR_NOTIFICATION_EMAILADDRESS"
	exit;
fi

SRC_ATLAS_CONNECTION=$1
TARGET_ATLAS_CONNECTION=$2
PREV_DATA_RELEASE=$3
ADMIN_PWD=$4
ERROR_NOTIFICATION_EMAILADDRESS=$5

MICROARRAY_HOME=/ebi/microarray/home
RELEASES_HOME=${MICROARRAY_HOME}/atlas-production/releases
TARGET_ATLAS_USER=`echo ${TARGET_ATLAS_CONNECTION} | awk -F'/' '{print $1}'`
DATA_RELEASE=`date +'%y.%m'`

log="/tmp/release_db."`eval date +%Y%m%d`".log"

echo "Beginning processing..."  > $log
cd /nfs/ma/home/gxa/release/gxa/sql
pushd flat-array-src
./make_flat_array.sh
cp flat_array ../
cp flat_array.pc ../
popd

echo "Packaging source DB..."  >> $log
./package_db.sh ${SRC_ATLAS_CONNECTION} atlas-data-relcan  2>&1 > atlas-data-relcan-package.log &
echo "Finished packaging source DB - log:" >> $log
cat atlas-data-relcan-package.log >> $log
mv atlas-data-relcan-package.log atlas-data-relcan

echo "Installing into target DB..."  >> $log
cd atlas-data-relcan
chmod 744 install.sh
./install.sh ${TARGET_ATLAS_CONNECTION} ${TARGET_ATLAS_USER}_DATA Data 2>&1 > atlas-data-relcan-install.log &
echo "Finished installing into target DB - errors  (if any):" >> $log
grep error install.log  | egrep -v '0 Rows not loaded due to data errors' >> $log
grep ERROR install.log >> $log
grep -ir error atlas-data-relcan-install.log >> >> $log
echo "Installation into target DB log :" >> $log
cat error atlas-data-relcan-install.log >> $log

export DATA_RELEASE=${DATA_RELEASE}
export PREV_DATA_RELEASE=${PREV_DATA_RELEASE}
export ADMIN_PWD=${ADMIN_PWD}
awk '{while(match($0,"[$]{[^}]*}")) {var=substr($0,RSTART+2,RLENGTH -3);gsub("[$]{"var"}",ENVIRON[var])}}1' < default_db_config.sql.template > default_db_config.sql

echo "Installing into target DB the following default DB config:"  >> $log
cat default_db_config.sql >> $log
 sqlplus -L -S ${TARGET_ATLAS_CONNECTION} @default_db_config.sql
rm -rf default_db_config.sql
echo "Installed the default DB config successfully"  >> $log

echo "Removing local data release directory"  >> $log
cd ..
cp -r atlas-data-relcan $RELEASES_HOME
rm -rf atlas-data-relcan
echo "Removed local data release directory successfully"  >> $log

# Send error notification email
err_msg="Release DB status for: "`date +'%d-%m-%Y'`
mailx -s "$err_msg" ${ERROR_NOTIFICATION_EMAILADDRESS} < $log

echo "Done"  >> $log