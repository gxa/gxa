#!/bin/bash
# This script drops <USER NAME> from Oracle instance <ORACLE_SID>
# To re-create the user use create_user.sh script
# Authors rpetry/rmani 30 march 2011 

if [ $# != 2 ]; then
   echo "Syntax: create_user <USER NAME> <ADMIN_USER>/<ADMIN_PWD>@<ORACLE_SID>"
   exit
fi
 
# Set arguments
usr_name=$1
connect_string=$2

sqlplus /nolog  <<EOF
connect $connect_string
drop user $usr_name cascade
EOF
