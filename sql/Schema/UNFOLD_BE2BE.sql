CREATE OR REPLACE PROCEDURE UNFOLD_BE2BE AS
  beid      INT := -1;
  cnt       INTEGER := 0;
  CURSOR becur IS
    SELECT bioentityid AS id
    FROM   a2_bioentity be;
  berec     becur%ROWTYPE;
  v_sysdate TIMESTAMP;
  q         VARCHAR2(2000);
BEGIN
  q := 'TRUNCATE TABLE a2_be2be_unfolded';

  dbms_output.Put_line(q);

  EXECUTE IMMEDIATE q;

  SELECT localtimestamp
  INTO   v_sysdate
  FROM   dual;

  dbms_output.Put_line('update relations '
                       || v_sysdate);

  OPEN becur;

  LOOP
      FETCH becur INTO berec;

      EXIT WHEN becur%notfound;

      beid := berec.id;

      --collect children
      SELECT COUNT (*)
      INTO   cnt
      FROM   a2_bioentity2bioentity be2be
      WHERE  be2be.bioentityidfrom = beid;

      IF ( cnt > 0 ) THEN
        FOR rc IN (SELECT be2be.bioentityidto AS connectionid
                   FROM   a2_bioentity2bioentity be2be
                   START WITH be2be.bioentityidfrom = beid
                   CONNECT BY PRIOR be2be.bioentityidto = be2be.bioentityidfrom)
        LOOP
            INSERT INTO a2_be2be_unfolded
            VALUES      (beid,
                         rc.connectionid);
        END LOOP;
      END IF;

      --collect parents
      SELECT COUNT (*)
      INTO   cnt
      FROM   a2_bioentity2bioentity be2be
      WHERE  be2be.bioentityidto = beid;

      IF ( cnt > 0 ) THEN
        FOR rc IN (SELECT be2be.bioentityidfrom AS connectionid
                   FROM   a2_bioentity2bioentity be2be
                   START WITH be2be.bioentityidto = beid
                   CONNECT BY PRIOR be2be.bioentityidfrom = be2be.bioentityidto)
        LOOP
            INSERT INTO a2_be2be_unfolded
            VALUES      (beid,
                         rc.connectionid);
        END LOOP;
      END IF;
  END LOOP;

  COMMIT;

  SELECT localtimestamp
  INTO   v_sysdate
  FROM   dual;

  dbms_output.Put_line('DONE '
                       || v_sysdate);

END UNFOLD_BE2BE;
/
exit;
/