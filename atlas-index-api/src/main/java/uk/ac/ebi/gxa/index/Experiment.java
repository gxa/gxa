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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.index;

import java.io.Serializable;

/**
 * @author pashky
 */
public class Experiment implements Serializable, Comparable<Experiment> {
    private Expression expression;
    private long id;
    private String ef;
    private String efv;
    private String[] efo;
    private double pvalue;
    private static final long serialVersionUID = 1L;

    Experiment(Expression expression, long id, String ef, String efv, String[] efo, double pvalue) {
        this.expression = expression;
        this.id = id;
        this.ef = ef;
        this.efv = efv;
        this.efo = efo;
        this.pvalue = pvalue;
    }

    public Expression getExpression() {
        return expression;
    }

    public long getId() {
        return id;
    }

    public String getEf() {
        return ef;
    }

    public String getEfv() {
        return efv;
    }

    public String[] getEfo() {
        return efo;
    }

    public double getPvalue() {
        return pvalue;
    }

    public int compareTo(Experiment o) {
        return Double.valueOf(o.pvalue).compareTo(pvalue);
    }
}
