-- Increased size of URL field
ALTER TABLE
   A2_annotationsrc
MODIFY (
   url varchar2(1024)
   );