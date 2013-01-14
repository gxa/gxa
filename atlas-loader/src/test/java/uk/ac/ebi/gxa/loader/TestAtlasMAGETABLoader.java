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

package uk.ac.ebi.gxa.loader;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import org.easymock.EasyMock;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.UnitAttribute;
import uk.ac.ebi.gxa.dao.AtlasDAOTestCase;
import uk.ac.ebi.gxa.dao.arraydesign.ArrayDesignService;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.dao.LoaderDAO;
import uk.ac.ebi.gxa.loader.service.PropertyValueMergeService;
import uk.ac.ebi.gxa.loader.steps.AssayAndHybridizationStep;
import uk.ac.ebi.gxa.loader.steps.CreateExperimentStep;
import uk.ac.ebi.gxa.loader.steps.ParsingStep;
import uk.ac.ebi.gxa.loader.steps.SourceStep;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.PropertyValue;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static uk.ac.ebi.microarray.atlas.model.Property.createProperty;

public class TestAtlasMAGETABLoader extends AtlasDAOTestCase {
    private static Logger log = LoggerFactory.getLogger(TestAtlasMAGETABLoader.class);

    private PropertyValueMergeService propertyValueMergeService = MockFactory.createPropertyValueMergeService();

    private ArrayDesignService arrayDesignServiceMock;

    private AtlasLoadCache cache;

    private URL parseURL;

    public void setUp() throws Exception {
        super.setUp();

        arrayDesignServiceMock = mock(ArrayDesignService.class);

        cache = new AtlasLoadCache();
        parseURL = this.getClass().getClassLoader().getResource("E-GEOD-3790.idf.txt");
    }

    public void tearDown() throws Exception {
        super.tearDown();
        cache = null;
    }

    @Test
    public void testParseAndCheckExperiments() throws AtlasLoaderException {
        log.debug("Running parse and check experiment test...");

        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        final Experiment expt = new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create());

