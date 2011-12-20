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

/*
 Dependencies:

 * drop the package;
 * drop all the types;
 * re-create the types
 */

CREATE OR REPLACE PACKAGE ATLASLDR IS

PROCEDURE A2_ARRAYDESIGNSET(
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,ENTRYPRIORITYLIST IDVALUETABLE  
  ,DesignElements DesignElementTable 
); 

PROCEDURE A2_EXPERIMENTSET(
  Accession varchar2
 ,Description varchar2 
 ,Performer varchar2 
 ,Lab varchar2
 ,PMID varchar2
 ,Abstract varchar2
);

PROCEDURE A2_ASSAYSET(
   Accession varchar2
  ,ExperimentAccession  varchar2
  ,ArrayDesignAccession varchar2
  ,Properties PropertyTable
);

PROCEDURE A2_SAMPLESET(
    ExperimentAccession varchar2
  , SampleAccession varchar2
  , Assays AccessionTable
  , Properties PropertyTable
  , Channel varchar2
);

PROCEDURE A2_EXPERIMENTDELETE(
  Accession varchar2
);

END;
/

/*******************************************************************************
BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY BODY
*******************************************************************************/
create or replace
PACKAGE BODY ATLASLDR AS

--------------------------------------------------------
--  DDL for Procedure A2_ARRAYDESIGNSET
--------------------------------------------------------
PROCEDURE A2_ARRAYDESIGNSET (
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,ENTRYPRIORITYLIST IDVALUETABLE  
  ,DesignElements DesignElementTable 
) 
as
  TheArrayDesignID int := 0;
  TheAccession varchar2(255) := Accession;
  TheType varchar2(255) := Type;
  TheProvider varchar2(255) := Provider;
  TheName varchar2(255) := Name;
  LowerCaseDesignElements DesignElementTable :=  A2_ARRAYDESIGNSET.DesignElements;
  TheGeneID integer;
  StartTime date := sysdate; 
