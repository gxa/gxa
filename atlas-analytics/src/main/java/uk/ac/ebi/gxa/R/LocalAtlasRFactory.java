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

package uk.ac.ebi.gxa.R;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.rcloud.server.DirectJNI;
import uk.ac.ebi.rcloud.server.RServices;

/**
 * A concrete implementation of {@link uk.ac.ebi.gxa.R.AtlasRFactory} that generates RServices that run on the local
 * machine.  As local R installations are not thread-safe, only one computation can be calculated at a time.  Therefore,
 * createRServices() will release at most one RService for use at any one time, and repeat requests will block until the
 * previously acquired RService has been released.
 *
 * Please refer to the http://www.rforge.net/JRI/ on how to configure environment for running local R services.
 * Q:  I get the following error, what's wrong?
 *     java.lang.UnsatisfiedLinkError: no jri in java.library.path
 *
 * A:  Usually it means that you didn't setup the necessary environment variables properly or the JRI library is not
 *     where it is expected to be. The recommended way to start JRI programs is to use the run script which is
 *     generated along with the library. It sets everything up and is tested to work. If you want to write your
 *     own script or launcher, you must observe at least the following points:
 *
 *     + R_HOME must be set correctly
 *
 *     + (Windows): The directory containing R.dll must be in your PATH
 *
 *     + (Mac): Well, it's a Mac, so it just works ;).
 *
 *     + (unix): R must be compiled using --enable-R-shlib and the directory containing libR.so must be in
 *       LD_LIBRARY_PATH. Also libjvm.so and other dependent Java libraries must be on LD_LIBRARY_PATH.
 *
 *     + JRI library must be in the current directory or any directory listed in java.library.path. Alternatively
 *       you can specify its path with -Djava.library.path= when starting the JVM. When you use the latter,
 *       make sure you check java.library.path property first such that you won't break your Java.
 *
 * @author Tony Burdett
 */
public class LocalAtlasRFactory implements AtlasRFactory {

    private static final String JAVA_LIBRARY_PATH = "java.library.path";
    private static final String R_HOME = "R_HOME";


    private final Logger log = LoggerFactory.getLogger(getClass());

    public boolean validateEnvironment() {
        final String envRHome = System.getenv(R_HOME);
        log.info(R_HOME + ": " + envRHome);

        if (Strings.isNullOrEmpty(envRHome)) {
            log.error("No " + R_HOME + " environment variable found. Can not start JNI bridge to R");
            return false;
        }

        String libPath = System.getProperty(JAVA_LIBRARY_PATH);
        log.info(JAVA_LIBRARY_PATH + ": " + libPath);

        if (!libPath.contains("rJava") || !libPath.contains("jri")) {
            log.warn("JRI path probably not set. Check your " + JAVA_LIBRARY_PATH + ": " + libPath);
            return false;
        }

        // check R install actually works
        try {
            RServices r = createRServices();
            recycleRServices(r);
        } catch (Throwable e) {
            log.error("Critical R whilst trying to bridge to local R install - " +
                    "check R is installed and required libraries present", e);
            return false;
        }
        return true;
    }

    public RServices createRServices() {
        log.info("Creating R services..");
        // create a R service - DirectJNI gets an R service on the local machine
        return DirectJNI.getInstance().getRServices();
    }

    public void recycleRServices(RServices rServices) {
    }

    public void releaseResources() {
        log.info("Releasing resources...");
        try {
            DirectJNI.getInstance()._rEngine.end();
//            DirectJNI.getInstance()._rEngine.eval("quit('no')");
        } catch (Throwable t) {
            log.error("Error", t);
        }
    }
}
