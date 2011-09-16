-- The script checking that the DB is in 2.0.9 state
--
-- In 2.0.10 upwards, we expect DB in one of two states:
-- 1. Managed by MigrationManager, in which case this script is already run and logged
-- 2. Not managed yet, in which case which script makes sure it is indeed a 2.0.9 database

select * from a2_schemachanges;

drop table a2_schemachanges;


