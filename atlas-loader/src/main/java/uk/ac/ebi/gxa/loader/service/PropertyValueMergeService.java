package uk.ac.ebi.gxa.loader.service;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.CharacteristicsAttribute;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.sdrf.node.attribute.FactorValueAttribute;
import uk.ac.ebi.gxa.efo.Efo;
import uk.ac.ebi.gxa.efo.EfoTerm;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.gxa.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This service class deals with cross-factor merging of factor values at experiment load time.
 * It also deals with merging units into their corresponding factor values, if these units exist in EFO.
 *
 * @author Robert Petryszak
 */
public class PropertyValueMergeService {

    private final static Logger log = LoggerFactory.getLogger(PropertyValueMergeService.class);

    // These constants are used in merging compounds and doses together
    private static final String COMPOUND = "compound";
    private static final String DOSE = "dose";

    // Units containing the following values should never be pluralised when being joined to factor values
    private static final List<String> NON_PLURALISED_UNITS = Lists.newArrayList();

    static {
        NON_PLURALISED_UNITS.add("other");
        NON_PLURALISED_UNITS.add("percent");
        NON_PLURALISED_UNITS.add("molar");
    }

    // Units that should end in 'es' rather than 's' in their plural form
    private static final String INCH = "inch";

    // separator in units in which only the first work should be pluralised (e.g. "micromole per kilogram")
    private static final String PER = "per";
    // The only case other than the above in which only the first word should be pluralised (e.g. "degree celsius")
    private static final String DEGREE = "degree";

    // A temporary mapping from MAGE-OM to EFO - for certain units that for operational reasons cannot for the time being arrive
    // into Atlas as EFO units
    private static final Map<String, String> TRANSLATED_UNITS = Maps.newHashMap();

    static {
        TRANSLATED_UNITS.put("K", "kelvin");
        TRANSLATED_UNITS.put("degrees_C", "degree celsius");
        TRANSLATED_UNITS.put("degrees_F", "degree fahrenheit");
    }

    private Efo efo;

    public void setEfo(Efo efo) {
        this.efo = efo;
    }

    /**
     * @param unit
     * @return true if unit should be pluralised; false otherwise
     */
    private static boolean isPluralised(String unit) {
        for (String unitVal : NON_PLURALISED_UNITS) {
            if (unit.contains(unitVal))
                return false;
        }
        return true;
    }

    /**
     * Pluralise a unit only if:
     * - unit is not empty
     * - factor value it describes is not equal to 1
     * - it is not equal to OTHER or contains PERCENT in it
     * - it does not already end in "s"
     * <p/>
     * Pluralisation method is as follows:
     * - if a unit contains PER, pluralise the term preceding it (unless that term ends in "s" already)
     * - else if a unit starts with DEGREE, pluralise word DEGREE unless the unit starts with DEGREE + "s"
     * - else unless the units already ends in "s", pluralise thh whole unit.
     * <p/>
     * See the junit test case of this method for the full list of test cases.
     * <p/> c.f. examples of units in EFO in ticket 3356:
     * MAGE-OM_to_EFO_Units.txt
     * OtherUnitsMappedToEFO.txt
     *
     * @param unit
     * @param factorValue
     * @return
     */
    public static String pluraliseUnitIfApplicable(String unit, String factorValue) {
        try {
            if (Strings.isNullOrEmpty(factorValue) || Integer.parseInt(factorValue) == 1)
                return unit;
        } catch (NumberFormatException nfe) {
            // quiesce
        }

        if (!Strings.isNullOrEmpty(unit) && isPluralised(unit)) {
            int idx = unit.indexOf(PER);
            if (idx != -1)
                return pluralise(unit.substring(0, idx - 1).trim()) + " " + unit.substring(idx);
            else if (unit.startsWith(DEGREE) && !unit.equals(DEGREE + "s"))
                return pluralise(DEGREE) + unit.substring(DEGREE.length());
            else
                return pluralise(unit);
        }

        return unit;
    }

    /**
     * @param word
     * @return Word pluralised according to a naive pluralisation definition - this method does not implement an exhaustive
     * English language  pluralisation method but is designed to cover the universe of units used in Atlas only.
     */
    private static String pluralise(String word) {
        if (!word.endsWith("s")) {
            if (INCH.equals(word)) {
                return word + "es";
            } else
                return word + "s";
        }
        return word;
    }


    /**
     *
     * @param unit
     * @return an EFO term, corresponding to unit - if unit is a key in TRANSLATED_UNITS; else unit itself
     */
    private static String translateUnitToEFOIfApplicable(String unit) {
        if (TRANSLATED_UNITS.containsKey(unit.trim()))
            return TRANSLATED_UNITS.get(unit.trim());
        return unit;
    }

