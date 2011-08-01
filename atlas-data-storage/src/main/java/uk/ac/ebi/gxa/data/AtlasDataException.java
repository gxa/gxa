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

/*
 * This exception should be trown outside of the package
 * if any internal error is occured during Data reading/writing.
 *
 * IOException or any specific exception from external libraries
 * (e.g. java netcdf library) should never be thrown outside of
 * atlas-data-storage module.
 */ 

public class AtlasDataException extends Exception {
    public AtlasDataException(String message) {
        super(message);
    }

    public AtlasDataException(Exception cause) {
        super(cause);
    }

    public AtlasDataException(String message, Exception cause) {
        super(message, cause);
    }
}
