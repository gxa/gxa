#!/bin/bash
#
# deploy.sh

source config.sh
source deploy-routines.sh

log "Starting deployment"

register_archive

kill_tomcat

update_config

start_tomcat

log "Finishing deployment"

