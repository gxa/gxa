

delete from a2_AssayPV where AssayID not in (Select AssayID from a2_Assay)

delete from a2_assaypv pv 
where exists (select 1 from a2_assaypv pv1 where pv1.AssayID = pv.AssayID and pv1.PropertyValueID = pv.PropertyValueID and pv1.assaypvid < pv.assaypvid   )

delete from a2_assaypvontology where AssayPVID is null

delete from a2_assaypvontology pv 
where exists (select 1 from a2_assaypvontology pv1 where pv1.AssayPVID = pv.AssayPVID and pv1.OntologyTermID = pv.OntologyTermID and pv1.assaypvontologyid < pv.assaypvontologyid   )

delete from A2_GENEPROPERTYVALUE pv 
where exists (select 1 from A2_GENEPROPERTYVALUE pv1 where pv1.GENEPROPERTYID = pv.GENEPROPERTYID and pv1.VALUE = pv.VALUE and pv1.genepropertyvalueid < pv.genepropertyvalueid  )

delete from A2_GENEGPV pv 
where exists (select 1 from A2_GENEGPV pv1 where pv1.GENEID = pv.GENEID and pv1.GENEPROPERTYVALUEID = pv.GENEPROPERTYVALUEID and pv1.genegpvid < pv.genegpvid )

  60146
7741512

select count(1) from a2_genegpv


select * from A2_GENEGPV


ALTER TABLE "A2_GENEGPV" ADD CONSTRAINT "UQ_GENEGPV" UNIQUE("GENEID","GENEPROPERTYVALUEID") ENABLE; 

commit

Select "GENEPROPERTYID","VALUE", count(1)
from A2_GENEPROPERTYVALUE
group by "GENEPROPERTYID","VALUE"
having count(1) > 1
