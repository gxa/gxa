--
-- Only use it if you understand what you're doing
-- This script will speed up the index building, but it will also hide all the analytics changes updates
--

rename vwexpressionanalyticsbygene to vwea;

drop table vwexpressionanalyticsbygene;

create table vwexpressionanalyticsbygene parallel(degree 32) nologging as select distinct * from vwea order by geneid;
