/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.service.experiment;

import ae3.model.AtlasGene;
import ae3.service.experiment.BestDesignElementsResult;
import com.google.common.base.Function;
import org.codehaus.jackson.annotate.JsonProperty;
import uk.ac.ebi.microarray.atlas.model.UpDownExpression;

import javax.annotation.Nullable;
import java.util.Collection;

import static com.google.common.collect.Iterables.transform;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatPValue;
import static uk.ac.ebi.gxa.utils.NumberFormatUtil.formatTValue;

/**
 * @author Olga Melnichuk
 */
public class ExperimentAnalytics {

    private final BestDesignElementsResult bestResults;

    public ExperimentAnalytics(BestDesignElementsResult bestResults) {
        this.bestResults = bestResults;
    }

    public Iterable<TableRow> getRows() {
        return transform(bestResults, new Function<BestDesignElementsResult.Item, TableRow>() {
            @Override
            public TableRow apply(@Nullable BestDesignElementsResult.Item input) {
                return new TableRow(input);
            }
        });
    }

    public int size() {
        return bestResults.size();
    }

    public int getTotalSize() {
        return bestResults.getTotalSize();
    }

    public String getArrayDesignAccession() {
        return bestResults.getArrayDesignAccession();
    }

    public Collection<AtlasGene> getGenes() {
        return bestResults.getGenes();
    }

    public static class TableRow {
        private final String geneName;
        private final String geneIdentifier;
        private final String deAccession;
        private final Integer deIndex;
        private final String factor;
        private final String factorValue;
        private final UpDownExpression upDown;
        private final float pValue;
        private final float tValue;

        public TableRow(BestDesignElementsResult.Item item) {
            this(item.getGene().getGeneName(),
                    item.getGene().getGeneIdentifier(),
                    item.getDeAccession(),
                    item.getDeIndex(),
                    item.getEf(),
                    item.getEfv(),
                    item.getPValue(),
                    item.getTValue(),
                    UpDownExpression.valueOf(item.getPValue(), item.getTValue()));
        }

        public TableRow(String geneName,
                        String geneIdentifier,
                        String deAccession,
                        Integer deIndex,
                        String factor,
                        String factorValue,
                        float pValue,
                        float tValue,
                        UpDownExpression upDown) {
            this.geneName = geneName;
            this.geneIdentifier = geneIdentifier;
            this.deAccession = deAccession;
            this.deIndex = deIndex;
            this.factor = factor;
            this.factorValue = factorValue;
            this.pValue = pValue;
            this.tValue = tValue;
            this.upDown = upDown;
        }

        @JsonProperty("geneName")
        public String getGeneName() {
            return geneName;
        }

        @JsonProperty("geneIdentifier")
        public String getGeneIdentifier() {
            return geneIdentifier;
        }

        @JsonProperty("deAcc")
        public String getDeAccession() {
            return deAccession;
        }

        @JsonProperty("deIndex")
        public Integer getDeIndex() {
            return deIndex;
        }

        @JsonProperty("ef")
        public String getFactor() {
            return factor;
        }

        @JsonProperty("efv")
        public String getFactorValue() {
            return factorValue;
        }

        @JsonProperty("upDown")
        public String getUpDown() {
            return upDown.toString();
        }

        @JsonProperty("pVal")
        public String getPValue() {
            return formatPValue(pValue);
        }

        @JsonProperty("tVal")
        public String getTValue() {
            return formatTValue(tValue);
        }

        public float getFloatPValue() {
            return pValue;
        }

        public float getFloatTValue() {
            return tValue;
        }
    }
}
