#!/bin/bash
# @author: rpetry
# @date:   31 Jul 2013
# This script bounces $HOSTNAME if response time exceeds allowed maximum

if [ $# -lt 2 ]; then
        echo "Usage: $0 HOSTNAME MAXTIME"
        exit;
fi

HOSTNAME=$1
MAXTIME=$2
ATLAS_NAGIOS_URL="http://${HOSTNAME}:8080/gxa/das/s4/features?segment=ENSG00000139618"

today="`eval date +%Y-%m-%d`"
log="/tmp/gxa_bounces.$today"

# Nagios query
responseTime=`curl -o /dev/null -X GET -s --max-time $MAXTIME -w %{time_total} "$ATLAS_NAGIOS_URL" | awk -F"." '{print $1}'`
echo "responseTime = $responseTime"
if [ "$responseTime" -ge "$MAXTIME" ]; then
    touch $log
    echo `eval date +%k:%M:%S`" - bouncing $HOSTNAME ..." >> $log
    ssh `whoami`@${HOSTNAME} /nfs/public/rw/webadmin/tomcat/bases/fg/tc-fg-gxa/bin/stop
    ssh `whoami`@${HOSTNAME} /nfs/public/rw/webadmin/tomcat/bases/fg/tc-fg-gxa/bin/start
    curl -o /dev/null -s -I 'http://${HOSTNAME}:8080/gxa/fval?q=brca&factor=&type=gene&limit=15'
    curl -o /dev/null -s -I 'http://${HOSTNAME}:8080/gxa/fval?q=canc&factor=&type=efoefv&limit=15'
fi