    /**
     * @param characteristicsAttribute
     * @return sample characteristic value contained in characteristicsAttribute (with an appended unit, if one exists)
     * @throws AtlasLoaderException - if UnitAttribute within characteristicsAttribute exists but has not value or if unit value cannot be found in EFO
     */
    public String getCharacteristicValueWithUnit(CharacteristicsAttribute characteristicsAttribute) throws AtlasLoaderException {
        String scValue = StringUtil.removeSeparatorDuplicates(characteristicsAttribute.getNodeName()).trim();
        if (!Strings.isNullOrEmpty(scValue) && characteristicsAttribute.unit != null) {
            String unitValue = translateUnitToEFOIfApplicable(characteristicsAttribute.unit.getAttributeValue());

            if (Strings.isNullOrEmpty(unitValue))
                throw new AtlasLoaderException("Unable to find unit value for sample characteristic value: " + scValue);
            if (!isEfoTerm(unitValue)) {
                throw new AtlasLoaderException("Unit: " + unitValue + " not found in EFO");
            }
            return Joiner.on(" ").join(scValue, PropertyValueMergeService.pluraliseUnitIfApplicable(unitValue.trim(), scValue));
        }
        return scValue;
    }


    /**
     * @param factorValueAttribute
     * @return factorValue contained in factorValueAttribute (with an appended unit, if one exists)
     * @throws AtlasLoaderException - if UnitAttribute within factorValueAttribute exists but has not value or if unit value cannot be found in EFO
     */
    private String getFactorValueWithUnit(FactorValueAttribute factorValueAttribute) throws AtlasLoaderException {
        String factorValueName = StringUtil.removeSeparatorDuplicates(factorValueAttribute.getNodeName()).trim();
        if (!Strings.isNullOrEmpty(factorValueName) && factorValueAttribute.unit != null) {
            String unitValue = translateUnitToEFOIfApplicable(factorValueAttribute.unit.getAttributeValue());

            if (Strings.isNullOrEmpty(unitValue))
                throw new AtlasLoaderException("Unable to find unit value for factor value: " + factorValueName);
            if (!isEfoTerm(unitValue)) {
                throw new AtlasLoaderException("Unit: " + unitValue + " not found in EFO");
            }
            return Joiner.on(" ").join(factorValueName, PropertyValueMergeService.pluraliseUnitIfApplicable(unitValue.trim(), factorValueName));
        }
        return factorValueName;
    }

    /**
     *
     * @param factorValueAttributes
     * @return equivalent of factorValueAttributes, but with all the relevant merge operations performed.
     * @throws AtlasLoaderException
     */
    public List<Pair<String, String>> getMergedFactorValues(List<Pair<String, FactorValueAttribute>> factorValueAttributes)
            throws AtlasLoaderException {
        String compoundFactorValue = null;
        String doseFactorValue = null;
        List<Pair<String, String>> factorValues = new ArrayList<Pair<String, String>>();
        for (Pair<String, FactorValueAttribute> pv : factorValueAttributes) {

            String ef = pv.getKey();
            String efv = getFactorValueWithUnit(pv.getValue());

            if (Strings.isNullOrEmpty(efv))
                continue; // We don't load empty factor values

            if (COMPOUND.equalsIgnoreCase(ef))
                compoundFactorValue = efv;
            else if (DOSE.equalsIgnoreCase(ef))
                doseFactorValue = efv;

            if (COMPOUND.equalsIgnoreCase(ef) || DOSE.equalsIgnoreCase(ef)) {
                if (!Strings.isNullOrEmpty(compoundFactorValue) && !Strings.isNullOrEmpty(doseFactorValue)) {
                    ef = COMPOUND;
                    efv = Joiner.on(" ").join(compoundFactorValue, doseFactorValue);
                    compoundFactorValue = null;
                    doseFactorValue = null;
                } else {
                    // Don't add either compound or dose factor values to assay on their own, until:
                    // - you have both of them, in which case merge them together and then add to assay
                    // - you know that either dose or compound values are missing, in which case add to assay that one
                    //   (dose or compound) that is present
                    continue;
                }
            }
            factorValues.add(Pair.create(ef, efv));
        }

        if (!Strings.isNullOrEmpty(compoundFactorValue) && Strings.isNullOrEmpty(doseFactorValue)) {
            factorValues.add(Pair.create(COMPOUND, compoundFactorValue));
            log.warn("Adding " + COMPOUND + " : " + compoundFactorValue + " to assay with no corresponding value for factor: " + DOSE);
        } else if (!Strings.isNullOrEmpty(doseFactorValue) && Strings.isNullOrEmpty(compoundFactorValue)) {
            throw new AtlasLoaderException(DOSE + " : " + doseFactorValue + " has no corresponding value for factor: " + COMPOUND);
        }

        return factorValues;
    }

    /**
     * @param term
     * @return true if term can be found in EFO; false otherwise
     */
    private boolean isEfoTerm(String term) {
        for (EfoTerm t : efo.searchTermPrefix(term)) {
            if (term.equals(t.getTerm()))
                return true;
        }
        return false;
    }
}
