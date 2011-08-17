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

package uk.ac.ebi.gxa.analytics.generator;

/**
 * An exception that occurs whenever something went wrong during
 * analytics generation
 *
 * @author Tony Burdett
 */
public class AnalyticsGeneratorException extends Exception {

  public AnalyticsGeneratorException(String s) {
    super(s);
  }

  public AnalyticsGeneratorException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public AnalyticsGeneratorException(Throwable throwable) {
    super(throwable);
  }
}
