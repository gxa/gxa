#!/bin/bash
#
# deploy.sh

if [ $# -ne 1 ]
then
  echo "Usage: `basename $0` {instance_name}"
  exit -1
fi

source config-$1.sh
source deploy-routines.sh

upload_archive $1

do_deploy