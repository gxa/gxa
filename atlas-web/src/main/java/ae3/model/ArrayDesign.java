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

package ae3.model;

import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import javax.annotation.Nonnull;

/**
 * Class, representing array design for {@link ae3.model.ExperimentalData} object
 * Is used only in NetCDFReader and should be replaced with newer model class.
 *
 * @author pashky
 */
public class ArrayDesign {
    private String accession;

    /**
     * Constructor
     * @param accession array design accession string
     */
    public ArrayDesign(@Nonnull String accession) {
        this.accession = accession;
    }

    /**
     * Gets accession string
     * @return accession string
     */
    @RestOut(name="accession")
    public String getAccession() {
        return accession;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ArrayDesign that = (ArrayDesign) o;

        if (!accession.equals(that.accession)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return accession.hashCode();
    }

    @Override
    public String toString() {
        return "ArrayDesign{" + accession + '}';
    }
}
