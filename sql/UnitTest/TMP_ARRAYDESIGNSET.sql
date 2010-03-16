create or replace
PROCEDURE TMP_ARRAYDESIGNSET (
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,DesignElements DesignElementTable 
) 
as
  TheArrayDesignID int := 0;
  TheAccession varchar2(255) := Accession;
  TheType varchar2(255) := Type;
  TheProvider varchar2(255) := Provider;
  TheName varchar2(255) := Name;
  LowerCaseDesignElements DesignElementTable :=  TMP_ARRAYDESIGNSET.DesignElements;
  TheGeneID integer;
  StartTime date := sysdate; 
begin

 --RAISE_APPLICATION_ERROR(-20001, 'A2_ARRAYDESIGNSET not implemented');
 
 --DO NOT convert all property names to lowercase
 if(LowerCaseDesignElements is not null) then 
  for j in LowerCaseDesignElements.first..LowerCaseDesignElements.last loop
    LowerCaseDesignElements(j).EntryName := LOWER(LowerCaseDesignElements(j).EntryName);
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
 MERGE into a2_GeneProperty p 
 USING (select distinct EntryName from table(CAST(LowerCaseDesignElements as DesignElementTable))) t
 ON (t.EntryName = p.Name)
 WHEN MATCHED THEN 
  Update set p.GenePropertyID = p.GenePropertyID --9i noop
 WHEN NOT MATCHED THEN 
  Insert (GenePropertyID, Name) 
  values (a2_GeneProperty_Seq.nextval, t.EntryName);  
 dbms_output.put_line(' update properties: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --update propertyvalues
 MERGE into a2_GenePropertyValue p
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
 select t.Accession, min(GeneID), null
 from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
 join vwGeneIDs IDs on ids.name = t.EntryName and ids.value = t.EntryValue
 group by t.Accession;
 dbms_output.put_line(' map DesignElementAccessions to existing genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));

 --add accessions for non-existing genes
 Insert into tmp_DesignElementMap(designelementaccession,GeneID,GeneIdentifier)
 select Accession, null, EntryValue
 from (
 select t.Accession, EntryValue, RowNum r 
 from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
 join vwgeneidproperty p on p.Name = t.EntryName
 where not exists(select 1 from tmp_DesignElementMap m where m.designelementaccession = t.Accession)
 order by p.Priority ) t where r=1;
 dbms_output.put_line(' add accessions for non-existing genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
  
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
   from tmp_DesignElementMap r
   where GeneID is null;
   
   Update tmp_DesignElementMap 
   set GeneID = (Select GeneID from a2_Gene where Identifier = tmp_DesignElementMap.GeneIdentifier) 
   where GeneID is null;
 
 /*
 for r in (select * from tmp_DesignElementMap where GeneID is null) loop
   select a2_Gene_Seq.nextval into TheGeneID from dual; 
    
   Insert into a2_Gene(GeneID, Name, Identifier)
   select TheGeneID,'GENE:' || r.GeneIdentifier, r.GeneIdentifier from dual;
      
   update tmp_DesignElementMap set GeneID = TheGeneID 
   where DesignElementAccession = r.DesignElementAccession;
 end loop;
 */ 
 dbms_output.put_line(' create missed genes: ' || TO_CHAR(sysdate, 'HH24:MI:SS'));
 
 --add properties (do not remove old properties from Gene) 
 MERGE INTO a2_GeneGPV gpv
 USING (select distinct m.GeneID, pv.genepropertyvalueid 
        from table(CAST(LowerCaseDesignElements as DesignElementTable)) t
        join tmp_DesignElementMap m on m.designelementaccession = t.Accession
        join a2_GeneProperty p on p.name = t.EntryName
        join a2_genepropertyvalue pv on pv.genepropertyid = p.genepropertyid and pv.value = t.EntryValue) t 
 ON (t.GeneID = gpv.GeneID)
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
 from tmp_DesignElementMap t;
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
