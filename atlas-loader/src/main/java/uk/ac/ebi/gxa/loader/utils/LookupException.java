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

package uk.ac.ebi.gxa.loader.utils;

/**
 * An exception that is thrown whenever an attempt to retrieve an object from
 * the cache of objects fails unexpectedly.  This should <b>NOT</b> be thrown if
 * the object is not yet in the cache, rather it should be thrown if somehting
 * goes critically wrong whilst trying to query for an object that is in the
 * cache already.
 *
 * @author Tony Burdett
 * @date 27-Aug-2009
 */
public class LookupException extends Exception {
  public LookupException() {
    super();
  }

  public LookupException(String s) {
    super(s);
  }

  public LookupException(String s, Throwable throwable) {
    super(s, throwable);
  }

  public LookupException(Throwable throwable) {
    super(throwable);
  }
}
