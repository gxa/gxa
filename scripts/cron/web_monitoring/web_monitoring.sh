#!/bin/bash
# @author: rpetry
# @date:   26 March 2012

if [ $# -lt 1 ]; then
        echo "Usage: $0 [pg|oy] NOTIFICATION_EMAILADDRESS"
        exit;
fi

DATACENTRE=$1
NOTIFICATION_EMAILADDRESS=$2

log="/tmp/web_monitoring."`eval date +%Y%m%d`".log"

for hostname in ves-${DATACENTRE}-74 ves-${DATACENTRE}-75; do
  ./web_response_times.sh $hostname > $log
  ./find_web_errors.sh $hostname >> $log

  if [ -e "$log" ]; then
      mailx -s "[gxa/cron] $hostname: Atlas Web Monitoring for "`date +'%d-%m-%Y'` ${NOTIFICATION_EMAILADDRESS} < $log
  fi
done
