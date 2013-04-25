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

package uk.ac.ebi.gxa.annotator.loader;

import org.apache.http.client.HttpClient;
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.genesig.FileBasedAnnotationLoader;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;

import java.io.IOException;

/**
 * User: nsklyar
 * Date: 05/12/2011
 */
public class FileBasedAnnotator extends Annotator {

    private final FileBasedAnnotationSource annSrc;

    private final HttpClient httpClient;

    public FileBasedAnnotator(FileBasedAnnotationSource annSrc, AtlasBioEntityDataWriter beDataWriter, HttpClient httpClient) {
        super(beDataWriter);
        this.annSrc = annSrc;
        this.httpClient = httpClient;
    }

    @Override
    public void updateAnnotations(int batchSize) {
        try {
            reportProgress("Loading properties from annotation source " + annSrc.getName());
            FileBasedAnnotationLoader loader = new FileBasedAnnotationLoader(httpClient, annSrc);
            loader.loadPropertyValues();

            reportProgress("Writing properties from annotation source " + annSrc.getName());
            writePropertyValues(loader.getPropertyValuesData(), annSrc, true, batchSize);

            reportSuccess("Update annotations from Annotation Source " + annSrc.getName() + " completed");
        } catch (AnnotationException e) {
            reportError(e);
        } catch (InvalidAnnotationDataException e) {
            reportError(e);
        } catch (IOException e) {
            reportError(e);
        } catch (InvalidCSVColumnException e) {
            reportError(e);
        }
    }

    @Override
    public void updateMappings(int batchSize) {
        //ToDo: implement this method if we file based annotation source with mappings
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " doesn't support method updateMappings ");
    }

}
