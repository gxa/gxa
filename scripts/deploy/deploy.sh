#!/bin/bash
#
# deploy.sh

source config-$1.sh
source deploy-routines.sh

upload_archive $1

do_deploy