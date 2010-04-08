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
ASSAYPVONTOLOGYID || chr(9) ||                
ONTOLOGYTERMID || chr(9) ||                 
ASSAYPVID 
from A2_ASSAYPVONTOLOGY

/
quit;
