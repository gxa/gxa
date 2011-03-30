#!/bin/bash
# This script creates a user <USER NAME> with the necessary permissions to install Atlas on Oracle instance <ORACLE_SID>.                  
# The default tablespace is <TABLESPACE NAME>_DATA.                                                                                                                # Default password is the same as <USER NAME>.                                                                                                                     # To drop user use script: drop_user.sh                                                                                                                            # Authors: rpetry/rmani 30 March 2011 
 
if [ $# != 3 ]; then
   echo "Usage: create_user <USER NAME> <TABLESPACE NAME> <ADMIN_USER>/<ADMIN_PWD>@<ORACLE_SID>"
   echo "Tablespace Names can be atlas2, atlas2test or atlas2dev"
   exit
fi
 
# Set arguments
 
usr_name=$1
tbs_name=$2
connect_string=$3
 
sqlplus /nolog <<EOF
connect ${connect_string};
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
