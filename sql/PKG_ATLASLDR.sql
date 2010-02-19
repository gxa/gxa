/*
drop procedure A2_ARRAYDESIGNSET;
drop procedure A2_EXPERIMENTSET;
drop procedure A2_ASSAYSET;
drop procedure A2_SAMPLESET;
drop procedure LOAD_PROGRESS;
*/


CREATE OR REPLACE PACKAGE ATLASLDR IS

PROCEDURE A2_ARRAYDESIGNSET(
   Accession varchar2
  ,Type varchar2
  ,Name varchar2
  ,Provider varchar2
  ,EntryPriorityList varchar2
  ,DesignElements DesignElementTable 
); 

PROCEDURE A2_EXPERIMENTSET(
  Accession varchar2
 ,Description varchar2 
 ,Performer varchar2 
 ,Lab varchar2
);

PROCEDURE A2_ASSAYSET(
   Accession varchar2
  ,ExperimentAccession  varchar2
  ,ArrayDesignAccession varchar2
  ,Properties PropertyTable
  ,ExpressionValues ExpressionValueTable
  --,UnknownAccessionThreshold int default 75  
);

PROCEDURE A2_AssaySetBegin(
   ExperimentAccession  IN varchar2 --null if many
);

PROCEDURE A2_AssaySetEnd(
   ExperimentAccession  IN varchar2 --null if many
);

PROCEDURE A2_SAMPLESET(
    Accession varchar2
  , Assays AccessionTable
  , Properties PropertyTable
  , Species varchar2
  , Channel varchar2
); 

PROCEDURE A2_AnalyticsSet(
    ExperimentAccession      IN   varchar2  
   ,Property                 IN   varchar2
   ,PropertyValue            IN   varchar2
   ,ExpressionAnalytics ExpressionAnalyticsTable
);

PROCEDURE A2_AnalyticsDelete(
    ExperimentAccession      IN   varchar2  
);

PROCEDURE A2_AnalyticsSetBegin(
    ExperimentAccession      IN   varchar2  
);

PROCEDURE A2_AnalyticsSetEnd(
    ExperimentAccession      IN   varchar2  
);

PROCEDURE LOAD_PROGRESS(
 experiment_accession varchar
 ,stage varchar --load, netcdf, similarity, ranking, searchindex
 ,status varchar --done, pending 
 ,load_type VARCHAR2 := 'experiment'
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
  ,EntryPriorityList varchar2
  ,DesignElements DesignElementTable 
) 
as
  TheArrayDesignID int := 0;
  TheAccession varchar2(255) := Accession;
  TheType varchar2(255) := Type;
  TheProvider varchar2(255) := Provider;
  TheName varchar2(255) := Name;
