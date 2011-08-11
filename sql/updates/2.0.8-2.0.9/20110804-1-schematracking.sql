-- Estimated running time: 1 second

DROP TABLE A2_SCHEMACHANGES;

CREATE TABLE A2_SCHEMACHANGES
(
  CHANGEID NUMBER CONSTRAINT SC_ID NOT NULL,
  FILENAME VARCHAR2(255) NOT NULL,
  USERNAME VARCHAR2(255) NOT NULL,
  TIME TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
  CONSTRAINT A2_SCHEMACHANGES PRIMARY KEY (CHANGEID)
);

CREATE SEQUENCE A2_SCHEMACHANGES_SEQ;

CREATE TRIGGER A2_SCHEMACHANGES_TRG BEFORE INSERT ON A2_SCHEMACHANGES
FOR EACH ROW
BEGIN
  SELECT A2_SCHEMACHANGES_SEQ.NEXTVAL INTO :NEW.CHANGEID FROM DUAL;
END;
/

-- Will be done by script runner from the next version on
insert into A2_SCHEMACHANGES values (null, '20110804-1-schematracking.sql', 'alf', CURRENT_TIMESTAMP);