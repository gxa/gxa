#!/bin/bash
#
# rdeploy.sh

source config.sh
source rdeploy-routines.sh

log "Starting deployment"

register_archive

kill_tomcat

update_config

start_tomcat

log "Finishing deployment"

