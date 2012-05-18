-- Use full InterPro description instead of short one
UPDATE A2_EXTERNAL_BEPROPERTY 
SET NAME = 'interpro_description' WHERE NAME = 'interpro_short_description';