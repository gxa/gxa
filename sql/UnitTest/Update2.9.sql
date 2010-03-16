select * from a2_ontologyterm

select count(1) from a2_ontologyterm
select count(1) from (select distinct ACCESSION from a2_OntologyTerm)

select * from a2_OntologyTerm where Accession in (
'EFO_0000302','Brain'
'EFO_0000792','craniofacial tissue'
'EFO_0000943','craniofacial sceleton bone'
'EFO_0000110','spinal cord'
'EFO_0000265','aorta'
'EFO_0000815','heart'
'EFO_0000803','renal system'
'EFO_0000793','digestive system component'
'EFO_0000827','eye'
'EFO_0000800','liver and billary system'
'EFO_0000889','smooth muscle'
'EFO_0000934','lung'
'EFO_0000935','trachea'
'EFO_0000968','abdomen'
'EFO_0001385','tibialis anterior muscle'
'EFO_0001412','deltoid'
'EFO_0001413','gastrocnemius'
'EFO_0001937','vastus lateralis'

select * from t1
select * from a2_genegenepropertyvalue WHERE

select count(1) from t1 --4370315
select count(1) from a2_GenePropertyValue--11696436
select count(1) from a2_genegenepropertyvalue --4370315


ALTER TABLE a2_GenePropertyValue RENAME TO t2 
ALTER TABLE t1 RENAME TO a2_GenePropertyValue 

COMMIT

select count(1) from a2_GenePropertyValue

 ALTER TABLE t2 DROP CONSTRAINT "FK_GENEPROPERTY_GENE";
 ALTER TABLE t2 DROP CONSTRAINT "FK_GENEPROPERTYVALUE_PROPERTY"; 
 ALTER TABLE t2 DROP CONSTRAINT "PK_GENEPROPERTYVALUE"; 

 
 ALTER TABLE "A2_GENEPROPERTYVALUE" ADD CONSTRAINT "FK_GENEPROPERTYVALUE_PROPERTY" FOREIGN KEY ("GENEPROPERTYID") REFERENCES "A2_GENEPROPERTY" ("GENEPROPERTYID") ENABLE;


create table t3 as select Min(OntologyTermID) OntologyTermID,OntologyID,Accession from a2_ontologyterm
group by OntologyID, Accession

select * from t3

commit

Update a2_Sampleontology set OntologyTermID = (
  Select t3.OntologyTermID 
  from t3 
  join a2_OntologyTerm ot on ot.Accession=t3.Accession and ot.OntologyID = t3.ontologyid
  where ot.OntologyTermID = a2_sampleontology.ontologytermid)

Update a2_AssayOntology set OntologyTermID = (
  Select t3.OntologyTermID 
  from t3 
  join a2_OntologyTerm ot on ot.Accession=t3.Accession and ot.OntologyID = t3.ontologyid
  where ot.OntologyTermID = a2_assayontology.ontologytermid)

Delete from a2_OntologyTerm
where not exists (select 1 from t3 where t3.OntologyTermID = a2_OntologyTerm.OntologyTermID)

ALTER TABLE a2_GENEGENEPROPERTYVALUE RENAME TO a2_GENEGPV;
ALTER TABLE a2_GENEGPV RENAME COLUMN GENEGENEPROPERTYVALUEID TO GENEGPVID;

ALTER TABLE A2_ASSAYONTOLOGY RENAME TO A2_ASSAYPVONTOLOGY;
ALTER TABLE A2_ASSAYPVONTOLOGY RENAME COLUMN ASSAYONTOLOGYID TO ASSAYPVONTOLOGYID;
ALTER TABLE A2_ASSAYPVONTOLOGY RENAME COLUMN SOURCEMAPPING TO ASSAYPVID;

ALTER TABLE A2_ASSAYPROPERTYVALUE RENAME TO A2_ASSAYPV;
ALTER TABLE A2_ASSAYPV RENAME COLUMN ASSAYPROPERTYVALUEID TO ASSAYPVID;

ALTER TABLE A2_SAMPLEONTOLOGY RENAME TO A2_SAMPLEPVONTOLOGY;
ALTER TABLE A2_SAMPLEPVONTOLOGY RENAME COLUMN SAMPLEONTOLOGYID TO SAMPLEPVONTOLOGYID;
ALTER TABLE A2_SAMPLEPVONTOLOGY RENAME COLUMN SOURCEMAPPING TO SAMPLEPVID;

ALTER TABLE A2_SAMPLEPROPERTYVALUE RENAME TO A2_SAMPLEPV;
ALTER TABLE A2_SAMPLEPV RENAME COLUMN SAMPLEPROPERTYVALUEID TO SAMPLEPVID;

ALTER TABLE A2_SPEC RENAME TO A2_ORGANISM;
ALTER TABLE A2_ORGANISM RENAME COLUMN SPECID TO ORGANISMID;



select count(1) from a2_GeneGPV
select count(1) from a2_GenePropertyValue

select * from a2_GeneProperty

delete from a2_GENEGPV
select count(1) from 
where GenePropertyValueID in (select GenePropertyValueID from a2_GenePropertyValue where GenePropertyID = 15) 

create table t4 as select a2_GeneGPV.GeneGPVID,a2_GeneGPV.GENEID,a2_GeneGPV.GenePropertyValueID
from a2_GeneGPV
join a2_GenePropertyValue on a2_GenePropertyValue.GenePropertyValueID = a2_GeneGPV.GenePropertyValueID
where a2_GenePropertyValue.GenePropertyID <> 15

select * from a2_Genegpv

select count(1) from t4         -- 7741512
select count(1) from a2_Genegpv --11696436

ALTER TABLE a2_GeneGPV RENAME to a2_GeneGPV_old;
ALTER TABLE t4 RENAME to a2_GeneGPV;

delete from a2_GenePropertyValue where genepropertyid = 15

COMMIT

ALTER TABLE a2_GeneGPV_old DROP CONSTRAINT FK_GeneGPV_GenePropertyValue 

Create INDEX IDX_1 ON a2_GENEGPV (GenePropertyValueID)

select * from a2_GENEGPV

select 1 from dual;

Select count(1) from a2_OntologyTerm
Select count(1) from a2_GeneGPV

select * from a2_OntologyTerm where TERM like '%diges%'


select distinct Ontology_id_key, Term, Accession, Description, ORIG_VALUE, ORIG_VALUE_SRC from ontology_annotation@dwprd

4009
2118

embryo	EFO_0001367		E8.5
embryo	EFO_0001367		E8.5
embryo	EFO_0001367		E9.5
embryo	EFO_0001367		E9.5
embryo	EFO_0001367		E10.5
embryo	EFO_0001367		E10.5
embryo	EFO_0001367		E11.5
embryo	EFO_0001367		E11.5
embryo	EFO_0001367		E12.5
embryo	EFO_0001367		E12.5
embryo	EFO_0001367		E13.5
embryo	EFO_0001367		E13.5
embryo	EFO_0001367		E14.5
embryo	EFO_0001367		E14.5
embryo	EFO_0001367		E15.5
embryo	EFO_0001367		E15.5
embryo	EFO_0001367		E16.5
embryo	EFO_0001367		E16.5
embryo	EFO_0001367		E17.5
embryo	EFO_0001367		E18.5
embryo	EFO_0001367		E18.5
embryo	EFO_0001367		embryo
embryo	EFO_0001367		embryo
embryo	EFO_0001367		embryo
embryo	EFO_0001367		embryo
embryo	EFO_0001367		TS4,embryo
embryo	EFO_0001367		TS5,embryo
embryo	EFO_0001367		TS9,embryo
embryo	EFO_0001367		TS10,embryo
embryo	EFO_0001367		TS11,embryo
embryo	EFO_0001367		TS12,embryo
embryo	EFO_0001367		TS13,embryo
embryo	EFO_0001367		TS14,embryo
embryo	EFO_0001367		TS15,embryo
embryo	EFO_0001367		TS16,embryo
embryo	EFO_0001367		TS17,embryo
embryo	EFO_0001367		TS18,embryo
embryo	EFO_0001367		TS19,embryo
embryo	EFO_0001367		TS20,embryo
embryo	EFO_0001367		TS21,embryo
embryo	EFO_0001367		TS22,embryo
embryo	EFO_0001367		TS23,embryo
embryo	EFO_0001367		embryonic stage 2
embryo	EFO_0001367		embryonic stage 5
embryo	EFO_0001367		embryonic stage 7
embryo	EFO_0001367		embryonic stage 9
embryo	EFO_0001367		embryo (E6.5 days)
embryo	EFO_0001367		embryo (E7.5 days)
embryo	EFO_0001367		embryonic stage 11
embryo	EFO_0001367		embryonic stage 11
embryo	EFO_0001367		embryonic stage 12
embryo	EFO_0001367		embryonic stage 12
embryo	EFO_0001367		embryonic stage 14
embryo	EFO_0001367		embryonic stage 11-15
embryo	EFO_0001367		embryo (pre- or early-gastrulation stage)
embryo	EFO_0001367		embryo (pre- or early-gastrulation stage)