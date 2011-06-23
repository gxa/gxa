#!/bin/bash
#
# deploy-routines.sh

log() {
	echo "[`date`] $instance - $1\n" >> $audit_log
}

# kills a tomcat identfied by $atlas_instance
kill_tomcat() {
	for thepin in `ps -Af | grep -v grep | grep tomcat | grep ${atlas_instance} | awk '{ print $2 }'`
	do
		log "Killing ${thepin}"
		kill -9 ${thepin}
	done
}

start_tomcat() {
	log "Starting ${TOMCAT_HOME}"
	$TOMCAT_HOME/$start_tomcat
}

update_config() {
	log "Linking to ${archive}"
	export WAR_FILE=${war_storage}/${archive}
	while read -r line; do eval echo "$line"; done < context.xml > ${TOMCAT_HOME}/conf/Catalina/localhost/${context}.xml
}

register_archive() {
	log "Registering ${archive} at ${war_storage}"
	cp ${archive} ${war_storage}/${archive}
}