begin

 RAISE_APPLICATION_ERROR(-20001, 'A2_ARRAYDESIGNSET not implemented');
 
 Select ArrayDesignID into TheArrayDesignID
 from a2_ArrayDesign
 where Accession = TheAccession;

 Update a2_ArrayDesign 
 set Type = TheType
     ,Name = TheName
     ,Provider = TheProvider
 where ArrayDesignID = ArrayDesignID;   

 EXCEPTION
 WHEN NO_DATA_FOUND THEN
 begin
  Insert into a2_ArrayDesign(ArrayDesignID, Accession,Type,Name,Provider)
  select a2_ArrayDesign_Seq.nextval, TheAccession,TheType, TheName, TheProvider
  from dual;
 
  Select a2_ArrayDesign_Seq.CURRVAL into TheArrayDesignID from dual;
 end;
 
 Insert into a2_ArrayDesign (ArrayDesignID,Accession,Type,Name,Provider)
 select a2_ArrayDesign_Seq.nextval, TheAccession, TheType, TheName, TheProvider 
 from dual;
 
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
  ,ExpressionValues ExpressionValueTable
  --,UnknownAccessionThreshold int default 75  
)
as
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  AssayID int :=0;
  UnknownDesignElementAccession int :=0; --;varchar2(255) := NULL;
  LowerCaseProperties PropertyTable := A2_AssaySet.Properties;
  MissedAccessionPercentage int := 0;
  UnknownAccessionThreshold int := 100;
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
  
  /*
  dbms_output.put_line('check for invalid design elements');
  begin
    Select count(t.DesignElementAccession) into UnknownDesignElementAccession
    from table(CAST(A2_AssaySet.ExpressionValues as ExpressionValueTable)) t
    where not exists(select 1 from a2_designelement e 
                     where e.arraydesignid = A2_AssaySet.arraydesignid 
                     and ((e.accession = t.DesignElementAccession) or (e.name = t.DesignElementAccession)));
    
    Select (100*UnknownDesignElementAccession)/count(t.DesignElementAccession) into MissedAccessionPercentage
    from table(CAST(A2_AssaySet.ExpressionValues as ExpressionValueTable)) t;
    
    if(MissedAccessionPercentage > UnknownDesignElementAccession) A2_AssaySet.n
      RAISE_APPLICATION_ERROR(-20001, 'unknown accession threshold exceeded');
    end if;
  end;
  */

  begin
      Select a.AssayID into A2_AssaySet.AssayID
      from a2_Assay a
      where a.Accession = A2_AssaySet.Accession;
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
  
  --convert properties to lowercase
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;
  
  dbms_output.put_line('cleanup');
  delete a2_assaypv pv where pv.assayid = A2_AssaySet.AssayID;
  delete a2_expressionvalue ev where ev.assayid = A2_AssaySet.AssayID;
  
  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionValue(DesignElementID,AssayID,Value)
  select distinct d.DesignElementID, A2_AssaySet.AssayID, t.Value
  from table(CAST(A2_AssaySet.ExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on ((d.Accession = t.DesignElementAccession) or (d.name = t.DesignElementAccession));

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
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_assayPV(AssayID, PropertyValueID, IsFactorValue)
  select distinct A2_AssaySet.AssayID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
  where not exists(select 1 from a2_AssayPV pv1 where pv1.AssayID = A2_AssaySet.assayid and pv1.PropertyValueID = pv.PropertyValueID);
  
  /*
  Insert into a2_AssayPropertyValue(DesignElementID,ExperimentID,AssayID,Value)
  select d.DesignElementID, ExperimentID, AssayID, t.Value
  from table(CAST(A2_AssaySet.ExpressionValues as ExpressionValueTable)) t
  join a2_designelement d on d.Accession = t.DesignElementAccession;
  */

  /*
  if SQL%NOTFOUND A2_AssaySet.N
    RAISE_APPLICATION_ERROR(-20001, 'no expression values inserted');
  end if; 

  Insert into a2_AssayPropertyValue(AssayID,PropertyValueID,IsFactorValue)
  select 
  from table (CAST(A2_AssaySet.Prope) )
  */
  COMMIT WORK;

end;

PROCEDURE A2_AssaySetBegin(
   ExperimentAccession  IN varchar2 --null if many
)
AS
BEGIN
  null;
END;

PROCEDURE A2_AssaySetEnd(
   ExperimentAccession  IN varchar2 --null if many
)
AS
BEGIN
  null;
END;

--------------------------------------------------------
--  DDL for Procedure A2_EXPERIMENTSET
--------------------------------------------------------
PROCEDURE A2_EXPERIMENTSET (
  Accession varchar2
 ,Description varchar2 
 ,Performer varchar2 
 ,Lab varchar2
)
AS
begin
  update a2_Experiment e
  set e.Description = A2_EXPERIMENTSET.description
  ,e.Performer = A2_EXPERIMENTSET.performer
  ,e.Lab = A2_EXPERIMENTSET.lab
  where e.accession = A2_EXPERIMENTSET.Accession;
  
  dbms_output.put_line('updated ' || sql%rowcount || ' rows' );
  
  if ( sql%rowcount = 0 )
  then
     insert into a2_Experiment(Accession,Description,Performer,Lab)
     values (A2_EXPERIMENTSET.Accession,A2_EXPERIMENTSET.Description,A2_EXPERIMENTSET.Performer,A2_EXPERIMENTSET.Lab);
     
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
    Accession varchar2
  , Assays AccessionTable
  , Properties PropertyTable
  , Species varchar2
  , Channel varchar2
) 
as
  SampleID int :=0;
  
  ExperimentID int := 0;
  ArrayDesignID int := 0;
  UnknownDesignElementAccession varchar2(255) := NULL;
  LowerCaseProperties PropertyTable := A2_SAMPLESET.Properties;
begin

 dbms_output.put_line('checking sample accession'); 
 begin
      Select s.SampleID into SampleID
      from a2_Sample s
      where s.Accession = A2_SAMPLESET.Accession;
  exception
     when NO_DATA_FOUND then
     begin
      insert into A2_Sample(Accession,Species,Channel)
      values (A2_SAMPLESET.Accession,A2_SAMPLESET.Species,A2_SAMPLESET.Channel);
      
      Select a2_Sample_seq.currval into SampleID from dual;
     end;
    when others then 
      RAISE;    
  end;

 --convert properties to lowercase 
  if(LowerCaseProperties is not null) then 
  for j in LowerCaseProperties.first..LowerCaseProperties.last loop
    LowerCaseProperties(j).name := LOWER(LowerCaseProperties(j).name);
  end loop;
  end if;

  dbms_output.put_line('linking sample and assay'); 
  Insert into a2_AssaySample(AssayID, SampleID)
  Select AssayID, SampleID
  from a2_Assay a
  where a.Accession in (select * from table(CAST(A2_SAMPLESET.Assays as AccessionTable)) t);
  
  dbms_output.put_line('insert property');
  Insert into a2_Property(Name /*, Accession*/)
  select distinct t.Name
  from table(CAST(A2_SAMPLESET.Properties as PropertyTable)) t
  where not exists (select 1 from a2_Property where Name = t.Name);
  
  dbms_output.put_line('insert property value');
  Insert into a2_propertyvalue(propertyid,name)
  select distinct p.PropertyID, t.Value
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  where not exists(select 1 from a2_propertyvalue where PropertyID = p.PropertyID and name = t.Value);

  dbms_output.put_line('link property value to assay');
  Insert into a2_samplePV(SampleID, PropertyValueID, IsFactorValue)
  select distinct SampleID, pv.PropertyValueID, t.IsFactorValue
  from table(CAST(LowerCaseProperties as PropertyTable)) t
  join a2_Property p on p.name = t.name
  join a2_PropertyValue pv on pv.PropertyID = p.PropertyID and pv.name = t.value
  where not exists(select 1 from a2_SamplePV pv1 where pv1.SampleID = A2_SAMPLESET.Sampleid and pv1.PropertyValueID = pv.PropertyValueID);
  
  COMMIT WORK;
end;


--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsSet
--------------------------------------------------------
PROCEDURE A2_AnalyticsSet(
    ExperimentAccession      IN   varchar2  
   ,Property                 IN   varchar2
   ,PropertyValue            IN   varchar2
   ,ExpressionAnalytics ExpressionAnalyticsTable
 )
 as
  ExperimentID int := 0;
  PropertyValueID int := 0;
  LowerCaseProperty varchar2(255) := LOWER(Property);
  err varchar2(255) := 0;
begin

  begin
      Select MIN(e.ExperimentID) into ExperimentID 
      from a2_Experiment e
      where e.Accession = ExperimentAccession
      group by 1;
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      err:= 'experiment not found:' || NVL(ExperimentAccession,'NULL');
      RAISE_APPLICATION_ERROR(-20001,err );
    when others then 
      RAISE;
  end;

  begin       
      Select MIN(pv.PropertyValueID) into PropertyValueID
      from a2_Property p
      join a2_PropertyValue pv on pv.PropertyID = p.PropertyID
      where p.Name = LowerCaseProperty
      and pv.Name = PropertyValue
      group by 1;
   exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      err:= 'property not found:' || NVL(Property ,'NULL') || ':' || NVL(PropertyValue ,'NULL');
      RAISE_APPLICATION_ERROR(-20001, err);
    when others then 
      RAISE;
  end;

  dbms_output.put_line('insert expression value');
  Insert into a2_ExpressionAnalytics(DesignElementID,ExperimentID,PropertyValueID,TSTAT,PVALADJ,FPVAL,FPVALADJ)
  select t.DesignElementID, ExperimentID, PropertyValueID, t.Tstat, t.PVALADJ, null, null
  from table(CAST(ExpressionAnalytics as ExpressionAnalyticsTable)) t;

  COMMIT WORK;
END;


--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsDelete 
--------------------------------------------------------
PROCEDURE A2_AnalyticsSetBegin(
    ExperimentAccession      IN   varchar2  
 )
as
  q varchar2(2000);
  ExperimentID int := 0;
begin

if ExperimentAccession is null then
   
  q := 'DROP INDEX "A2_IDX_EXPRESSION_FV"';
  EXECUTE IMMEDIATE q;
  
  q := 'DROP INDEX "IDX_ANALYTICS_DESIGNELEMENT"';
  EXECUTE IMMEDIATE q;
  
  q := 'DROP INDEX "IDX_EA_EXPERIMENT"';
  EXECUTE IMMEDIATE q;
    
  q := 'ALTER TABLE "A2_EXPRESSIONANALYTICS" DISABLE CONSTRAINT "FK_ANALYTICS_EXPERIMENT"';  
  EXECUTE IMMEDIATE q;
  
  q := 'ALTER TABLE "A2_EXPRESSIONANALYTICS" DISABLE CONSTRAINT "FK_ANALYTICS_PROPERTYVALUE"';
  EXECUTE IMMEDIATE q;

  q := 'ALTER TRIGGER "A2_ExpressionAnalytics_Insert" DISABLE';
  EXECUTE IMMEDIATE q;
   
else --ExperimentAccession is null

  begin
      Select e.ExperimentID into ExperimentID 
      from a2_Experiment e
      where e.Accession = ExperimentAccession;
      
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or property not found');
    when others then 
      RAISE;
  end;

  dbms_output.put_line('delete expression value');
  delete from a2_ExpressionAnalytics
  where ExperimentID = A2_AnalyticsSetBegin.ExperimentID;
end if;
  
  COMMIT WORK;
END;

--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsDelete 
--------------------------------------------------------
PROCEDURE A2_AnalyticsSetEnd(
    ExperimentAccession      IN   varchar2  
 )
as
  ExperimentID int := 0;
  q varchar2(2000);
  INDEX_TABLESPACE varchar2(2000);
begin
  if ExperimentAccession is null then
  
  select 'TABLESPACE ' || TABLESPACE_NAME into INDEX_TABLESPACE
  from user_indexes where INDEX_NAME = 'PK_SPEC';

  q := 'CREATE INDEX "A2_IDX_EXPRESSION_FV" ON "A2_EXPRESSIONANALYTICS" ("PROPERTYVALUEID") $INDEX_TABLESPACE';
  q := REPLACE(q,'$INDEX_TABLESPACE', INDEX_TABLESPACE);
  EXECUTE IMMEDIATE q;
  
  q := 'CREATE INDEX "IDX_ANALYTICS_DESIGNELEMENT" ON "A2_EXPRESSIONANALYTICS" ("DESIGNELEMENTID") TABLESPACE ATLAS2TEST_INDX';
  q := REPLACE(q,'$INDEX_TABLESPACE', INDEX_TABLESPACE);
  EXECUTE IMMEDIATE q;
  
  q := 'CREATE INDEX "IDX_EA_EXPERIMENT" ON "A2_EXPRESSIONANALYTICS" ("EXPERIMENTID") TABLESPACE ATLAS2TEST_INDX';
  q := REPLACE(q,'$INDEX_TABLESPACE', INDEX_TABLESPACE);
  EXECUTE IMMEDIATE q;
    
  q := 'ALTER TABLE "A2_EXPRESSIONANALYTICS" ENABLE CONSTRAINT "FK_ANALYTICS_EXPERIMENT"';  
  EXECUTE IMMEDIATE q;
  
  q := 'ALTER TABLE "A2_EXPRESSIONANALYTICS" ENABLE CONSTRAINT "FK_ANALYTICS_PROPERTYVALUE"';
  EXECUTE IMMEDIATE q;

  q := 'ALTER TRIGGER "A2_ExpressionAnalytics_Insert" ENABLE';
  EXECUTE IMMEDIATE q;

  else --ExperimentAccession is not null
    null;  

  end if;
END;

--------------------------------------------------------
--  DDL for Procedure A2_AnalyticsDelete !!!OBSOLETE!!!
--------------------------------------------------------
PROCEDURE A2_AnalyticsDelete(
    ExperimentAccession      IN   varchar2  
 )
 as
  ExperimentID int := 0;
begin

 -- return;

  begin
      Select e.ExperimentID into ExperimentID 
      from a2_Experiment e
      where e.Accession = ExperimentAccession;
      
  exception 
    when NO_DATA_FOUND then
      dbms_output.put_line('NO_DATA_FOUND');  
      RAISE_APPLICATION_ERROR(-20001, 'experiment or property not found');
    when others then 
      RAISE;
  end;

  dbms_output.put_line('delete expression value');
  delete from a2_ExpressionAnalytics
  where ExperimentID = A2_AnalyticsDelete.ExperimentID;

  COMMIT WORK;
END;


--------------------------------------------------------
--  DDL for Procedure LOAD_PROGRESS
--------------------------------------------------------
PROCEDURE LOAD_PROGRESS (
 experiment_accession varchar
 ,stage varchar --load, netcdf, similarity, ranking, searchindex
 ,status varchar --done, pending 
 ,load_type VARCHAR2 := 'experiment'
)
as
 thestatus varchar(255) := status;
begin
  if stage not in ('load', 'netcdf', 'similarity', 'ranking', 'searchindex') then
    RAISE_APPLICATION_ERROR(-20001, 'unknownn stage. possible values:load,netcdf,similarity,ranking,searchindex');
  end if;  
  
  if status not in ('pending', 'done', 'working', 'failed') then
    RAISE_APPLICATION_ERROR(-20001, 'unknownn status. possible values:pending,done');
  end if;

  if(stage = 'load') then
    update load_monitor set status = thestatus where accession = experiment_accession;
  elsif(stage = 'netcdf') then 
   update load_monitor set netcdf = thestatus where accession = experiment_accession;   
  elsif(stage = 'similarity') then 
   update load_monitor set similarity = thestatus where accession = experiment_accession;   
  elsif(stage = 'ranking')  then
   update load_monitor set ranking = thestatus where accession = experiment_accession;   
  elsif(stage = 'searchindex') then 
   update load_monitor set searchindex = thestatus where accession = experiment_accession;
  end if; 
   
   if ( sql%rowcount = 0 ) then 
      Insert into load_monitor(
          ID
          , Accession
          , Status
          , Netcdf
          , Similarity
          , Ranking
          , SearchIndex
          , Load_Type)
      select 
          load_monitor_seq.nextval
          ,experiment_accession
          , CASE stage WHEN 'load' then thestatus else 'pending' end
          , CASE stage WHEN 'netcdf' then thestatus else 'pending' end
          , CASE stage WHEN 'similarity' then thestatus else 'pending' end
          , CASE stage WHEN 'ranking' then thestatus else 'pending' end
          , CASE stage WHEN 'searchindex' then thestatus else 'pending' end
          ,load_type
     from dual;    
    end if;
  
commit work;  
  
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
      RAISE_APPLICATION_ERROR(-20001, 'experiment not found');
    when others then 
      RAISE;
  end; 
  
  Delete from a2_ExpressionAnalytics where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID;
  Delete from a2_ExpressionValue where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);

  Delete from a2_SamplePVOntology where SamplePVID in (Select spv.SamplePVID 
                                                         from vwExperimentSample 
                                                         join a2_SamplePV spv on spv.SampleID = vwExperimentSample.SampleID
                                                         where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_SamplePV where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_AssaySample where AssayID in (Select AssayID from a2_Assay where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);
  Delete from a2_Sample where SampleID in (Select SampleID from vwExperimentSample where ExperimentID = A2_EXPERIMENTDELETE.ExperimentID);

  
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
/
