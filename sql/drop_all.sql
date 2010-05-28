-- packages
DROP PACKAGE ATLASAPI;
DROP PACKAGE ATLASLDR;
DROP PACKAGE ATLASMGR;
DROP PACKAGE BODY ATLASAPI;
DROP PACKAGE BODY ATLASLDR;
DROP PACKAGE BODY ATLASMGR;

-- procedures
DROP PROCEDURE CUR_MERGEPROPERTYVALUE;

-- functions
DROP FUNCTION LIST_TO_TABLE;

-- tables
BEGIN
  FOR T in (SELECT table_name from user_tables) LOOP
  EXECUTE IMMEDIATE ('DROP TABLE ' || T.table_name || ' CASCADE CONSTRAINTS');
  END LOOP;
END;
/

-- views
BEGIN
  FOR T in (SELECT view_name from user_views) LOOP
  EXECUTE IMMEDIATE ('DROP VIEW ' || T.view_name);
  END LOOP;
END;
/

-- types
BEGIN
  FOR T IN (SELECT type_name FROM user_types) LOOP
    EXECUTE IMMEDIATE ('DROP TYPE ' || T.type_name);
  END LOOP;
END;
/

-- sequences
BEGIN
  FOR T in (SELECT sequence_name from user_sequences) LOOP
  EXECUTE IMMEDIATE ('DROP SEQUENCE ' || T.sequence_name);
  END LOOP;
END;
/

purge recyclebin;

quit;
