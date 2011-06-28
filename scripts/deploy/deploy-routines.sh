#!/bin/bash
#
# deploy-routines.sh

log() {
    echo "[`date`] $instance - $1\n"
}

upload_archive() {
    archive=`git describe`.war
    remote_dir=${ssh_host}:${temp_dir}
    log "Uploading webapp"
    scp ../../atlas-web/target/atlas-web-*.war ${remote_dir}/${archive}
    log "Uploading config sample"
    scp ./context.xml ${remote_dir}
    log "Uploading scripts"
    scp ./rdeploy-routines.sh ${remote_dir}
    scp ./rdeploy.sh ${remote_dir}
    scp ./config-$1.sh ${remote_dir}/config.sh
    log "Upload done"
}

do_deploy() {
    log "Deploying..."
    ssh ${ssh_host} sudo -u ${sudo_as} archive=${archive} "sh ${temp_dir}/rdeploy.sh"
    log "Done!"
}