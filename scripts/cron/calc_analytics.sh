#!/bin/bash
# @author: rpetry
# @date:   14 Oct 2011

# This script serves to re-calculate analytics for all experiments that need it on server ATLAS_URL:ATLAS_PORT/ATLAS_ROOT. using admin credentials: ADMIN_USERNAME/ADMIN_PASSWORD
# It assumes that update ncdfs step has been run before and was successful. If it finds any experiments with ncdfs needing updating it emails an error report to ERROR_NOTIFICATION_EMAILADDRESS and quits

if [ $# -eq 0 ]; then
        echo "Usage: $0 ADMIN_USERNAME ADMIN_PASSWORD ATLAS_URL ATLAS_PORT ATLAS_ROOT ERROR_NOTIFICATION_EMAILADDRESS"
        echo "e.g. $0 admin <pwd> lime 14032 gxa-load atlas-developers@ebi.ac.uk"
        exit;
fi

ADMIN_USERNAME=$1
ADMIN_PASSWORD=$2
ATLAS_URL=$3
ATLAS_PORT=$4
ATLAS_ROOT=$5
ERROR_NOTIFICATION_EMAILADDRESS=$6

process_data="${ATLAS_URL}.${ATLAS_PORT}.`eval date +%Y%m%d`"
process_file="/tmp/process_experiments.$process_data"
authentication_cookie=$process_file.$$

# login to Atlas admin
curl -X GET -c $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=login&userName=${ADMIN_USERNAME}&password=${ADMIN_PASSWORD}&indent" >> /dev/null

# cut commands extract e.g. 0 from {"numTotal":0,
num_incomplete_ncdfs=`curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=searchexp&pendingOnly=INCOMPLETE_NETCDF" | cut -d'{' -f2 | cut -d':' -f2 | cut -d',' -f1`

if [ $num_incomplete_ncdfs != "0" ]; then
# If the number of ncdfs that need updating is not 0, email an error to ERROR_NOTIFICATION_EMAILADDRESS and quit
    curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=searchexp&pendingOnly=INCOMPLETE_NETCDF&indent" 2>&1 >> $process_file.log
    mailx -s "[gxa/cron] Processing experiments on ${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT} failed due to incomplete ncdf updates (see message body)" ${ERROR_NOTIFICATION_EMAILADDRESS} < $process_file.log
else
    echo `eval date +%H:%M:%S`": The following experiments will have their analytics calculated: "  >> $process_file.log
    # calculate analytics
    curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=schedulesearchexp&runMode=RESTART&type=analytics&pendingOnly=INCOMPLETE_ANALYTICS&autoDepends=false&indent" 2>&1 >> $process_file.log
fi

# logout from Atlas admin
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://${ATLAS_URL}:${ATLAS_PORT}/${ATLAS_ROOT}/admin?op=logout&indent" >> /dev/null
rm -rf $authentication_cookie