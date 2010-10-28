/*******************************************************************************
-- uncurated
*******************************************************************************/
create or replace procedure CUR_SetUnCurated(
 Accession varchar2)
as
  TheExperimentID INTEGER;
begin

 BEGIN
    Select ExperimentID into CUR_SetUnCurated.TheExperimentID
    from a2_experiment 
    where Accession = CUR_SetUnCurated.Accession;
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
  ,'uncurated'
  ,null
  ,sysdate
  ,USER
from dual;

COMMIT;
end;
/
exit;
/