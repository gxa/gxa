create tablespace
   ATLAS2
datafile  
  '/OraData/ATLAS2.dbf'
size
   32000m;

alter tablespace
   ATLAS2
add datafile  
  '/OraData/ATLAS2_1.dbf'
size
   32000m;

CREATE USER Atlas2 IDENTIFIED BY Atlas2 DEFAULT TABLESPACE ATLAS2;