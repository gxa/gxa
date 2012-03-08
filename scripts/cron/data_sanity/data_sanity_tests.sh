#!/bin/bash
# @author: rpetry
# @date:   16 Jan 2012

if [ $# -lt 5 ]; then
        echo "Usage: $0 NCDF_DIR CURAPI_USERNAME CURAPI_PASSWORD NOTIFICATION_EMAILADDRESS ATLAS_URL"
        exit;
fi

NCDF_DIR=$1
CURAPI_USERNAME=$2
CURAPI_PASSWORD=$3
NOTIFICATION_EMAILADDRESS=$4
ATLAS_URL=$5

log="/tmp/data_sanity_tests."`eval date +%Y%m%d`".log"

rm -rf $log

./find_no_stats_exps.sh $NCDF_DIR >> $log

./find_unused_properties_values.sh $ATLAS_URL $CURAPI_USERNAME $CURAPI_PASSWORD >> $log

./find_near_duplicate_properties.sh $ATLAS_URL $CURAPI_USERNAME $CURAPI_PASSWORD >> $log

if [ -e "$log" ]; then
    mailx -s "[gxa/cron] Atlas Data Sanity Tests for: "`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log
fi