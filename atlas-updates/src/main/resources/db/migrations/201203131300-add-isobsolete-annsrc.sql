alter table A2_ANNOTATIONSRC add ISOBSOLETE VARCHAR2(1) default 'T';

UPDATE A2_ANNOTATIONSRC SET ISOBSOLETE='F' WHERE SOFTWAREID IN (SELECT MAX(SOFTWAREID) FROM A2_SOFTWARE GROUP BY NAME);