/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.tags.resourcebundle;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author Olga Melnichuk
 */
public interface WebResourceBundleConfig {

    /**
     * Loads resource bundle specs from a config file.
     *
     * @param configPath a path to the config file to parse
     * @throws WebResourceBundleConfigException
     *          if any error happened during the config loading
     */
    public void load(File configPath) throws WebResourceBundleConfigException;

    /**
     * Loads resource bundle specs from an input stream.
     *
     * @param in an InputStream to parse
     * @throws WebResourceBundleConfigException
     *          if any error happened during the config loading
     */
    public void load(InputStream in) throws WebResourceBundleConfigException;

    /**
     * Returns resources of given types for a specified bundle.
     *
     * @param bundleName    a bundle name to get resources for
     * @param resourceTypes types of resources to look for
     * @return a collection of web resources found
     * @throws WebResourceBundleConfigException
     *          if resource bundle is not configured
     */
    public Collection<WebResource> getResources(String bundleName, Collection<WebResourceType> resourceTypes) throws WebResourceBundleConfigException;

    /**
     * Checks if the resource bundle contains resources of required type
     *
     * @param bundleName a bundle name to check
     * @param type       type of web resource to check for existence
     * @return true if at least one resource of given type exists
     * @throws WebResourceBundleConfigException
     *          if the resource bundle is not configured
     */
    boolean hasResources(String bundleName, WebResourceType type) throws WebResourceBundleConfigException;
}
