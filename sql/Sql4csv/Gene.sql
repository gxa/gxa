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

Select GENEID || chr(9) || ORGANISMID || chr(9) || IDENTIFIER || chr(9) || NAME 
from A2_GENE

/
quit;
