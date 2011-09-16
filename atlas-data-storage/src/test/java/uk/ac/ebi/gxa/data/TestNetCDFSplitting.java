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
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.data;

import junit.framework.TestCase;

import java.io.File;
import java.util.*;

import uk.ac.ebi.gxa.utils.FileUtil;

import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.microarray.atlas.model.Experiment;

public class TestNetCDFSplitting extends TestCase {
	private File baseDirectory;
	private File tempDirectory;
    private AtlasDataDAO atlasDataDAO;

	private final String[] assays33 = {
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02083092b_Skeletal_Muscle_Psoas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02040823_salivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022758_thymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02051713_Testi_SeminiferousTubule.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022633_fetal liver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022867_Pituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02022205_Brain Amygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022750_fetal liver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02050806_Ovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052309_Islet.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022639_trachea.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022228_ fetal brain.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02053111_PrefrontalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052320_WHOLEBLOOD(JJV).CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052305_TestiIntersitial.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060514_OlfactoryBulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052312_HUMANCULTUREDADIPOCYTE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052302_Ovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02051002_AdrenalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02050802_AdrenalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02053107_CardiacMyocytes.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02030704_Prostate.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052323_FETALTHYROID.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS0207263HB_PLACENTA.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022764_cerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02030702_Heart.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080772b_atrioventricular_node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060604_CardiacMyocytes.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052114_TestiSeminiferousTubule.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02022204_Thyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02061907_Hypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060417_IsletCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02030703_Uterus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02072570_HUMANCULTUREDADIPOCYTE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022865_bone marrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02081483b_Trigeminal_Ganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02031411_thymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02041226_salivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02083092a_Skeletal_Muscle_Psoas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080776b_skin.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS0207253IA_PLACENTA.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02082987A_TONGUE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060506_Hypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022643_kidney.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02082805_PB_CD8TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022224_  spinal cord.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052108_Testi-GermCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02053109_PrefrontalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02022604_Thyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02021911_ PROSTATE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02022605_Brain Amygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02021915_ LIVER.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022107_  spinal cord.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02082803_PB_CD4TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJW02021805_lung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02030706_Liver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02083089a_Uterus_Corpus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02030705_Lung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02030474_bonemarrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022647_cerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02081483a_Trigeminal_Ganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02031304_fetallung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052318_WHOLEBLOOD(JJV).CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02053103_HBEC.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022760_kidney.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022645_adrenal gland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022872_ fetal lung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080736f_DRG.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02083089b_Uterus_Corpus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052217_fetalThyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02082987B_TONGUE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02051709_Testi_Intersitial.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02021909_HEART.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080772a_atrioventricular_node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02021913_ UTERUS.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02072561_OlfactoryBulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02030476_pituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060502_Pancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022635_testis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022115_  lymph node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02053101_HBEC.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AMH02082804_PB_CD4TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080776a_skin.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022111_ fetal brain.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022232_  lymph node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3ARS02080736e_DRG.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02060401_Pancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02051711_Testi_LeydigCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02052112_TestiLeydigCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022762_adrenal gland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02051707_Testi_GermCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022752_testis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.3AJZ02022756_trachea.CHP.DerivedBioAssay"
	};

	private final String[] assays39 = {
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212025Aprostate.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212072Bsubstantianigra.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207054Bheart.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228087Bembryoday9.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311030Asnoutepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030211062Bamygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211001Badiposetissue.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212027Asalivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212027Bsalivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312083Ab220+bcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207007Acerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207020Blymphnode.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212055Aepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030401084Bembryoday6.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228086Aembryoday8.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207007Bcerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211018Aliver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212009Afrontalcortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228091Bembryoday10.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212008Bcerebralcortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312066Aplacenta.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312066Bplacenta.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030211019Blung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212072Asubstantianigra.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212060Apancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311038Atongueepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228091Aembryoday10.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312023Aovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030228080Apituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312023Bovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211002Aadrenalgland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312065Bdorsalrootganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030401094Apreoptic.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312028Askeletalmuscle.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212061Abrownfat.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212014Ahippocampus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311036Athymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312056Abonemarrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312082Bcd8+Tcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212053Bhypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212070Aolfactorybulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212070Bolfactorybulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212060Bpancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212057Bspinalcordlower.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211018Bliver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207020Alymphnode.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311039Btrachea.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312083Bb220+bcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207054Aheart.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212037Athyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228086Bembryoday8.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207135Atestis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311036Bthymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211071Adorsalstriatum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228087Aembryoday9.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228085Bembryoday7.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212037Bthyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312056Bbonemarrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212053Ahypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212061Bbrownfat.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212040Btrigeminal.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207041Buterus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212058Bspinalcordupper.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207135Btestis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030228080Bpituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212040Atrigeminal.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311038Btongueepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212025Bprostate.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211016Bkidney.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312028Bskeletalmuscle.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030402094Bpreoptic.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312065Adorsalrootganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030211019Alung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312081Acd4+Tcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212057Aspinalcordlower.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030207041Auterus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211002Badrenalgland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212055Bepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312082Acd8+Tcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030212058Aspinalcordupper.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212009Bfrontalcortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211071Bdorsalstriatum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030211001Aadiposetissue.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030211062Aamygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311030Bsnoutepidermis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030312081Bcd4+Tcell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312008Acortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGJZ030212014Bhippocampus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228085Aembryoday7.5.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030312016Akidney.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030311039Atrachea.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.MGMH030228084Aembryoday6.5.CHP.DerivedBioAssay"
	};

