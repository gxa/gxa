create or replace
PACKAGE ATLASBELDR AS

  PROCEDURE A2_BIOENTITYSET (
   Typename varchar2
  ,Organism varchar2
  );

  PROCEDURE A2_BIOENTITYSETBEGIN;

  PROCEDURE A2_BIOENTITYSETEND;

END ATLASBELDR;
/
/*******************************************************************************
BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY
*******************************************************************************/
CREATE OR REPLACE
PACKAGE BODY ATLASBELDR AS

  PROCEDURE A2_BIOENTITYSET (
   Typename varchar2
  ,Organism varchar2
  ) AS

  OrganismID int := 0;

  CURSOR be_cur IS
     SELECT distinct accession
     from TMP_BIOENTITY;

  BEID int := 0;
  cnt int := 0;
  v_sysdate timestamp;
  q varchar2(2000);

 begin
  begin
      select o.organismid into OrganismID
      from A2_ORGANISM o
      where o.name = Organism;

  exception
     when NO_DATA_FOUND then
     begin
      select A2_ORGANISM_SEQ.nextval into OrganismID from dual;
      insert into A2_ORGANISM(organismid, Name)
      values (OrganismID, Organism);
     end;
    when others then
      RAISE;
  end;

  dbms_output.put_line('getting organism id '|| OrganismID);

  q:= 'CREATE INDEX tmp_bioentity_accessionm ON tmp_bioentity (accession)';
  EXECUTE IMMEDIATE q;

  select localtimestamp into v_sysdate from dual;
  DBMS_OUTPUT.PUT_LINE('insert into bioentity' || v_sysdate);

  INSERT INTO a2_bioentity (BIOENTITYID, IDENTIFIER, ORGANISMID, TYPE)
  SELECT A2_BIOENTITY_SEQ.nextval, tbe.accession, OrganismID, Typename
  FROM (SELECT distinct accession from TMP_BIOENTITY ) tbe
  WHERE NOT EXISTS (SELECT distinct be.BIOENTITYID
                   FROM a2_bioentity be
                   WHERE be.IDENTIFIER = tbe.accession);

  select localtimestamp into v_sysdate from dual;
  DBMS_OUTPUT.PUT_LINE('insert into bioentityproperty' || v_sysdate);

 INSERT INTO A2_BIOENTITYPROPERTY(BIOENTITYPROPERTYID, NAME)
  SELECT A2_BIOENTITYPROPERTY_SEQ.nextval, LOWER(tbe.name)
  FROM (
      SELECT distinct name from TMP_BIOENTITY
  ) tbe
  WHERE NOT EXISTS (SELECT p.BIOENTITYPROPERTYID
                   FROM A2_BIOENTITYPROPERTY p
                   WHERE p.name = LOWER(tbe.name));

 select localtimestamp into v_sysdate from dual;
    DBMS_OUTPUT.PUT_LINE('insert into bioentityproperty value' || v_sysdate);

  INSERT INTO A2_BIOENTITYPROPERTYVALUE(BEPROPERTYVALUEID, BIOENTITYPROPERTYID, VALUE)
  SELECT A2_BIOENTITYPROPERTY_SEQ.nextval, p1.BIOENTITYPROPERTYID, p1.value
  FROM
  (SELECT distinct RTRIM(LTRIM(tbe.value)) value, (select bioentitypropertyid
                      from A2_BIOENTITYPROPERTY p
                      where p.name = LOWER(tbe.name)
                      ) BIOENTITYPROPERTYID
  from TMP_BIOENTITY tbe) p1
  where not exists (SELECT 1
                   FROM  A2_BIOENTITYPROPERTYVALUE pv
                   WHERE pv.value = p1.value
                   AND pv.BIOENTITYPROPERTYID = p1.BIOENTITYPROPERTYID);

 select localtimestamp into v_sysdate from dual;
 DBMS_OUTPUT.PUT_LINE('start deleting ' || v_sysdate);

 FOR berec IN be_cur
  LOOP
        select be.bioentityid into BEID
        from A2_BIOENTITY be
        where be.identifier = berec.accession;

        delete from A2_BIOENTITYBEPV
        where bioentityid = BEID;
  END LOOP;

    select localtimestamp into v_sysdate from dual;
    DBMS_OUTPUT.PUT_LINE('writing be to bePV mapping' || v_sysdate);

  INSERT INTO A2_BIOENTITYBEPV (BIOENTITYBEPVID, BIOENTITYID, BEPROPERTYVALUEID)
  SELECT A2_BIOENTITYBEPV_SEQ.nextval, t.BIOENTITYID, t.BEPROPERTYVALUEID
  FROM
    (SELECT distinct be.BIOENTITYID BIOENTITYID, pv.BEPROPERTYVALUEID BEPROPERTYVALUEID
     FROM TMP_BIOENTITY tbe, a2_bioentity be, A2_BIOENTITYPROPERTYVALUE pv, a2_bioentityproperty p
     WHERE be.identifier = tbe.accession
     AND pv.value = tbe.value
     AND p.name = tbe.name
     AND pv.bioentitypropertyid = p.bioentitypropertyid
     ) t;


    select localtimestamp into v_sysdate from dual;
    DBMS_OUTPUT.PUT_LINE('DONE' || v_sysdate);

  COMMIT WORK;
  END A2_BIOENTITYSET;

  PROCEDURE A2_BIOENTITYSETBEGIN AS
  q varchar2(2000);

  BEGIN
   begin
    q:= 'drop table TMP_BIOENTITY';
    dbms_output.put_line(q);
    EXECUTE IMMEDIATE q;
   exception
    WHEN OTHERS THEN NULL;
   end;

   begin
    q:= 'CREATE TABLE "TMP_BIOENTITY" (accession varchar2(255) ,name varchar2(255) ,value varchar2(255))';
    dbms_output.put_line(q);
    EXECUTE IMMEDIATE q;
   exception
    WHEN OTHERS THEN NULL;
   end;

  END A2_BIOENTITYSETBEGIN;

  PROCEDURE A2_BIOENTITYSETEND AS
  BEGIN
    /* TODO implementation required */
    NULL;
  END A2_BIOENTITYSETEND;

END ATLASBELDR;
/
exit;
/
