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

package uk.ac.ebi.gxa.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.dao.AtlasDAO;

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Oracle DB-backed storage for {@link TaskManager} class
 * @author pashky
 */
public class DbStorage implements PersistentStorage {
    private Logger log = LoggerFactory.getLogger(getClass());
    private JdbcTemplate jdbcTemplate;

    public void setDao(AtlasDAO dao) {
        this.jdbcTemplate = dao.getJdbcTemplate();
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static String decodeAccession(String accession) {
        return " ".equals(accession) ? "" : accession;
    }

    private static String encodeAccession(String accession) {
        return "".equals(accession) ? " " : accession;
    }


    public long getNextTaskId() {
        return jdbcTemplate.queryForLong("SELECT A2_TASKMAN_TASKID_SEQ.NEXTVAL FROM DUAL");
    }

    public void updateTaskStatus(TaskSpec task, TaskStatus status) {
        try {
            if(jdbcTemplate.update(
                    "MERGE INTO A2_TASKMAN_STATUS ts USING DUAL ON (ts.type = :1 and ts.accession = :2) " +
                            "WHEN MATCHED THEN UPDATE SET status = :3 " +
                            "WHEN NOT MATCHED THEN INSERT (type,accession,status) values (:4, :5, :6)",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            status.toString(),
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            status.toString()
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task stage " + task + " " + status, e);
        }
    }

    public TaskStatus getTaskStatus(TaskSpec task) {
        try {
            return TaskStatus.valueOf(jdbcTemplate.queryForObject(
                    "SELECT status FROM A2_TASKMAN_STATUS ts WHERE ts.type = :1 AND ts.accession = :2",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession())
                    }, String.class).toString());
        } catch (EmptyResultDataAccessException e) {
            return TaskStatus.NONE;
        } catch (DataAccessException e) {
            log.error("Can't retrieve task stage " + task, e);
            return TaskStatus.NONE;
        }
    }

    public void logTaskEvent(Task task, TaskEvent event, String message) {
        try {
            if(jdbcTemplate.update(
                    "INSERT INTO A2_TASKMAN_LOG (TASKID, TYPE, ACCESSION, RUNMODE, USERNAME, EVENT, MESSAGE) VALUES (?,?,?,?,?,?,?)",
                    new Object[] {
                            task.getTaskId(),
                            task.getTaskSpec().getType(),
                            encodeAccession(task.getTaskSpec().getAccession()),
                            task.getRunMode() == null ? "" : task.getRunMode().toString(),
                            task.getUser().getUserName(),
                            event.toString(),
                            message == null ? "" : message
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task stage log " + task + " " + event + " " + message, e);
        }
    }


    public void addTag(Task task, TaskTagType type, String tag) {
        try {
            if(jdbcTemplate.update(
                    "MERGE INTO A2_TASKMAN_TAG t " +
                            "USING (SELECT NVL(TT.TASKCLOUDID, ?) AS TASKCLOUDID, ? AS TAGTYPE, ? AS TAG FROM DUAL LEFT JOIN A2_TASKMAN_TAGTASKS tt ON tt.TASKID=?) nt " +
                            "ON (nt.TAGTYPE=t.TAGTYPE AND nt.TAG=t.TAG AND nt.TASKCLOUDID=t.TASKCLOUDID) " +
                            "WHEN NOT MATCHED THEN INSERT (TASKCLOUDID, TAGTYPE, TAG) VALUES (nt.TASKCLOUDID, nt.TAGTYPE, nt.TAG)",
                    new Object[] {
                            task.getTaskId(), type.toString().toLowerCase(), tag, task.getTaskId()
                    }) > 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task " + task.getTaskId() + " tag " + tag, e);
        }
    }

    public void joinTagCloud(Task existingTask, Task newTaskId) {
        try {
            if(jdbcTemplate.update(
                    "MERGE INTO A2_TASKMAN_TAGTASKS t " +
                            "USING (SELECT NVL(tt.TASKCLOUDID, ?) AS TASKCLOUDID, ? AS TASKID FROM DUAL LEFT JOIN A2_TASKMAN_TAGTASKS tt ON tt.TASKID=?) nt " +
                            "ON (nt.TASKID=t.TASKID AND nt.TASKCLOUDID=t.TASKCLOUDID) " +
                            "WHEN NOT MATCHED THEN INSERT (TASKCLOUDID, TASKID) VALUES (nt.TASKCLOUDID, nt.TASKID)",
                    new Object[] {
                            existingTask.getTaskId(), newTaskId.getTaskId(), existingTask.getTaskId()
                    }) > 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task " + newTaskId.getTaskId() + " in cloud of " + existingTask.getTaskId(), e);
        }
    }

    public static class TaskEventLogItem {
        public final TaskSpec taskSpec;
        public final TaskUser user;
        public final TaskRunMode runMode;
        public final TaskEvent event;
        public final String message;
        public final Timestamp timestamp;

        public TaskEventLogItem(TaskSpec taskSpec,
                                TaskUser user,
                                TaskRunMode runMode,
                                TaskEvent event,
                                String message,
                                Timestamp timestamp) {
            this.taskSpec = taskSpec;
            this.user = user;
            this.runMode = runMode;
            this.event = event;
            this.message = message != null ? message : "";
            this.timestamp = timestamp;
        }
    }

    private final static RowMapper LOG_ROWMAPPER = new RowMapper() {
        public Object mapRow(ResultSet rs, int i) throws SQLException {
            return new TaskEventLogItem(
                    new TaskSpec(rs.getString(1), decodeAccession(rs.getString(2))),
                    new TaskUser(rs.getString(3)),
                    rs.getString(4) != null ? TaskRunMode.valueOf(rs.getString(4)) : null,
                    TaskEvent.valueOf(rs.getString(5)),
                    rs.getString(6),
                    rs.getTimestamp(7)
            );
        }
    };

    @SuppressWarnings("unchecked")
    public List<TaskEventLogItem> getLastTaskEventLogItems(int number) {
        return (List<TaskEventLogItem>) jdbcTemplate.query("SELECT TYPE,ACCESSION,USERNAME,RUNMODE,EVENT,MESSAGE,TIME FROM (SELECT * FROM A2_TASKMAN_LOG ORDER BY TIME DESC) WHERE ROWNUM <= ? ORDER BY TIME ASC",
                new Object[] { number },
                LOG_ROWMAPPER);
    }

    @SuppressWarnings("unchecked")
    public List<TaskEventLogItem> getExperimentHistory(String accession) {
        String type = TaskTagType.EXPERIMENT.toString().toLowerCase();
        return (List<TaskEventLogItem>) jdbcTemplate.query("SELECT TYPE,ACCESSION,USERNAME,RUNMODE,EVENT,MESSAGE,TIME FROM A2_TASKMAN_LOG " +
                "WHERE TASKID IN (" +
                "  select taskcloudid from a2_taskman_tag where tagtype=? and tag=? " +
                "  union " +
                "  select taskid from a2_taskman_tagtasks tt join a2_taskman_tag t on t.taskcloudid=tt.taskcloudid and t.tagtype=? and t.tag=?" +
                ") ORDER BY TIME ASC",
                new Object[] { type, accession, type, accession },
                LOG_ROWMAPPER);
    }
}
