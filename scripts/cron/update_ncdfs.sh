#!/bin/bash
# @author: rpetry
# @date:   14 Oct 2011

# This script serves to update ncdfs for all experiments that need it on server ATLAS_URL:ATLAS_PORT/ATLAS_ROOT. using admin credentials: ADMIN_USERNAME/ADMIN_PASSWORD

if [ $# -eq 0 ]; then
        echo "Usage: $0 ADMIN_USERNAME ADMIN_PASSWORD ATLAS_URL ATLAS_PORT ATLASROOT"
        echo "e.g. $0 admin <pwd> lime 14032 gxa-load"
        exit;
fi

process_data="$3.$4.`eval date +%Y%m%d`"
process_file="/tmp/process_experiments.$process_data"
authentication_cookie=$process_file.$$

# login to Atlas admin
curl -X GET -c $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=login&userName=$1&password=$2&indent" >> /dev/null

echo `eval date +%H:%M:%S`": The following experiments will have their ncdfs updated: "  >> $process_file.log
# update ncdfs
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=schedulesearchexp&runMode=RESTART&type=updateexperiment&pendingOnly=INCOMPLETE_NETCDF&autoDepends=false&indent" 2>&1 >> $process_file.log

# logout from Atlas admin
curl -X GET -b $authentication_cookie -H "Accept: application/json" "http://$3:$4/$5/admin?op=logout&indent"  >> /dev/null
rm -rf $authentication_cookie


