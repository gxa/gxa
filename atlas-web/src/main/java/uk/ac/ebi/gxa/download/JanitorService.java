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

package uk.ac.ebi.gxa.download;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Olga Melnichuk
 */
public class JanitorService {

    private final ScheduledExecutorService schedulerService;
    private final long startDelay;
    private final long repeatInterval;
    private TimeUnit startDelayTimeUnit;
    private TimeUnit repeatIntervalTimeUnit;

    public JanitorService(ScheduledExecutorService schedulerService, long startDelay, long repeatInterval) {
        this.schedulerService = schedulerService;
        this.startDelay = startDelay;
        this.repeatInterval = repeatInterval;
    }

    public void setStartDelayTimeUnit(TimeUnit startDelayTimeUnit) {
        this.startDelayTimeUnit = startDelayTimeUnit;
    }

    public void setRepeatIntervalTimeUnit(TimeUnit repeatIntervalTimeUnit) {
        this.repeatIntervalTimeUnit = repeatIntervalTimeUnit;
    }

    public void schedule(Janitor janitor) {
        schedule(janitor, startDelay, startDelayUnit());
    }

    public void schedule(final Janitor janitor, long delay, TimeUnit timeUnit) {
        schedulerService.schedule(new Runnable() {
            @Override
            public void run() {
                if (janitor.keepOnCleaning()) {
                    schedule(janitor, repeatInterval, repeatIntervalUnit());
                }
            }
        }, delay, timeUnit);
    }

    private TimeUnit startDelayUnit() {
        return timeUnit(startDelayTimeUnit);
    }

    private TimeUnit repeatIntervalUnit() {
        return timeUnit(repeatIntervalTimeUnit);
    }

    private TimeUnit timeUnit(TimeUnit timeUnit) {
        return (timeUnit == null) ? TimeUnit.MICROSECONDS : timeUnit;
    }

    public interface Janitor {
        public boolean keepOnCleaning();
    }
}
