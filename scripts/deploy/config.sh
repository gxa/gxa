instance=TODO

# connection details
ssh_host=TODO
sudo_as=TODO
temp_dir=TODO

# WAR storage
war_storage=TODO

# tomcat details
atlas_instance=TODO
TOMCAT_HOME=TODO
start_tomcat=bin/startup.sh
audit_log=${TOMCAT_HOME}/logs/deploy.log
context=gxa

# context.xml variables
# (exporting to allow substitution work)
export ATLAS_JDBC_URL=TODO
export ATLAS_JDBC_USERNAME=TODO
export ATLAS_JDBC_PASSWORD=TODO
export ATLAS_INDEX=TODO
export ATLAS_NETCDF=TODO
export ATLAS_EFO_URI=TODO
export ATLAS_UNPACK_WAR=TODO

# our small bash_rc
export JAVA_HOME=TODO
export PATH=$JAVA_HOME/bin:$PATH:TODO
export http_proxy=TODO