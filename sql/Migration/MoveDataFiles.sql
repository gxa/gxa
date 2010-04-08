select * from v$tablespace
select * from v$datafiles


drop tablespace ATLAS2 INCLUDING CONTENTS
drop tablespace ATLAS2_INDX INCLUDING CONTENTS

SELECT file_name, tablespace_name,
       bytes/1024/1024 MB, blocks
FROM dba_data_files
UNION ALL
SELECT file_name, tablespace_name,
       bytes/1024/1024 MB, blocks
FROM dba_temp_files
ORDER BY tablespace_name, file_name;

select tablespace_name from dba_data_files
USERS                          
UNDOTBS1                       
SYSAUX                         
SYSTEM 

select tablespace_name from dba_temp_files
TEMP

ALTER TABLESPACE INDX OFFLINE ;

ALTER TABLESPACE USERS OFFLINE;
ALTER TABLESPACE TEMP TEMPFILE OFFLINE;
ALTER TABLESPACE UNDOTBS1 OFFLINE;
ALTER TABLESPACE SYSAUX OFFLINE;
ALTER TABLESPACE SYSTEM OFFLINE;

ALTER TABLESPACE USERS RENAME DATAFILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\USERS01.DBF' TO 'E:\ORADATA\USERS01.DBF';

ALTER DATABASE RENAME FILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\USERS01.DBF' TO 'E:\ORADATA\WINN\USERS01.DBF';
ALTER DATABASE RENAME FILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\UNDOTBS01.DBF' TO 'E:\ORADATA\WINN\UNDOTBS01.DBF';
ALTER DATABASE RENAME FILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\TEMP01.DBF' TO 'E:\ORADATA\WINN\TEMP01.DBF';
ALTER DATABASE RENAME FILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\SYSTEM01.DBF' TO 'E:\ORADATA\WINN\SYSTEM01.DBF';
ALTER DATABASE RENAME FILE 'C:\APP\ADMINISTRATOR\ORADATA\WINN\SYSAUX01.DBF' TO 'E:\ORADATA\WINN\SYSAUX01.DBF';

 alter tablespace USERS
  add datafile 'E:\ORADATA\WIN2\USERS02.dbf' size 30G autoextend off;
 alter tablespace USERS
  add datafile 'E:\ORADATA\WIN2\USERS03.dbf' size 30G autoextend off;

create tablespace ATLAS2 NOLOGGING DATAFILE 'E:\ORADATA\WINN\ATLAS2.dbf' SIZE 60g AUTOEXTEND ON;

create undo tablespace UNDOTBS2 datafile 'E:\ORADATA\WINN\UNDOTBS02.DBF' SIZE 2000m;
alter system set undo_tablespace= undotbs2 ;
drop tablespace UNDOTBS1 including contents and datafiles;

create temporary tablespace TEMP1 tempfile 'E:\ORADATA\WINN\TEMP02.DBF' SIZE 64m REUSE AUTOEXTEND ON NEXT 28 MAXSIZE UNLIMITED;
ALTER DATABASE DEFAULT TEMPORARY TABLESPACE TEMP1;
DROP TABLESPACE temp INCLUDING CONTENTS; AND DATAFILES;


alter system set undo_tablespace= undotbs2 ;
drop tablespace UNDOTBS1 including contents and datafiles;

size <n>;



C:\APP\ADMINISTRATOR\ORADATA\WINN\SYSAUX01.DBF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    SYSAUX                         854.25                 109344                 
C:\APP\ADMINISTRATOR\ORADATA\WINN\SYSTEM01.DBF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    SYSTEM                         700                    89600                  
C:\APP\ADMINISTRATOR\ORADATA\WINN\TEMP01.DBF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      TEMP                           1922                   246016                 
C:\APP\ADMINISTRATOR\ORADATA\WINN\UNDOTBS01.DBF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   UNDOTBS1                       2210                   282880                 
C:\APP\ADMINISTRATOR\ORADATA\WINN\USERS01.DBF 

create tablespace t1;
drop tablespace t1;
ALTER SYSTEM SET DB_CREATE_FILE_DEST = 'E:\ORADATA'

