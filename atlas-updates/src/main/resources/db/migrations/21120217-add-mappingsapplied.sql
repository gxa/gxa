alter table A2_ANNOTATIONSRC add mappingsApplied VARCHAR2(1) DEFAULT 'F';
alter table A2_ANNOTATIONSRC rename column isApplied to annotationsApplied;