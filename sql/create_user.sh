#!/bin/csh
# This script creates a user <USER NAME> with the necessary permissions to install Atlas on Oracle instance <ORACLE_SID>. 
# The default tablespace is <TABLESPACE NAME>_DATA.
# Default password is the same as <USER NAME>.
# To drop user use script: drop_user.sh
# Authors: rpetry/rmani 30 March 2011

if ( $#argv != 5 ) then
   echo
   echo "Syntax: create_user <USER NAME> <TABLESPACE NAME> <ADMIN_USER> <ADMIN_PWD> <ORACLE_SID>"
   echo "Tablespace Names can be atlas2, atlas2test or atlas2dev"
   echo
   exit
endif
 
# Set arguments
set usr_name = ${1}
set tbs_name = ${2}
set admin_usr = ${3}
set admin_pwd = ${4}
set oracle_sid = ${5}
 
sqlplus /nolog <<EOF
connect ${admin_usr}/${admin_pwd}@${oracle_sid};
CREATE USER ${usr_name}
  IDENTIFIED BY ${usr_name}
  DEFAULT TABLESPACE ${tbs_name}_DATA
  TEMPORARY TABLESPACE TEMP
  PROFILE DEFAULT
  ACCOUNT UNLOCK;
  -- 2 Roles for user
  GRANT RESOURCE TO ${usr_name};
  GRANT CONNECT TO ${usr_name};
  ALTER USER ${usr_name} DEFAULT ROLE ALL;
  -- 1 System Privilege for user
  GRANT CREATE VIEW TO ${usr_name};
  -- 2 Tablespace Quotas for user
  ALTER USER ${usr_name} QUOTA UNLIMITED ON ${tbs_name}_DATA;
  ALTER USER ${usr_name} QUOTA UNLIMITED ON ${tbs_name}_INDX;
EOF
