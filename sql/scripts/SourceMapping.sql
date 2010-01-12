create table tmp_SourceMapping(SampleOntologyID int, SamplePropertyValueID int) 
create table tmp_SourceMappingAssay(AssayOntologyID int, AssayPropertyValueID int) 

Insert into tmp_SourceMapping(SampleOntologyID, SamplePropertyValueID)
Select sa.SampleOntologyID, spv.SamplePropertyValueID
from a2_SampleOntology sa
join a2_OntologyTerm ot on ot.OntologyTermID = sa.OntologyTermID
join ontology_annotation@dwprd oa on oa.Sample_ID_key = sa.SampleID and oa.Accession = ot.accession
join a2_PropertyValue pv on pv.name = ot.orig_value
join a2_property p on p.propertyid = pv.PropertyID and p.ae1tablename_sample = oa.ORIG_VALUE_SRC
join a2_samplepropertyvalue spv on spv.SampleID = sa.SampleID and spv.propertyvalueid = pv.propertyvalueid

create index IDX_tmp_SampleOntologyID on tmp_SourceMapping(SampleOntologyID) 

drop table tmp_SourceMappingAssay1

Insert into tmp_SourceMappingAssay(AssayOntologyID, AssayPropertyValueID)
Select sa.AssayOntologyID, MIN(spv.AssayPropertyValueID)
from a2_AssayOntology sa
join a2_OntologyTerm ot on ot.OntologyTermID = sa.OntologyTermID
join ontology_annotation@dwprd oa on oa.Assay_ID_key = sa.AssayID and oa.Accession = ot.accession
join a2_PropertyValue pv on pv.name = ot.orig_value
join a2_property p on p.propertyid = pv.PropertyID and p.ae1tablename_assay = oa.ORIG_VALUE_SRC
join a2_Assaypropertyvalue spv on spv.AssayID = sa.AssayID and spv.propertyvalueid = pv.propertyvalueid
group by sa.AssayOntologyID

create index IDX_tmp_AssayOntologyID on tmp_SourceMappingAssay(AssayOntologyID) 

create index IDX_tmp_AssayOntologyID1 on tmp_SourceMappingAssay1(AssayOntologyID)

--where sa.SampleID = 240222952 
--and so1.sampleontologyid= sa.sampleontologyid;
select * from a2_sampleontology

commit work

update a2_SampleOntology set SourceMapping = (select SamplePropertyValueID from tmp_SourceMapping t where t.sampleontologyid = a2_SampleOntology.sampleontologyid)

update a2_AssayOntology set SourceMapping = (select AssayPropertyValueID from tmp_SourceMappingAssay t where t.assayontologyid = a2_AssayOntology.Assayontologyid)

create table tmp_SourceMappingAssay1 as select distinct * from tmp_SourceMappingAssay

select AssayOntologyID, count(1) 
from tmp_SourceMappingAssay
group by AssayOntologyID
having count(1) > 1

rollback
