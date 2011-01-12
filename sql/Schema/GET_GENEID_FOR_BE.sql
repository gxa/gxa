create or replace
FUNCTION GET_GENEID_FOR_BE (
   BEID NUMBER)
RETURN NUMBER deterministic

AS

  r NUMBER(22,0) := -1;

BEGIN
  select be.bioentityid into r
  from TABLE(GET_BE_CONNECTIONS(beid)) con
  join A2_BIOENTITY be on be.bioentityid  = con.CONNENCTEDBEID
  join A2_BIOENTITYTYPE bet on bet.bioentitytypeid = be.bioentitytypeid
  where bet.id_for_index = '1';


  RETURN r;

END GET_GENEID_FOR_BE;