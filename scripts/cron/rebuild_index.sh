#!/bin/bash
# @author: rpetry
# @date:   14 Oct 2011

# This script serves to re-build index for all experiments that need it on server ATLAS_URL:ATLAS_PORT/ATLAS_ROOT. using admin credentials: ADMIN_USERNAME/ADMIN_PASSWORD
# It assumes that calculate analytics step has been run before and was successful. If it finds any experiments with incomplete analytics it emails an error report to ERROR_NOTIFICATION_EMAILADDRESS and quits

if [ $# -eq 0 ]; then
        echo "Usage: $0 ADMIN_USERNAME ADMIN_PASSWORD ATLAS_URL ATLAS_PORT ATLAS_ROOT ERROR_NOTIFICATION_EMAILADDRESS"
        echo "e.g. $0 admin <pwd> lime 14032 gxa-load atlas-developers@ebi.ac.uk"
        exit;
fi

process_data="$3.$4.`eval date +%Y%m%d`"
process_file="/tmp/process_experiments.$process_data"
authentication_cookie=$process_file.$$

# login to Atlas admin
curl -X POST -c $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=login&userName=$1&password=$2&indent" >> /dev/null

# cut commands extract e.g. 0 from {"numTotal":0,
num_incomplete_analytics=`curl -X POST -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=searchexp&pendingOnly=INCOMPLETE_ANALYTICS" | cut -d'{' -f2 | cut -d':' -f2 | cut -d',' -f1`

if [ $num_incomplete_analytics != "0" ]; then
    # If the number of experiments with incomplete analytics is not 0, email an error to ERROR_NOTIFICATION_EMAILADDRESS and quit
    curl -X POST -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=searchexp&pendingOnly=INCOMPLETE_ANALYTICS&indent" 2>&1 >> $process_file.log
    mailx -s `eval date +%HH%MM%ss`" Processing experiments on $3:$4/$5 failed due to incomplete analytics (see message body)" $6 < $process_file.log
else
    # re-build index
    curl -X POST -b $authentication_cookie -H "Accept: application/json" 'http://$3:$4/$5/admin?op=schedule&runMode=RESTART&type=index&autoDepends=false&indent' 2>&1 >> $process_file.log
fi

# logout from Atlas admin
curl -X POST -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=logout&indent" >> /dev/null

rm -rf $authentication_cookie

