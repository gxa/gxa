-- Update array designs of all existing RNA-seq experiments to the HTS placeholder: A-ENST-X
update A2_assay set arraydesignid = (select arraydesignid from A2_arraydesign where accession = 'A-ENST-X')
where arraydesignid in (select arraydesignid from A2_arraydesign where accession in 
('A-ENST-1','A-ENST-10','A-ENST-11','A-ENST-12','A-ENST-13','A-ENST-14','A-ENST-15','A-ENST-16','A-ENST-17','A-ENST-2','A-ENST-3','A-ENST-4','A-ENST-5','A-ENST-6','A-ENST-7','A-ENST-8','A-ENST-9'));
-- Remove all existing HTS array designs apart from the placeholder: A-ENST-X
delete from A2_arraydesign where accession in 
('A-ENST-1','A-ENST-10','A-ENST-11','A-ENST-12','A-ENST-13','A-ENST-14','A-ENST-15','A-ENST-16','A-ENST-17','A-ENST-2','A-ENST-3','A-ENST-4','A-ENST-5','A-ENST-6','A-ENST-7','A-ENST-8','A-ENST-9');