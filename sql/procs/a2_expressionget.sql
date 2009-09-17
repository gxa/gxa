
  CREATE OR REPLACE PROCEDURE "A2_EXPRESSIONGET" (GeneIds varchar2,EfvIds tblVarchar, io IN OUT sys_refcursor) 
--return sys_refcursor
as
a1 sys_refcursor;
--efv table( EvfID integer); --tblInteger;
begin
  
      
  open a1 for 
  select g.GeneID GeneID
       , g.Identifier GeneIdentifier
       , g.Name GeneName
       , f.name ExperimentalFactor
       , fv.name ExperimentalFactorValue
       , fv.ExperimentFactorValueID ExperimentFactorValueID
       , SUM(CASE WHEN TSTAT > 0 THEN 1 ELSE 0 END)  Up
       , SUM(CASE WHEN TSTAT < 0 THEN 1 ELSE 0 END)  Dn
       , f.name || '|' || fv.name EfvStringID
  from a2_expression e
  join a2_gene g on g.GeneID = e.GeneID
  join a2_experimentfactorvalue fv on fv.experimentfactorvalueid = e.experimentfactorvalueid
  join a2_experimentfactor f on f.experimentfactorid = fv.experimentfactorid
  join (
    select a.column_value val
    from the ( select cast( list_to_table(GeneIds) as tblInteger )
                             from dual ) a                     
                             where a.column_value is not null  
       ) g_i on g_i.val = e.geneid
  join (
   select e.ExperimentFactorValueID EfvID
    from a2_experimentfactorvalue e
    join a2_experimentfactor f on f.experimentfactorid = e.experimentfactorid
    join (select a.column_value val
        from the ( select EfvIds from dual ) a                     
                             where a.column_value is not null  ) a on a.val = f.Name || '|' || e.Name

  ) efv on efv.EfvID = e.experimentfactorvalueid
  group by g.GeneID
       , g.Identifier
       , g.Name 
       , f.name 
       , fv.name
       , fv.ExperimentFactorValueID
  order by GeneName, fv.ExperimentFactorValueID;
       
   io:=a1;    
   
   -- return io;
end;
/
 