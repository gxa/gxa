create or replace type tblInteger 
  as table of integer;
/*******************************************************************************
*******************************************************************************/
create or replace function list_to_table(
             p_LIST                 IN VARCHAR2,
             p_DELIMITER            IN VARCHAR2 DEFAULT ','
      ) 
      return tblInteger
  as
              v_STRING_TBL       tblInteger := tblInteger();
              v_START_POSITION   NUMBER;
              v_END_POSTION      NUMBER;
              v_DELIMITER_LENGTH NUMBER;
              v_EXPANDED_LIST    VARCHAR2(32767);
  begin
          IF p_DELIMITER IS NULL
                THEN
                  RAISE_APPLICATION_ERROR(
                                          -20500,
                                          'Invalid Delimiter'
                                         );

                 END IF;
             v_EXPANDED_LIST    := p_LIST || p_DELIMITER;
              v_DELIMITER_LENGTH := LENGTH(
                                          p_DELIMITER
                                          );
              v_END_POSTION      := 1 - v_DELIMITER_LENGTH;
             LOOP
                v_START_POSITION := v_END_POSTION + v_DELIMITER_LENGTH;
                v_END_POSTION    := INSTR(
                                         v_EXPANDED_LIST,
                                          p_DELIMITER,
                                          v_START_POSITION
                                         );
                EXIT WHEN v_END_POSTION = 0;
                v_STRING_TBL.EXTEND;
                v_STRING_TBL(v_STRING_TBL.LAST) := SUBSTR(
                                                         v_EXPANDED_LIST,
                                                          v_START_POSITION,
                                                          v_END_POSTION - v_START_POSITION
                                                         );
              END LOOP;
              RETURN v_STRING_TBL;
  end;