	private final String[] assays40 = {
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022755_trachea.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052311_HUMANCULTUREDADIPOCYTE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ022987B_TONGUE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02051710_Testi_Intersitial.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02053108_CardiacMyocytes.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022223_spinalcord.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022644_kidney.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02083008_PB_CD4TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022646_adrenalgland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022105_Brain Amygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022227_fetalbrain.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022866_bonemarrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052109_TestiIntersitial.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ022987A_TONGUE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022751_testis.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02081483b_Trigeminal_Ganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022811_Lung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022634_fetalliver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060511_OlfactoryBulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02080872b_atrioventricular_node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02061205_fetalThyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS0207253IB_PLACENTA.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022805_Lung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052321_WHOLEBLOOD(JJV).CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060406_Hypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02050809_AdrenalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052208_Ovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022108_spinalcord.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022804_Prostate.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02083005_PB_CD8TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02083092b_Skeletal_Muscle_Psoas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022704_Thyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02030601_Prostate.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052111_TestiLeydigCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02053104_HBEC.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022648_cerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022808_Heart.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052308_Islet.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022809_Uterus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022231_lymphnode.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060515_IsletCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022112_fetalbrain.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060501_Pancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02030473_bonemarrow.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02031305_fetallung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02083092a_Skeletal_Muscle_Psoas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022749_fetalliver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022705_Brain Amygdala.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02083007_PB_CD4TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02081483a_Trigeminal_Ganglion.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02053110_PrefrontalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02030684_KIDNEY.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022702_Pancreas.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02061809_pituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022642_thymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052319_WHOLEBLOOD(JJV).CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02081536e_DRG.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052201_AdrenalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052107_TestiGermCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02080876b_skin.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02051714_Testi_SeminiferousTubule.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022640_trachea.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022104_thyroid.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022761_adrenalgland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02061909_pituitary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022806_Liver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02080876a_skin.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052113_TestiSeminiferousTubule.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02072563_Hypothalamus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS0207253HB_PLACENTA.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022757_thymus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02053112_PrefrontalCortex.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02083006_PB_CD8TCells.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060602_CardiacMyocytes.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02050813_Ovary.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02083089b_Uterus_Corpus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02040822_salivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02041227_salivarygland.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02081536f_DRG.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022802_Heart.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022116_lymphnode.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02083089a_Uterus_Corpus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02051708_Testi_GermCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02053102_HBEC.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022114_fetallung.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02051712_Testi_LeydigCell.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022803_Uterus.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02060414_OlfactoryBulb.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BRS02080872a_atrioventricular_node.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052324_FETALTHYROID.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02052313_HUMANCULTUREDADIPOCYTE.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BMH02022812_Liver.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022763_cerebellum.CHP.DerivedBioAssay",
        "ebi.ac.uk:MAGETabulator:E-MTAB-25.1BJZ02022636_testis.CHP.DerivedBioAssay"
	};

    @Override
    protected void setUp() throws Exception {
        atlasDataDAO = new AtlasDataDAO();
        baseDirectory = new File(getClass().getClassLoader().getResource("").getPath());
		tempDirectory = FileUtil.createTempDirectory("atlas-test");
		final File baseExperimentDirectory = new File(baseDirectory.getAbsolutePath() + "/MTAB/00/E-MTAB-25");
		final File experimentDirectory = new File(tempDirectory.getAbsolutePath() + "/MTAB/00/E-MTAB-25");
		experimentDirectory.mkdirs();
		for (String name : new String[] { "E-MTAB-25_A-AFFY-33.nc", "E-MTAB-25_A-AFFY-39.nc", "E-MTAB-25_A-AFFY-40.nc" }) {
			FileUtil.copyFile(new File(baseExperimentDirectory, name), new File(experimentDirectory, name));
		}
		
        atlasDataDAO.setAtlasDataRepo(tempDirectory);
    }

    @Override
    protected void tearDown() throws Exception {
		//FileUtil.deleteDirectory(tempDirectory);
    }

    public void testSplitting() {
		try {
            final Experiment experiment = new Experiment(411512559L, "E-MTAB-25");
        
			final List<Assay> assays = new LinkedList<Assay>();
			final List<Sample> samples = new LinkedList<Sample>();
        
			final ArrayDesign a33 = new ArrayDesign("A-AFFY-33");	
			for (String accession : assays33) {
                final Assay a = new Assay(accession);
                final Sample s = new Sample(accession);
                a.setArrayDesign(a33);
				assays.add(a);
				samples.add(s);
				s.addAssay(a);
			}
        
			final ArrayDesign a39 = new ArrayDesign("A-AFFY-39");	
			for (String accession : assays39) {
                final Assay a = new Assay(accession);
                final Sample s = new Sample(accession);
                a.setArrayDesign(a39);
				assays.add(a);
				samples.add(s);
				s.addAssay(a);
			}
        
			final ArrayDesign a40 = new ArrayDesign("A-AFFY-40");	
			for (String accession : assays40) {
                final Assay a = new Assay(accession);
                final Sample s = new Sample(accession);
                a.setArrayDesign(a40);
				assays.add(a);
				samples.add(s);
				s.addAssay(a);
			}
        
            experiment.setAssays(assays);
            experiment.setSamples(samples);
        
			atlasDataDAO.createExperimentWithData(experiment).updateAllData();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