        assertNotNull("Local cache doesn't contain an experiment", expt);
        assertEquals("Experiment is null", "E-GEOD-3790", expt.getAccession());
        log.debug("Experiment parse and check test done!");
    }

    @Test
    public void testAll() throws Exception {
        log.debug("Running parse and check experiment test...");

        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        final Experiment expt = new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create());

        cache.setExperiment(expt);
        final LoaderDAO dao = mockLoaderDAO();
        new SourceStep().readSamples(investigation, cache, dao, propertyValueMergeService);
        new AssayAndHybridizationStep().readAssays(investigation, cache, dao, arrayDesignServiceMock, propertyValueMergeService);

        log.debug("experiment.getAccession() = " + expt.getAccession());
        assertNotNull("Experiment is null", expt);
        assertEquals("Wrong experiment", "E-GEOD-3790", expt.getAccession());

        Set<String> referencedArrayDesigns = new HashSet<String>();
        for (Assay assay : cache.fetchAllAssays()) {
            if (!referencedArrayDesigns.contains(assay.getArrayDesign().getAccession())) {
                referencedArrayDesigns.add(assay.getArrayDesign().getAccession());
            }
        }
    }

    @Test
    public void testParseAndCheckSamplesAndAssays() throws AtlasLoaderException {
        log.debug("Running parse and check samples and assays test...");

        final MAGETABInvestigation investigation = new ParsingStep().parse(parseURL);
        cache.setExperiment(new CreateExperimentStep().readExperiment(investigation, HashMultimap.<String, String>create()));
        final LoaderDAO dao = mockLoaderDAO();
        new SourceStep().readSamples(investigation, cache, dao, propertyValueMergeService);
        new AssayAndHybridizationStep().readAssays(investigation, cache, dao, arrayDesignServiceMock, propertyValueMergeService);


        // parsing finished, look in our cache...
        assertNotSame("Local cache doesn't contain any samples",
                cache.fetchAllSamples().size(), 0);

        assertNotSame("Local cache doesn't contain any assays",
                cache.fetchAllAssays().size(), 0);

        log.debug("Parse and check sample/assays done");
    }

    @Test
    public void testUnitPluralisation() throws AtlasLoaderException {
        assertEquals("microgram", PropertyValueMergeService.pluraliseUnitIfApplicable("microgram", "1"));
        assertEquals("micrograms", PropertyValueMergeService.pluraliseUnitIfApplicable("microgram", "1.0"));
        assertEquals("other", PropertyValueMergeService.pluraliseUnitIfApplicable("other", " 5"));
        assertEquals("percent", PropertyValueMergeService.pluraliseUnitIfApplicable("percent", "5.0 "));
        assertEquals("volume percent", PropertyValueMergeService.pluraliseUnitIfApplicable("volume percent", "5 "));
        assertEquals("percent per volume", PropertyValueMergeService.pluraliseUnitIfApplicable("percent per volume", "5 "));
        assertEquals("nanograms per milliliter", PropertyValueMergeService.pluraliseUnitIfApplicable("nanogram per milliliter", "10"));
        assertEquals("nanograms per milliliter", PropertyValueMergeService.pluraliseUnitIfApplicable("nanograms per milliliter", "10"));
        assertEquals("nanogram per milliliter", PropertyValueMergeService.pluraliseUnitIfApplicable("nanogram per milliliter", "1"));
        assertEquals(null, PropertyValueMergeService.pluraliseUnitIfApplicable(null, "1"));
        assertEquals("nanogram per milliliter", PropertyValueMergeService.pluraliseUnitIfApplicable("nanogram per milliliter", null));
        assertEquals(null, PropertyValueMergeService.pluraliseUnitIfApplicable(null, null));
        assertEquals("cubic centimeters", PropertyValueMergeService.pluraliseUnitIfApplicable("cubic centimeter", "5"));
        assertEquals("parts per million", PropertyValueMergeService.pluraliseUnitIfApplicable("parts per million", "5"));
        assertEquals("degrees celsius", PropertyValueMergeService.pluraliseUnitIfApplicable("degree celsius", "5"));
        assertEquals("degrees", PropertyValueMergeService.pluraliseUnitIfApplicable("degree", "5"));
        assertEquals("degrees", PropertyValueMergeService.pluraliseUnitIfApplicable("degrees", "5"));
        assertEquals("degrees", PropertyValueMergeService.pluraliseUnitIfApplicable("degrees", "1"));
        assertEquals("International Units per mililiter", PropertyValueMergeService.pluraliseUnitIfApplicable("International Unit per mililiter", "5"));
        assertEquals("other", PropertyValueMergeService.pluraliseUnitIfApplicable("other", "5"));
        assertEquals("other", PropertyValueMergeService.pluraliseUnitIfApplicable("other", "5"));
        assertEquals("picomolar", PropertyValueMergeService.pluraliseUnitIfApplicable("picomolar", "5"));
        assertEquals("molar", PropertyValueMergeService.pluraliseUnitIfApplicable("molar", "5"));
        assertEquals("milligrams per kilogram", PropertyValueMergeService.pluraliseUnitIfApplicable("milligram per kilogram", "5"));
        assertEquals("inches", PropertyValueMergeService.pluraliseUnitIfApplicable("inch", "5"));
    }

    @Test
    public void testGetMergedFactorValues() throws AtlasLoaderException {
        List<Pair<String, String>> factorValues =
                propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("compound", mockFactorValueAttribute("tamoxifen", null))));
        assertEquals(factorValues.size(), 1);
        assertEquals("compound", factorValues.get(0).getKey());
        assertEquals("tamoxifen", factorValues.get(0).getValue());
    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedFactorValues1() throws AtlasLoaderException {
        propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("dose", mockFactorValueAttribute("5", null))));
        fail("AtlasLoaderException: 'dose : 5 has no corresponding value for factor: compound' should have been thrown");
    }

    @Test
    public void testGetMergedFactorValues2() throws AtlasLoaderException {
        List<Pair<String, FactorValueAttribute>> factorValueAttributes = Lists.newArrayList();
        factorValueAttributes.add(Pair.create("compound", mockFactorValueAttribute("tamoxifen", null)));
        factorValueAttributes.add(Pair.create("dose", mockFactorValueAttribute("5", "milligram")));
        List<Pair<String, String>> factorValues = propertyValueMergeService.getMergedFactorValues(factorValueAttributes);
        assertEquals(factorValues.size(), 1);
        assertEquals("compound", factorValues.get(0).getKey());
        assertEquals("tamoxifen 5 milligrams", factorValues.get(0).getValue());
    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedFactorValues3() throws AtlasLoaderException {
        List<Pair<String, FactorValueAttribute>> factorValueAttributes = Lists.newArrayList();
        factorValueAttributes.add(Pair.create("compound", mockFactorValueAttribute("tamoxifen", null)));
        factorValueAttributes.add(Pair.create("dose", mockFactorValueAttribute("5", "mg")));
        propertyValueMergeService.getMergedFactorValues(factorValueAttributes);
        fail("AtlasLoaderException: 'Unit: mg not found in EFO' should have been thrown");
    }

    @Test
    public void testGetMergedFactorValues4() throws AtlasLoaderException {
        List<Pair<String, String>> factorValues =
                propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("temperature", mockFactorValueAttribute("5", "degrees_C"))));
        assertEquals(factorValues.size(), 1);
        assertEquals("temperature", factorValues.get(0).getKey());
        assertEquals("5 degrees celsius", factorValues.get(0).getValue());
        factorValues =
                propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("temperature", mockFactorValueAttribute("5", " degrees_F"))));
        assertEquals(factorValues.size(), 1);
        assertEquals("temperature", factorValues.get(0).getKey());
        assertEquals("5 degrees fahrenheit", factorValues.get(0).getValue());
        factorValues =
                propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("temperature", mockFactorValueAttribute("5", " K"))));
        assertEquals(factorValues.size(), 1);
        assertEquals("temperature", factorValues.get(0).getKey());
        assertEquals("5 kelvins", factorValues.get(0).getValue());
    }

        @Test
    public void testGetMergedSampleCharacteristicValues1() throws AtlasLoaderException {
        assertEquals("5 milligrams", propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", "milligram")));
    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedSampleCharacteristicValues2() throws AtlasLoaderException {
        propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", "mg"));
    }

    @Test
    public void testGetMergedSampleCharacteristicValues3() throws AtlasLoaderException {
        assertEquals("5", propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", null)));
    }

    @Test
    public void testGetMergedFactorValues5() throws AtlasLoaderException {
        assertEquals("5 degrees celsius", propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", "degrees_C")));
        assertEquals("5 degrees fahrenheit", propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", " degrees_F")));
        assertEquals("5 kelvins", propertyValueMergeService.getCharacteristicValueWithUnit(mockSampleCharacteristicValueAttribute("5", " K")));
    }

    private LoaderDAO mockLoaderDAO() {
        final LoaderDAO dao = createMock(LoaderDAO.class);
        expect(dao.getOrCreatePropertyValue(EasyMock.<String>anyObject(), EasyMock.<String>anyObject()))
                .andReturn(new PropertyValue(null, createProperty("Test"), "test"))
                .anyTimes();
        expect(dao.getArrayDesignShallow("A-AFFY-33"))
                .andReturn(new ArrayDesign("A-AFFY-33"))
                .anyTimes();
        expect(dao.getArrayDesignShallow("A-AFFY-34"))
                .andReturn(new ArrayDesign("A-AFFY-34"))
                .anyTimes();
        replay(dao);
        return dao;
    }

    private CharacteristicsAttribute mockSampleCharacteristicValueAttribute(String characteristicAttributeValue, String unit) {
        final CharacteristicsAttribute characteristicsAttribute = createMock(CharacteristicsAttribute.class);
        characteristicsAttribute.unit = mockUnitAttribute(unit);
        expect(characteristicsAttribute.getNodeName()).andReturn(characteristicAttributeValue).once();
        replay(characteristicsAttribute);
        return characteristicsAttribute;
    }

    private FactorValueAttribute mockFactorValueAttribute(String factorValue, String unit) {
        final FactorValueAttribute factorValueAttribute = createMock(FactorValueAttribute.class);
        factorValueAttribute.unit = mockUnitAttribute(unit);
        expect(factorValueAttribute.getNodeName()).andReturn(factorValue).once();
        replay(factorValueAttribute);
        return factorValueAttribute;
    }

    private UnitAttribute mockUnitAttribute(String unit) {
        if (Strings.isNullOrEmpty(unit))
            return null;
        final UnitAttribute unitAttribute = createMock(UnitAttribute.class);
        expect(unitAttribute.getAttributeValue()).andReturn(unit).once();
        replay(unitAttribute);
        return unitAttribute;
    }
}
