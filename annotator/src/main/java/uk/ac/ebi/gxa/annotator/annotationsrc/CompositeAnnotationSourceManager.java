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

package uk.ac.ebi.gxa.annotator.annotationsrc;


import com.google.common.base.Joiner;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.loader.util.HttpClientHelper;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.validation.ValidationReportBuilder;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
public class CompositeAnnotationSourceManager {

    private List<AbstractAnnotationSourceManager<? extends AnnotationSource>> managers;

    @Autowired
    private SoftwareManager softwareManager;

    @Autowired
    private HttpClient httpClient;

    public static final String SEPARATOR = "\n$$$\n";

    public CompositeAnnotationSourceManager(List<AbstractAnnotationSourceManager<? extends AnnotationSource>> managers) {
        this.managers = managers;
    }

    public Collection<AnnotationSource> getAnnotationSourcesBySoftware(Software software) {
        return softwareManager.getAnnotationSourcesBySoftware(software);
    }

    public List<Software> getAllSoftware() {
        List<Software> result = new ArrayList<Software>();
        result.addAll(softwareManager.getAllSoftware());
        for (AbstractAnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            result.addAll(manager.getNewVersionSoftware());
        }
        return result;
    }

    public void validateProperties(AnnotationSource annSrc, ValidationReportBuilder reportBuilder) {
        findManager(annSrc).validateProperties(annSrc, reportBuilder);
    }

    public AnnotationSource getAnnSrc(long id, AnnotationSourceType type) throws RecordNotFoundException {
        return findManager(type).getAnnSrc(id);
    }

    public String getAnnSrcString(long id, AnnotationSourceType type) throws RecordNotFoundException {
        return findManager(type).getAnnSrcString(id);
    }

    public ValidationReportBuilder validateAndSaveAnnSrc(long annSrcId, String text, AnnotationSourceType type) {
        return findManager(type).validateAndSaveAnnSrc(annSrcId, text);
    }

    public String getLatestAnnotationSourcesAsText() {
        Collection<String> sourceStrings = new ArrayList<String>();
        for (AbstractAnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            sourceStrings.add(manager.getLatestAnnotationSourcesAsText(SEPARATOR));
        }
        return Joiner.on(SEPARATOR).join(sourceStrings);
    }

    public ValidationReportBuilder updateLatestAnnotationSourcesFromUrl(String url) {
        final ValidationReportBuilder errors = new ValidationReportBuilder();
        try {
            final InputStream inputStream = HttpClientHelper.httpGet(httpClient, URI.create(url));
            updateLatestAnnotationSources(IOUtils.toString(inputStream));
        } catch (IOException e) {
            errors.addMessage("Cannot update annotation sources from URL " + url + "Error: " + e.getMessage());
        }
        return errors;
    }

    protected ValidationReportBuilder updateLatestAnnotationSources(String text) {
        final ValidationReportBuilder errors = new ValidationReportBuilder();
        for (AbstractAnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            manager.updateLatestAnnotationSources(text, SEPARATOR, errors);
        }
        return errors;
    }

    public Software getSoftware(long softwareId) throws RecordNotFoundException {
        return softwareManager.getSoftware(softwareId);
    }

    public Software activateSoftware(long softwareId) throws RecordNotFoundException {
        return softwareManager.activateSoftware(softwareId);
    }

    private AbstractAnnotationSourceManager<? extends AnnotationSource> findManager(AnnotationSource annSrc) {
        return findManager(annSrc.getClass());
    }

    private AbstractAnnotationSourceManager<? extends AnnotationSource> findManager(AnnotationSourceType type) {
        return findManager(type.getClazz());
    }

    private AbstractAnnotationSourceManager<? extends AnnotationSource> findManager(Class<? extends AnnotationSource> clazz) {
        for (AbstractAnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(clazz)) {
                return manager;
            }
        }
        throw new IllegalArgumentException("Annotation source manager is not available for class: " + clazz);
    }
}
