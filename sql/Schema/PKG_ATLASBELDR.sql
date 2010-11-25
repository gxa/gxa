create or replace
PACKAGE ATLASBELDR AS

  PROCEDURE A2_BIOENTITYSET (
   Typename varchar2
  ,Organism varchar2
  );

  PROCEDURE A2_BIOENTITYSETPREPARE;
    
  PROCEDURE A2_VIRTUALDESIGNSET (
    ADaccession varchar2
    ,ADname  varchar2
    ,Typename varchar2
    ,provider varchar2
    ,SWname varchar2
    ,SWversion varchar2
    ,DEtype varchar2
  );

    PROCEDURE A2_DESIGNELEMENTMAPPINGSET (
    ADaccession varchar2
    ,ADname  varchar2
    ,Typename varchar2
    ,provider varchar2
    ,SWname varchar2
    ,SWversion varchar2
    ,DEtype varchar2
  );

END ATLASBELDR;
/


/*******************************************************************************
BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY
*******************************************************************************/
CREATE OR replace PACKAGE BODY atlasbeldr
AS

/*
 Procedure to load bioentities and their properties from tmp_bioentity table into
 a2_bioentity, a2_bioentityproperty, a2_bioentitypropertyvalue and a2_bioentitybepv tables

*/
  PROCEDURE A2_bioentityset (typename VARCHAR2,
                             organism VARCHAR2)
  AS
    organismid INT := 0;
    CURSOR be_cur IS
      SELECT DISTINCT accession
      FROM   tmp_bioentity;
    beid       INT := 0;
    cnt        INT := 0;
    v_sysdate  TIMESTAMP;
    q          VARCHAR2(2000);
  BEGIN
    BEGIN
        SELECT o.organismid
        INTO   organismid
        FROM   a2_organism o
        WHERE  o.name = organism;
    EXCEPTION
        WHEN no_data_found THEN
          BEGIN
              SELECT a2_organism_seq.nextval
              INTO   organismid
              FROM   dual;

              INSERT INTO a2_organism
                          (organismid,
                           name)
              VALUES      (organismid,
                           organism);
          END;
        WHEN OTHERS THEN
          RAISE;
    END;

    dbms_output.Put_line('getting organism id '
                         || organismid);

    q := 'CREATE INDEX tmp_bioentity_accession ON tmp_bioentity (accession)';

    EXECUTE IMMEDIATE q;

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('insert into bioentity'
                         || v_sysdate);

    INSERT INTO a2_bioentity
                (bioentityid,
                 identifier,
                 organismid,
                 type)
    SELECT a2_bioentity_seq.nextval,
           tbe.accession,
           organismid,
           typename
    FROM   (SELECT DISTINCT accession
            FROM   tmp_bioentity) tbe
    WHERE  NOT EXISTS (SELECT DISTINCT be.bioentityid
                       FROM   a2_bioentity be
                       WHERE  be.identifier = tbe.accession);

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('insert into bioentityproperty'
                         || v_sysdate);

    INSERT INTO a2_bioentityproperty
                (bioentitypropertyid,
                 name)
    SELECT a2_bioentityproperty_seq.nextval,
           Lower(tbe.name)
    FROM   (SELECT DISTINCT name
            FROM   tmp_bioentity) tbe
    WHERE  NOT EXISTS (SELECT p.bioentitypropertyid
                       FROM   a2_bioentityproperty p
                       WHERE  p.name = Lower(tbe.name));

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('insert into bioentityproperty value'
                         || v_sysdate);

    INSERT INTO a2_bioentitypropertyvalue
                (bepropertyvalueid,
                 bioentitypropertyid,
                 value)
    SELECT a2_bioentityproperty_seq.nextval,
           p1.bioentitypropertyid,
           p1.value
    FROM   (SELECT DISTINCT Rtrim(Ltrim(tbe.value))           value,
                            (SELECT bioentitypropertyid
                             FROM   a2_bioentityproperty p
                             WHERE  p.name = Lower(tbe.name))
                            bioentitypropertyid
            FROM   tmp_bioentity tbe) p1
    WHERE  NOT EXISTS (SELECT 1
                       FROM   a2_bioentitypropertyvalue pv
                       WHERE  pv.value = p1.value
                              AND pv.bioentitypropertyid =
    p1.bioentitypropertyid);

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('start deleting '
                         || v_sysdate);

    FOR berec IN be_cur LOOP
        SELECT be.bioentityid
        INTO   beid
        FROM   a2_bioentity be
        WHERE  be.identifier = berec.accession;

        DELETE FROM a2_bioentitybepv
        WHERE  bioentityid = beid;
    END LOOP;

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('writing be to bePV mapping'
                         || v_sysdate);

    INSERT INTO a2_bioentitybepv
                (bioentitybepvid,
                 bioentityid,
                 bepropertyvalueid)
    SELECT a2_bioentitybepv_seq.nextval,
           t.bioentityid,
           t.bepropertyvalueid
    FROM   (SELECT DISTINCT be.bioentityid       bioentityid,
                            pv.bepropertyvalueid bepropertyvalueid
            FROM   tmp_bioentity tbe,
                   a2_bioentity be,
                   a2_bioentitypropertyvalue pv,
                   a2_bioentityproperty p
            WHERE  be.identifier = tbe.accession
                   AND pv.value = tbe.value
                   AND p.name = tbe.name
                   AND pv.bioentitypropertyid = p.bioentitypropertyid) t;

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('DONE'
                         || v_sysdate);

    COMMIT WORK;
  END a2_bioentityset;

  /* Procedure to initialize TMP_BIOENTITY table*/
  PROCEDURE A2_BIOENTITYSETPREPARE
  AS
    q VARCHAR2(2000);
  BEGIN
    BEGIN
        q := 'TRUNCATE TABLE TMP_BIOENTITY';

        dbms_output.Put_line(q);

        EXECUTE IMMEDIATE q;
        
    EXCEPTION
        WHEN OTHERS THEN NULL;
    END;

    BEGIN
        q :='DROP INDEX tmp_bioentity_accession';
        dbms_output.Put_line(q);

        EXECUTE IMMEDIATE q;
    EXCEPTION
      WHEN OTHERS THEN NULL;
    END;
    COMMIT WORK;
  END A2_BIOENTITYSETPREPARE;

  /* Procedure to write DesignElements and their mappings to
  Bioentities from from tmp_bioentity table*/
  PROCEDURE A2_virtualdesignset (adaccession VARCHAR2,
                                 adname      VARCHAR2,
                                 typename    VARCHAR2,
                                 adprovider    VARCHAR2,
                                 swname      VARCHAR2,
                                 swversion   VARCHAR2,
                                 detype      VARCHAR2)
  AS
    adid      INT := 0;
    mappingid INT := 0;
    v_sysdate TIMESTAMP;
    cnt       INT := 0;
    no_bioentities_found EXCEPTION;
  BEGIN
    SELECT COUNT (*)
    INTO   cnt
    FROM   tmp_bioentity;

    IF cnt = 0 THEN
      RAISE no_bioentities_found;
    END IF;

    --find/create ArrayDesignID
    BEGIN
        SELECT arraydesignid
        INTO   adid
        FROM   a2_arraydesign
        WHERE  accession = adaccession;

    UPDATE a2_arraydesign
    SET type = typename,
        name = adname,
        provider = adprovider
    WHERE arraydesignid = adid;
    EXCEPTION
        WHEN no_data_found THEN
          BEGIN
              INSERT INTO a2_arraydesign
                          (arraydesignid,
                           accession,
                           type,
                           provider,
                           name)
              SELECT a2_arraydesign_seq.nextval,
                     adaccession,
                     typename,
                     adprovider,
                     adname
              FROM   dual;

              SELECT a2_arraydesign_seq.currval
              INTO   adid
              FROM   dual;
          END;
    END;

    dbms_output.Put_line('ArrayDesingID = '
                         || adid);

    --find/create software id
    BEGIN
        SELECT SOFTWAREid
        INTO   mappingid
        FROM   a2_SOFTWARE
        WHERE  name = swname
               AND version = swversion;
    EXCEPTION
        WHEN no_data_found THEN
          BEGIN
              INSERT INTO a2_SOFTWARE
                          (SOFTWAREid,
                           name,
                           version)
              SELECT a2_SOFTWARE_seq.nextval,
                     swname,
                     swversion
              FROM   dual;

              SELECT a2_SOFTWARE_seq.currval
              INTO   mappingid
              FROM   dual;
          END;
    END;

    dbms_output.Put_line('mappingSWID = '
                         || mappingid);

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('start insert into a2_designelement '
                         || v_sysdate);

    INSERT INTO a2_designelement
                (designelementid,
                 arraydesignid,
                 accession,
                 name,
                 type)
    SELECT a2_designelement_seq.nextval,
           adid,
           be.acc,
           be.acc,
           detype
    FROM   (SELECT DISTINCT accession acc
            FROM   tmp_bioentity tbe
            WHERE NOT EXISTS (SELECT NULL
                              FROM a2_designelement de
                              WHERE de.accession = tbe.accession

                              AND de.arraydesignid = adid)) be;
    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('delete design element -> bioentity mappings '
                         || v_sysdate);

    DELETE FROM a2_designeltbioentity debe
        WHERE EXISTS (SELECT null
        FROM  a2_designelement de, (select distinct accession acc from tmp_bioentity) tbe

        where de.accession = tbe.acc
        and debe.designelementid = de.designelementid
        and debe.SOFTWAREid = mappingid
        and de.arraydesignid = adid);

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('start insert into A2_DESIGNELTBIOENTITY '
                         || v_sysdate);

    INSERT INTO a2_designeltbioentity
                (debeid,
                 designelementid,
                 bioentityid,
                 SOFTWAREid)
    SELECT a2_designeltbioentity_seq.nextval,
           debe.designelementid,
           debe.bioentityid,
           mappingid
    FROM   (SELECT DISTINCT de.designelementid,
                            be.bioentityid
            FROM   a2_designelement de,
                   a2_bioentity be
            WHERE  de.arraydesignid = adid
                   AND be.identifier = de.accession) debe;

    SELECT localtimestamp
    INTO   v_sysdate
    FROM   dual;

    dbms_output.Put_line('done '
                         || v_sysdate);

    COMMIT WORK;
  EXCEPTION
    WHEN no_bioentities_found THEN
               Raise_application_error (-20001,'No data found in TMP_BIOENTITY table.');
    WHEN OTHERS THEN
               RAISE;
               
  END a2_virtualdesignset;

    /* Procedure to write DesignElements and their mappings to
  Bioentities from from tmp_bioentity table*/
  PROCEDURE A2_DESIGNELEMENTMAPPINGSET (adaccession VARCHAR2,
                                 adname      VARCHAR2,
                                 typename    VARCHAR2,
                                 adprovider    VARCHAR2,
                                 swname      VARCHAR2,
                                 swversion   VARCHAR2,
                                 detype      VARCHAR2)
  AS
    adid      INT := 0;
    mappingid INT := 0;
    v_sysdate TIMESTAMP;
    cnt       INT := 0;
    no_bioentities_found EXCEPTION;

  BEGIN

    SELECT COUNT (*)
    INTO   cnt
    FROM   tmp_bioentity;

    IF cnt = 0 THEN
      RAISE no_bioentities_found;
    END IF;

    --find/create ArrayDesignID
    BEGIN
        SELECT arraydesignid
        INTO   adid
        FROM   a2_arraydesign
        WHERE  accession = adaccession;

    UPDATE a2_arraydesign
    SET type = typename,
        name = adname,
        provider = adprovider
    WHERE arraydesignid = adid;
    EXCEPTION
        WHEN no_data_found THEN
          BEGIN
              INSERT INTO a2_arraydesign
                          (arraydesignid,
                           accession,
                           type,
                           provider,
                           name)
              SELECT a2_arraydesign_seq.nextval,
                     adaccession,
                     typename,
                     adprovider,
                     adname
              FROM   dual;

              SELECT a2_arraydesign_seq.currval
              INTO   adid
              FROM   dual;
          END;
    END;

    dbms_output.Put_line('ArrayDesingID = ' || adid);

    --find/create software id
    BEGIN
        SELECT mappingsrcid
        INTO   mappingid
        FROM   a2_mappingsrc
        WHERE  name = swname
               AND version = swversion;
    EXCEPTION
        WHEN no_data_found THEN
          BEGIN
              INSERT INTO a2_mappingsrc
                          (mappingsrcid,
                           name,
                           version)
              SELECT a2_mappingsrc_seq.nextval,
                     swname,
                     swversion
              FROM   dual;

              SELECT a2_mappingsrc_seq.currval
              INTO   mappingid
              FROM   dual;
          END;
    END;

    dbms_output.Put_line('mappingSWID = '
                         || mappingid);


    SELECT localtimestamp INTO  v_sysdate FROM   dual;
    dbms_output.Put_line('start insert into a2_designelement ' || v_sysdate);


    INSERT INTO a2_designelement
                (designelementid,
                 arraydesignid,
                 accession,
                 name,
                 type)
    SELECT a2_designelement_seq.nextval,
           adid,
           be.acc,
           be.acc,
           detype
    FROM   (SELECT DISTINCT accession acc
            FROM   TMP_BIOENTITY tbe
            WHERE NOT EXISTS (SELECT NULL
                              FROM a2_designelement de
                              WHERE de.name = tbe.accession
                              AND de.arraydesignid = adid)) be;


    SELECT localtimestamp INTO  v_sysdate FROM   dual;
    dbms_output.Put_line('start insert into a2_bioentity ' || v_sysdate);


    INSERT INTO a2_bioentity
                (bioentityid,
                 identifier,
                 organismid,
                 type)
    SELECT a2_bioentity_seq.nextval,
           tbe.name,
           null,
           'UNKNOWN'
    FROM   (SELECT DISTINCT name
            FROM   tmp_bioentity) tbe
    WHERE  NOT EXISTS (select null
                       from a2_bioentity be
                       where be.identifier = tbe.name);

   
   SELECT localtimestamp INTO v_sysdate FROM   dual;
   dbms_output.Put_line('delete design element -> bioentity mappings ' || v_sysdate);

   DELETE FROM a2_designeltbioentity debe
        WHERE EXISTS (SELECT null
        FROM  a2_designelement de, (select distinct accession acc from TMP_BIOENTITY) tbe

        where de.name = tbe.acc
        and debe.designelementid = de.designelementid
        and debe.mappingsrcid = mappingid
        and de.arraydesignid = adid);

    SELECT localtimestamp INTO v_sysdate FROM   dual;
    dbms_output.Put_line('update tmp_bioentity - add designelementid in value column ' || v_sysdate);

    update tmp_bioentity tbe
    set value = (
     select de.designelementid
     from a2_designelement de
      where de.name = tbe.accession and de.arraydesignid = adid);

    SELECT localtimestamp INTO   v_sysdate FROM   dual;
    dbms_output.Put_line('start insert mapping into A2_DESIGNELTBIOENTITY ' || v_sysdate);

    INSERT INTO a2_designeltbioentity
                (debeid,
                 designelementid,
                 bioentityid,
                 mappingsrcid)
    SELECT a2_designeltbioentity_seq.nextval,
           debe.designelementid,
           debe.bioentityid,
           mappingid
    FROM   (SELECT DISTINCT tbe.value designelementid,
                            be.bioentityid
            FROM   a2_bioentity be,
                   tmp_bioentity tbe
            WHERE  be.identifier = tbe.name
                   ) debe;

    SELECT localtimestamp INTO   v_sysdate FROM   dual;
    dbms_output.Put_line('done ' || v_sysdate);


    COMMIT WORK;

  EXCEPTION
    WHEN no_bioentities_found THEN
               Raise_application_error (-20001,'No data found in TMP_BIOENTITY table.');
    WHEN OTHERS THEN
               RAISE;

  END A2_DESIGNELEMENTMAPPINGSET;

END atlasbeldr;
/
exit;
/
