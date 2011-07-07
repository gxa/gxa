#!/bin/bash
#
# rdeploy-routines.sh

log() {
    echo "[`date`] $instance - $1" | tee -a $audit_log
}

# kills a tomcat identified by $atlas_instance
kill_tomcat() {
    log "Killing tomcats:"
    ps -Af | grep -v grep | grep tomcat | grep ${atlas_instance}                                     | tee -a $audit_log
    for thepin in `ps -Af | grep -v grep | grep tomcat | grep ${atlas_instance} | awk '{ print $2 }'`
    do
        log "Killing ${thepin}"
        kill -9 ${thepin}
    done
}

start_tomcat() {
    log "Special magic for shared NFS"
    umask 0000
    log "Starting ${TOMCAT_HOME}"
    pushd ${TOMCAT_HOME}
    ${start_tomcat}
    popd
}

update_config() {
    log "Linking to ${archive}"
    export WAR_FILE=${war_storage}/${archive}
    awk '{while(match($0,"[$]{[^}]*}")) {var=substr($0,RSTART+2,RLENGTH -3);gsub("[$]{"var"}",ENVIRON[var])}}1' < context.xml > ${TOMCAT_HOME}/conf/Catalina/localhost/${context}.xml
}

register_archive() {
    log "Registering ${archive} at ${war_storage}"
    cp ${archive} ${war_storage}/${archive}
}
