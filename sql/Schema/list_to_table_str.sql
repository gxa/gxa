/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa/
 */

 --------------------------------------------------------
--  DDL for Function LIST_TO_TABLE
--------------------------------------------------------

  CREATE OR REPLACE FUNCTION "LIST_TO_TABLE_STR" (
             p_LIST                 IN VARCHAR2,
             p_DELIMITER            IN VARCHAR2 DEFAULT ','
      ) 
      return TBLVARCHAR
  as
              v_STRING_TBL       TBLVARCHAR := TBLVARCHAR();
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
/
exit;