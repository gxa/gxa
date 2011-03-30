#!/bin/csh

# This script drops <USER NAME> from Oracle instance <ORACLE_SID>
# To re-create the user use create_user.sh script
# Authors rpetry/rmani 30 march 2011 
if ( $#argv != 4 ) then
   echo
   echo "Syntax: create_user <USER NAME> <ADMIN_USER> <ADMIN_PWD> <ORACLE_SID>"
   echo
   exit
endif
 
# Set arguments
 
set usr_name = ${1}
set admin_usr = ${2}
set admin_pwd = ${3}
set oracle_sid = ${4}
 
sqlplus /nolog <<EOF
connect ${admin_usr}/${admin_pwd}@${oracle_sid};
drop user ${usr_name} cascade
EOF
