package uk.ac.ebi.gxa.loader;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.junit.Test;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.UnitAttribute;
import uk.ac.ebi.gxa.loader.service.PropertyValueMergeService;
import uk.ac.ebi.gxa.utils.Pair;

import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestPropertyValueMergeService {

    PropertyValueMergeService propertyValueMergeService = MockFactory.createPropertyValueMergeService();

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
        factorValues =
                propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("irradiate", mockFactorValueAttribute("xray", null))));
        assertEquals(factorValues.size(), 1);
        assertEquals("irradiate", factorValues.get(0).getKey());
        assertEquals("xray", factorValues.get(0).getValue());
    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedFactorValues1() throws AtlasLoaderException {
        propertyValueMergeService.getMergedFactorValues(Collections.singletonList(Pair.create("dose", mockFactorValueAttribute("5", null))));
        fail("AtlasLoaderException: 'dose : 5 has no corresponding value for any of the following factors: [compound, irradiate]' should have been thrown");
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

    @Test
    public void testGetMergedFactorValues6() throws AtlasLoaderException {
        List<Pair<String, FactorValueAttribute>> factorValueAttributes = Lists.newArrayList();
        factorValueAttributes.add(Pair.create("irradiate", mockFactorValueAttribute("xray", null)));
        factorValueAttributes.add(Pair.create("dose", mockFactorValueAttribute("5", "becquerel")));
        List<Pair<String, String>> factorValues = propertyValueMergeService.getMergedFactorValues(factorValueAttributes);
        assertEquals(factorValues.size(), 1);
        assertEquals("irradiate", factorValues.get(0).getKey());
        assertEquals("xray 5 becquerels", factorValues.get(0).getValue());
    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedFactorValues3() throws AtlasLoaderException {
        List<Pair<String, FactorValueAttribute>> factorValueAttributes = Lists.newArrayList();
        factorValueAttributes.add(Pair.create("compound", mockFactorValueAttribute("tamoxifen", null)));
        factorValueAttributes.add(Pair.create("dose", mockFactorValueAttribute("5", "mg")));
        propertyValueMergeService.getMergedFactorValues(factorValueAttributes);
        fail("AtlasLoaderException: 'Unit: mg not found in EFO' should have been thrown");

    }

    @Test(expected = AtlasLoaderException.class)
    public void testGetMergedFactorValues7() throws AtlasLoaderException {
        List<Pair<String, FactorValueAttribute>> factorValueAttributes = Lists.newArrayList();
        factorValueAttributes.add(Pair.create("irradiate", mockFactorValueAttribute("xray", null)));
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
