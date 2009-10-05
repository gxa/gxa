
  CREATE TABLE "ATLAS2"."A2_ONTOLOGY" 
   (	"ONTOLOGYID" NUMBER(22,0) NOT NULL ENABLE, 
	"NAME" VARCHAR2(255 BYTE), 
	"SOURCE_URI" VARCHAR2(500 BYTE), 
	"DESCRIPTION" VARCHAR2(4000 BYTE), 
	"VERSION" VARCHAR2(50 BYTE), 
	 CONSTRAINT "SYS_C008068" PRIMARY KEY ("ONTOLOGYID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 
  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT)
  TABLESPACE "ATLAS2_INDX"  ENABLE
   ) PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 1048576 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT)
  TABLESPACE "ATLAS2_DATA" ;
 
