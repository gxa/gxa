SET HEAD OFF
set newp none
set recsep off
set echo off
set serveroutput off
set tab on
set line 500
set wrap off
SET FEEDBACK OFF 
SET VERIFY OFF 

Select 
GENEPROPERTYID || chr(9) ||                 
NAME || chr(9) ||                           
AE2TABLENAME || chr(9) ||
IDENTIFIERPRIORITY     
from A2_GENEPROPERTY

/
quit;
