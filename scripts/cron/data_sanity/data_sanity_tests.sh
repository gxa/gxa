#!/bin/bash
# @author: rpetry
# @date:   16 Jan 2012

if [ $# -lt 2 ]; then
        echo "Usage: $0 NCDF_DIR NOTIFICATION_EMAILADDRESS"
        exit;
fi

NCDF_DIR=$1
NOTIFICATION_EMAILADDRESS=$2

log="/tmp/data_sanity_tests."`eval date +%Y%m%d`".log"

./find_no_stats_exps.sh $NCDF_DIR > $log

mailx -s "[gxa/cron] Atlas Data Sanity Tests for: "`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log