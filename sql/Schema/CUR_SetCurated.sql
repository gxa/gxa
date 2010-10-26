/*******************************************************************************
mark experiment as curated
call CUR_SetCurated('E-MTAB-62','finally finished, pfffffffff')
*****************************************************************************/
create or replace procedure CUR_SetCurated(
 Accession varchar2
,Message varchar2)
as
  TheExperimentID INTEGER;
begin

 BEGIN
    Select ExperimentID into CUR_SetCurated.TheExperimentID
    from a2_experiment 
    where Accession = CUR_SetCurated.Accession;
 EXCEPTION
 WHEN NO_DATA_FOUND THEN
  RAISE_APPLICATION_ERROR(-20001, 'experiment not found');
 END;

Insert Into A2_EXPERIMENTLOG(
 EXPERIMENTID
, TYPE 
, MESSAGE 
, TIME 
, USERNAME)
select TheExperimentID
  ,'curated'
  ,CUR_SetCurated.Message
  ,sysdate
  ,USER
from dual;

COMMIT;
end;
/

