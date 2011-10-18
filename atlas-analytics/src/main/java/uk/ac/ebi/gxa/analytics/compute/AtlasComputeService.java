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

package uk.ac.ebi.gxa.analytics.compute;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.R.AtlasRFactory;
import uk.ac.ebi.rcloud.server.RServices;

/**
 * Provides access to R computational infrastructure via an AtlasRFactory. To use, pass a {@link ComputeTask} to the
 * method {@link #computeTask(ComputeTask)}, the return type is determined by the type parameter to {@code
 * ComputeTask}.
 * <p/>
 * For example:
 * <code><pre>
 * RNumeric i = computeService.computeTask(new ComputeTask<RNumeric> () {
 *   public compute(RServices R) throws RemoteException {
 *     return (RNumeric) R.getObject("1 + 3");
 *   }
 * );
 * </pre></code>
 *
 * @author Misha Kapushesky
 * @author Tony Burdett
 */
public class AtlasComputeService {
    private AtlasRFactory atlasRFactory;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public void setAtlasRFactory(AtlasRFactory atlasRFactory) {
        this.atlasRFactory = atlasRFactory;
    }

    /**
     * Executes task on a borrowed worker. Returns type specified in generic type parameter T to the method.
     *
     * @param task task to evaluate, {@link ComputeTask}
     * @param <T>  type that the task returns on completion
     * @return T
     */
    public <T> T computeTask(ComputeTask<T> task) throws ComputeException {
        RServices rService = null;
        try {
            log.debug("Acquiring RServices");
            rService = atlasRFactory.createRServices();
            if(rService == null) {
                log.error("Can't create R service, so can't compute!");
                throw new ComputeException("Can't create R service, so can't compute!");
            }

            if(rService.getServantName() != null)
                log.debug("Computing on " + rService.getServantName());
            return task.compute(rService);
        }
        catch (ComputeException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Problem computing task!", e);
            throw new ComputeException(e);
        }
        finally {
            if (rService != null) {
                try {
                    log.debug("Recycling R service");
                    atlasRFactory.recycleRServices(rService);
                }
                catch (Exception e) {
                    log.error("Problem returning worker!", e);
                }
            }
        }
    }

    /**
     * Releases any resources that are retained by this AtlasComputeService
     */
    public void shutdown() {
        log.debug("Shutting down...");
        atlasRFactory.releaseResources();
    }
}
