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
SAMPLEPVONTOLOGYID|| chr(9) ||               
ONTOLOGYTERMID|| chr(9) ||                 
SAMPLEPVID
from A2_SAMPLEPVONTOLOGY

/
quit;