begin

 --RAISE_APPLICATION_ERROR(-20001, 'A2_ARRAYDESIGNSET not implemented');
 
 --convert all property names to lowercase
 if(LowerCaseDesignElements is not null) then 
  for j in LowerCaseDesignElements.first..LowerCaseDesignElements.last loop
    LowerCaseDesignElements(j).EntryName := LOWER(LowerCaseDesignElements(j).EntryName);
    LowerCaseDesignElements(j).EntryValue := RTRIM(LTRIM(LowerCaseDesignElements(j).EntryValue));
  end loop;
  end if;
 
 BEGIN
 
 --find/create ArrayDesignID
 Select ArrayDesignID into TheArrayDesignID
 from a2_ArrayDesign
 where Accession = TheAccession;

 Update a2_ArrayDesign 
 set Type = TheType
     ,Name = TheName
     ,Provider = TheProvider
 where ArrayDesignID = TheArrayDesignID;   

 EXCEPTION
 WHEN NO_DATA_FOUND THEN
 begin
  Insert into a2_ArrayDesign(ArrayDesignID, Accession,Type,Name,Provider)
  select a2_ArrayDesign_Seq.nextval, TheAccession,TheType, TheName, TheProvider
  from dual;
 
  Select a2_ArrayDesign_Seq.CURRVAL into TheArrayDesignID from dual;
 end;
 END;
 dbms_output.put_line(' create ArrayDesignID: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));

 --update properties 
 MERGE /*+ parallel(a2_GeneProperty,10) append */ into a2_GeneProperty p 
 USING (select distinct EntryName from table(CAST(LowerCaseDesignElements as DesignElementTable))) t
 ON (t.EntryName = p.Name)
 WHEN MATCHED THEN 
  Update set p.GenePropertyID = p.GenePropertyID --9i noop
 WHEN NOT MATCHED THEN 
  Insert (GenePropertyID, Name) 
  values (a2_GeneProperty_Seq.nextval, t.EntryName);  
 dbms_output.put_line(' update properties: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --update propertyvalues
 MERGE /*+ parallel(a2_GenePropertyValue,10) append */ into a2_GenePropertyValue p
 USING (select distinct p.GenePropertyID, EntryName, EntryValue 
        from table(CAST(LowerCaseDesignElements as DesignElementTable)) d 
        join a2_GeneProperty p on p.Name = d.EntryName
        where d.EntryValue is not null) t
 ON (p.GenePropertyID = t.GenePropertyID and p.Value = t.EntryValue)
 WHEN MATCHED THEN 
  Update set p.GenePropertyValueID = p.GenePropertyValueID --9i noop 
 WHEN NOT MATCHED THEN
   Insert (GenePropertyValueID, GenePropertyID, value)
   values (a2_GenePropertyValue_seq.nextval, t.GenePropertyID, t.EntryValue);
 dbms_output.put_line(' update propertyvalues: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --map DesignElementAccessions to existing genes

 Insert into tmp_DesignElementMap(DesignElementAccession,GeneID,GeneIdentifier)
 select distinct t.Accession, min(GeneID), null
 from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
 join (select g.GeneID
        ,p.Name
        ,pv.Value
 from a2_Gene g
 join a2_GeneGPV gpv on gpv.GeneID = g.GeneID
 join a2_GenePropertyValue pv on pv.genepropertyvalueid = gpv.genepropertyvalueid
 join a2_geneproperty p on p.genepropertyid = pv.genepropertyid
 join table(CAST(ENTRYPRIORITYLIST as IDVALUETABLE)) t on t.Value = p.Name) IDs on ids.name = t.EntryName and ids.value = t.EntryValue
 group by t.Accession;
 dbms_output.put_line(' map DesignElementAccessions to existing genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --add accessions for non-existing genes
 Insert into tmp_DesignElementMap(designelementaccession,GeneID,GeneIdentifier)
 select distinct Accession, null, EntryValue
 from (
 select t.Accession
      , EntryValue
      , RANK() OVER (PARTITION BY t.Accession ORDER BY p.priority) r
 from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
 join (select Value, ID as priority from table(cast(ENTRYPRIORITYLIST as IDVALUETABLE))) p on p.Value = t.EntryName
 where not exists(select 1 from tmp_DesignElementMap m where m.designelementaccession = t.Accession)
 ) t where t.r=1 
 and EntryValue is not null;
 dbms_output.put_line(' add accessions for non-existing genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 /*for r in (select * from table(CAST(LowerCaseDesignElements as DesignElementTable))) loop
       dbms_output.put_line( 'ACCESSION:' || r.ACCESSION || ' ENTRYNAME:' || r.ENTRYNAME || ' ENTRYVALUE:' || r.ENTRYVALUE );
   end loop;
  
   for r in (select * from tmp_DesignElementMap) loop
       dbms_output.put_line( 'DE ACCESSION:' || r.DESIGNELEMENTACCESSION || ' GENEID:' || r.GENEID || ' GENEIDENTIFIER:' || r.GENEIDENTIFIER );
   end loop;*/
  
 -- add accessions for non-identifiable genes  
 Insert into  tmp_DesignElementMap(designelementaccession,GeneID,GeneIdentifier)
 select distinct Accession, null, 'GENE_BY_DE:' || Accession
 from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
 where not exists(select 1 from tmp_DesignElementMap m where m.designelementaccession = t.Accession);
 dbms_output.put_line(' add accessions for non-identifiable genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 -- find orphane genes with same identifiers (to keep identifier unique)
 Update tmp_DesignElementMap set GeneID = (select GeneID from a2_Gene where identifier = tmp_DesignElementMap.geneidentifier)
 where GeneID is null;
 dbms_output.put_line(' find orphane genes with same identifiers: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --create missed genes  
 Insert into a2_Gene(GeneID, Name, Identifier)
 select a2_Gene_Seq.nextval,'GENE:' || r.GeneIdentifier, r.GeneIdentifier
 from (select distinct GeneIdentifier from tmp_DesignElementMap r1
 where r1.GeneID is null
 and not exists(select 1 from a2_Gene where a2_Gene.identifier = r1.GeneIdentifier) )r;
   
 Update tmp_DesignElementMap 
 set GeneID = (Select GeneID from a2_Gene where Identifier = tmp_DesignElementMap.GeneIdentifier) 
 where GeneID is null;
 dbms_output.put_line(' create missed genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 /*
 for r in (select * from tmp_DesignElementMap) loop
   dbms_output.put_line('GENE:' || r.GeneID || ' ' || r.GeneIdentifier);
 end loop;
 */
 
 /*
 if exists(select 1 from tmp_DesignElementMap where ""=RTRIM(LTRIM(Identifier))) then
  RAISE_APPLICATION_ERROR(-20001, 'empty gene identifier');
 end if;
 */
 
 --add properties (do not remove old properties from Gene) 
 MERGE /*+ parallel(a2_GeneGPV,12) append */ INTO a2_GeneGPV gpv
 USING (select distinct m.GeneID, pv.genepropertyvalueid 
        from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
        join tmp_DesignElementMap m on m.designelementaccession = t.Accession
        join a2_GeneProperty p on p.name = t.EntryName
        join a2_genepropertyvalue pv on pv.genepropertyid = p.genepropertyid and pv.value = t.EntryValue) t 
 ON (t.GeneID = gpv.GeneID and t.GenePropertyValueID = gpv.GenePropertyValueID)
 WHEN MATCHED THEN 
  Update set gpv.GeneGPVID = gpv.GeneGPVID --9i noop  
 WHEN NOT MATCHED THEN
  Insert (GeneGPVID, GeneID, GenePropertyValueID)
  values (a2_GeneGPV_SEQ.nextval, t.GeneID, t.GenePropertyValueID);
 dbms_output.put_line(' add properties: ' || TO_CHAR(sysdate, 'HH24:MI:SS')); 
   
 delete from a2_designelement where arraydesignid = TheArrayDesignID;
 dbms_output.put_line(' delete existing design elements: ' || TO_CHAR(sysdate, 'HH24:MI:SS')); 
   
 --add design elements to ArrayDesign
 Insert into a2_DesignElement( DesignElementID
                              , ArrayDesignID
                              , GeneID
                              , Accession
                              , Name
                              , Type
                              , IsControl)
 select a2_DesignElement_seq.nextval
      , TheArrayDesignID
      , t.GeneID
      , t.DesignElementAccession
      , t.DesignElementAccession
      , 'compositeSequence'
      , null
 from (select Min(GeneID) GeneID
            , DesignElementAccession 
       from tmp_DesignElementMap
       group by DesignElementAccession) t;
 dbms_output.put_line(' add design elements to ArrayDesign: ' || TO_CHAR(sysdate, 'HH24:MI:SS')); 

  
 /* 
 Delete from a2_DesignElement where ArrayDesignID = TheArrayDesignID;
 
 Insert into a2_DesignElement (DesignElementID, ArrayDesignID, GeneID, Accession, Name, Type, IsControl)
 Select a2_DesignElement_seq.NEXTVAL, TheArrayDesignID, g.GeneID, de.Accession, de.Accession, 'unknown', 0
 from table(CAST(DesignElements as DesignElementTable)) de
 join a2_Gene g on g.Identifier = de.GeneAccession;
 */ 
 
 commit;
end;

--------------------------------------------------------
--  DDL for Procedure A2_ASSAYSET
--------------------------------------------------------
PROCEDURE A2_ASSAYSET (
   Accession varchar2
  ,ExperimentAccession  varchar2
  ,ArrayDesignAccession varchar2
  ,Properties PropertyTable
)
as
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  AssayID int :=0;
  UnknownDesignElementAccession int :=0; --;varchar2(255) := NULL;
  LowerCaseProperties PropertyTable := A2_AssaySet.Properties;
  MissedAccessionPercentage int := 0;
  UnknownAccessionThreshold int := 100;
  propMap PropertyOntologyTable := new PropertyOntologyTable();
begin

  begin
      Select MIN(e.ExperimentID) into A2_AssaySet.ExperimentID 
      from a2_Experiment e
      where e.Accession = A2_AssaySet.ExperimentAccession
      group by 1; --returns "NULL" oA2_AssaySet.rwise

      Select MIN(d.ArrayDesignID) into A2_AssaySet.ArrayDesignID
      from a2_ArrayDesign d
      where d.Accession = A2_AssaySet.ArrayDesignAccession
      group by 1;
      
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or array design not found');
    when others then 
      RAISE;
  end;
  
  begin
      Select a.AssayID into A2_AssaySet.AssayID
      from a2_Assay a
      where a.Accession = A2_AssaySet.Accession and a.ExperimentID = A2_AssaySet.ExperimentID;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Assay(AssayID, Accession,ExperimentID,ArrayDesignID)
      values (A2_ASSAY_SEQ.nextval,A2_AssaySet.Accession,A2_AssaySet.ExperimentID,A2_AssaySet.ArrayDesignID);
      
      Select a2_Assay_seq.currval into A2_AssaySet.AssayID from dual;
     end;
    when others then 
      RAISE;    
  end;
  
  if (A2_AssaySet.AssayID is null) then
    RAISE_APPLICATION_ERROR(-20001, 'assay can not be created');
  end if;
  
  --convert properties to lowercase
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;
  
  dbms_output.put_line('cleanup');
  delete a2_assaypv pv where pv.assayid = A2_AssaySet.AssayID;

  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value)
  and not(t.Value is null);

  dbms_output.put_line('link property value to assay');
  Insert into a2_assayPV(AssayID, PropertyValueID)
  select distinct A2_AssaySet.AssayID, pv.PropertyValueID
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
  where not exists(select 1 from a2_AssayPV pv1 where pv1.AssayID = A2_AssaySet.assayid and pv1.PropertyValueID = pv.PropertyValueID);
  
  for r in (select apv.AssayPVID, t.Ontologies
            from a2_AssayPV apv
            join a2_PropertyValue v on v.PropertyValueID = apv.PropertyValueID
            join a2_property p on p.PropertyID = v.PropertyID
            join table(CAST(properties as PROPERTYTABLE)) t on t.Name = p.Name and t.Value = v.Name
            where apv.assayID = A2_AssaySet.assayID) loop
            
            if(r.Ontologies is not null) then
            FOR r1 in (select r.AssayPVID, Ontology
                       from (select column_value Ontology from table(CAST(LIST_TO_TABLE_STR(r.Ontologies) as TBLVARCHAR)) t) 
                       ) loop
                        propMap.Extend(1);
                        propMap(propMap.LAST) := new PropertyOntology(SomePVID => r1.AssayPVID
                                                                     ,Ontology => r1.Ontology);
                       end loop;
           end if;            
  end loop;
  
  insert into a2_assaypvontology(AssayPVID,OntologyTermID)
  select m.SomePVID, ot.OntologyTermID
  from table(CAST(propMap as PropertyOntologyTable)) m
  join a2_ontologyterm ot on ot.Accession = m.Ontology
  where not exists(select 1 from a2_assaypvontology o1 where o1.AssayPVID = m.SomePVID and o1.OntologyTermID = ot.OntologyTermID);
  
  COMMIT WORK;

end;


--------------------------------------------------------
--  DDL for Procedure A2_EXPERIMENTSET
--------------------------------------------------------
PROCEDURE A2_EXPERIMENTSET (
  Accession varchar2
 ,Description varchar2 
 ,Performer varchar2 
 ,Lab varchar2
 ,PMID varchar2
 ,Abstract varchar2
)
AS
begin
  A2_EXPERIMENTDELETE(Accession);

  update a2_Experiment e
  set e.Description = A2_EXPERIMENTSET.description
  ,e.Performer = A2_EXPERIMENTSET.performer
  ,e.Lab = A2_EXPERIMENTSET.lab
  ,e.PMID = A2_EXPERIMENTSET.PMID
  ,e.Abstract = A2_EXPERIMENTSET.Abstract
  ,e.LOADDATE = SYSDATE
  where e.accession = A2_EXPERIMENTSET.Accession;
  
  dbms_output.put_line('updated ' || sql%rowcount || ' rows' );
  
  if ( sql%rowcount = 0 )
  then
     insert into a2_Experiment(Accession,Description,Performer,Lab,Loaddate,PMID,Abstract)
     values (A2_EXPERIMENTSET.Accession,A2_EXPERIMENTSET.Description,A2_EXPERIMENTSET.Performer,A2_EXPERIMENTSET.Lab,sysdate,A2_EXPERIMENTSET.PMID,A2_EXPERIMENTSET.Abstract);
     
     dbms_output.put_line('inserted');
  else
     dbms_output.put_line('update rowcount <> 0');
  end if;
  
  commit;
  
end;

--------------------------------------------------------
--  DDL for Procedure A2_SAMPLESET
--------------------------------------------------------
PROCEDURE A2_SAMPLESET (
    ExperimentAccession varchar2
  , SampleAccession varchar2
  , Assays AccessionTable
  , Properties PropertyTable
  , Channel varchar2
) 
as
  SampleID int :=0;
  
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  UnknownDesignElementAccession varchar2(255) := NULL;
  LowerCaseProperties PropertyTable := A2_SAMPLESET.Properties;
  AssayFound int := 0; 
  propMap PropertyOntologyTable := new PropertyOntologyTable();
begin

 dbms_output.put_line('checking sample accession'); 
 begin
      Select s.SampleID into SampleID
      from a2_Sample s
      join a2_AssaySample ass on ass.SampleID = s.SampleID
      join a2_Assay a on a.AssayID = ass.AssayID
      join a2_Experiment e on e.ExperimentID = a.ExperimentID
      where e.Accession = A2_SAMPLESET.ExperimentAccession
      and s.Accession = A2_SAMPLESET.SampleAccession;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Sample(Accession,Channel)
      values (A2_SAMPLESET.SampleAccession,A2_SAMPLESET.Channel);
      
      Select a2_Sample_seq.currval into SampleID from dual;
     end;
    when others then 
      RAISE;    
  end;

  
  select count(1) into AssayFound from a2_Assay a
                         join a2_Experiment e on a.ExperimentID = e.ExperimentID
                         where a.Accession in ( select * from table(CAST(A2_SAMPLESET.Assays as AccessionTable)) t)
                         and e.accession = A2_SAMPLESET.ExperimentAccession;
               
  if (AssayFound < 1) then
  RAISE_APPLICATION_ERROR(-20001, 'no assays found for sample');    
  end if;   
  
 --convert properties to lowercase 
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;
  
  dbms_output.put_line('linking sample and assay'); 
  Insert into a2_AssaySample(AssayID, SampleID)
  Select a.AssayID, A2_SAMPLESET.SampleID
  from a2_Assay a
  where a.Accession in (select * from table(CAST(A2_SAMPLESET.Assays as AccessionTable)) t)
  and a.ExperimentID = (Select ExperimentID from a2_Experiment where Accession = A2_SAMPLESET.ExperimentAccession)
  and not exists(select 1 from a2_AssaySample a2 
                  where a2.SampleID = A2_SAMPLESET.SampleID 
                  and a2.AssayID = a.AssayID);
  
  --select * from a2_AssaySample
  --select * from a2_Assay
  
  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(A2_SAMPLESET.LowerCaseProperties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value)
  and not(t.Value is null);

  dbms_output.put_line('link property value to assay');
  Insert into a2_samplePV(SampleID, PropertyValueID)
  select distinct SampleID, pv.PropertyValueID
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
  where not exists(select 1 from a2_SamplePV pv1 where pv1.SampleID = A2_SAMPLESET.Sampleid and pv1.PropertyValueID = pv.PropertyValueID);
  
  for r in (select apv.SamplePVID, t.Ontologies
            from a2_SamplePV apv
            join a2_PropertyValue v on v.PropertyValueID = apv.PropertyValueID
            join a2_property p on p.PropertyID = v.PropertyID
            join table(CAST(properties as PROPERTYTABLE)) t on t.Name = p.Name and t.Value = v.Name
            where apv.SampleID = A2_SAMPLESET.SampleID) loop
            
            if(r.Ontologies is not null) then
            FOR r1 in (select r.SamplePVID, Ontology
                       from (select column_value Ontology from table(CAST(LIST_TO_TABLE_STR(r.Ontologies) as TBLVARCHAR)) t) 
                       ) loop
                        propMap.Extend(1);
                        propMap(propMap.LAST) := new PropertyOntology(SomePVID => r1.SamplePVID
                                                                     ,Ontology => r1.Ontology);
                       end loop;
           end if;            
  end loop;
  
  insert into a2_Samplepvontology(SamplePVID,OntologyTermID)
  select m.SomePVID, ot.OntologyTermID
  from table(CAST(propMap as PropertyOntologyTable)) m
  join a2_ontologyterm ot on ot.Accession = m.Ontology
  where not exists(select 1 from a2_samplepvontology o1 where o1.SamplePVID = m.SomePVID and o1.OntologyTermID = ot.OntologyTermID);

  dbms_output.put_line('set organism id to the sample');
  insert into a2_organism(name)
  select lower(t.value)
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  where t.name = 'organism'
  and not exists(select 1 from a2_organism org where org.name = lower(t.value));

  update A2_Sample
  set organismid = (
     select org.organismid
     from a2_organism org, table(CAST(LowerCaseProperties as PropertyTable)) t
     where org.name = lower(t.value)
     and t.name = 'organism'
  )
  where sampleid = A2_SAMPLESET.SampleID;

  COMMIT WORK;
end;

PROCEDURE A2_EXPERIMENTDELETE(
  Accession varchar2
)
AS 
  ExperimentID integer := 0;
BEGIN
 
 begin
  Select ExperimentID into ExperimentID 
  from a2_Experiment where Accession = A2_EXPERIMENTDELETE.Accession;
 exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      --RAISE_APPLICATION_ERROR(-20001, 'experiment not found');
      return;
    when others then 
      RAISE;
  end;

  Delete from a2_SamplePVOntology where SamplePVID in (Select spv.SamplePVID
                                                         from vwExperimentSample 
                                                         join a2_SamplePV spv on spv.SampleID = vwExperimentSample.SampleID
                                                         where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_SamplePV where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  --Delete from a2_Sample where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);

  --redundant call, assaysample must be deleted by CASCADE delete
  Delete from a2_AssaySample where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  
  Delete from a2_Sample where not exists(select 1 from a2_AssaySample where a2_AssaySample.SampleID = a2_Sample.SampleID);
  
  
  Delete from a2_AssayPVOntology where AssayPVID in (Select AssayPVID
                                                    from a2_AssayPV   
                                                    join a2_Assay ON a2_Assay.AssayID = a2_AssayPV.AssayID 
                                                    where a2_Assay.ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_AssayPV where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
 
  Delete from a2_Experiment where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
  
  commit;
END;

END;
/
exit;
