-- Find all ensgene_atl bioentities that have same identifiers as ensgene bioentities
create table ensgene_atl_aux as
select * from A2_bioentity where bioentityid in (
select b.bioentityid from A2_bioentity b, A2_bioentity b1, A2_bioentitytype bt, A2_bioentitytype bt1
  where bt.name = 'ensgene_atl'
  and b1.identifier = b.identifier
  and bt1.name = 'ensgene'
  and b.bioentitytypeid = bt.bioentitytypeid
  and b1.bioentitytypeid = bt1.bioentitytypeid
);

-- Find all ensgene bioentities that have same identifiers as ensgene_atl bioentities
create table ensgene_aux as
  select * from A2_bioentity where bioentityid in (
select b1.bioentityid from A2_bioentity b, A2_bioentity b1, A2_bioentitytype bt, A2_bioentitytype bt1
  where bt.name = 'ensgene_atl'
  and b1.identifier = b.identifier
  and bt1.name = 'ensgene'
  and b.bioentitytypeid = bt.bioentitytypeid
  and b1.bioentitytypeid = bt1.bioentitytypeid
);

-- Re-point to ensgene bioentities all design elements previously pointing to ensgene_atl
update A2_DESIGNELTBIOENTITY dbe
set dbe.bioentityid = (select e.bioentityid from ensgene_aux e, ensgene_atl_aux ea where e.identifier = ea.identifier and ea.bioentityid = dbe.bioentityid)
where bioentityid in (select bioentityid from ensgene_atl_aux);

-- Now delete ensgene_atl in ensgene_atl_aux as no design elements are mapped to them any more
delete from A2_bioentity
where bioentityid in (select bioentityid from ensgene_atl_aux);

-- Drop auxiliary tables
drop table ensgene_atl_aux;
drop table ensgene_aux;