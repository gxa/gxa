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
import org.springframework.jdbc.core.ResultSetExtractor;
import uk.ac.ebi.gxa.dao.AtlasDAO;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
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


    public void updateTaskStage(TaskSpec task, TaskStage stage) {
        try {
            if(jdbcTemplate.update(
                    "MERGE INTO A2_TASKMAN_TASKSTAGE ts USING DUAL ON (ts.type = :1 and ts.accession = :2) " +
                            "WHEN MATCHED THEN UPDATE SET stage = :3 " +
                            "WHEN NOT MATCHED THEN INSERT (type,accession,stage) values (:4, :5, :6)",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            stage.toString(),
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            stage.toString()
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task stage " + task + " " + stage, e);
        }
    }

    public TaskStage getTaskStage(TaskSpec task) {
        try {
            return TaskStage.valueOf(jdbcTemplate.queryForObject(
                    "SELECT stage FROM A2_TASKMAN_TASKSTAGE ts WHERE ts.type = :1 AND ts.accession = :2",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession())
                    }, String.class).toString());
        } catch (EmptyResultDataAccessException e) {
            return TaskStage.NONE;
        } catch (DataAccessException e) {
            log.error("Can't retrieve task stage " + task, e);
            return TaskStage.NONE;
        }
    }

    public void logTaskStageEvent(TaskSpec task, TaskStage stage, TaskStageEvent event, String message) {
        try {
            if(jdbcTemplate.update(
                    "INSERT INTO A2_TASKMAN_TASKSTAGELOG (TYPE, ACCESSION, STAGE, EVENT, MESSAGE) VALUES (?,?,?,?,?)",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            stage.getStage(),
                            event.toString(),
                            message == null ? "" : message
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task stage log " + task + " " + stage + " " + event + " " + message, e);
        }
    }

    public void logTaskOperation(TaskSpec task, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message) {
        try {
            if(jdbcTemplate.update(
                    "INSERT INTO A2_TASKMAN_OPERATIONLOG (TYPE, ACCESSION, RUNMODE, USERNAME, OPERATION, MESSAGE) VALUES (?,?,?,?,?,?)",
                    new Object[] {
                            task.getType(),
                            encodeAccession(task.getAccession()),
                            runMode == null ? "" : runMode.toString(),
                            user.getUserName(),
                            operation.toString(),
                            message == null ? "" : message
                    }) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task operation log " + task + " " + runMode + " " + operation + " " + " " + user + " " + message, e);
        }
    }

    public static class OperationLogItem {
        public final TaskSpec taskSpec;
        public final TaskRunMode runMode;
        public final TaskUser user;
        public final TaskOperation operation;
        public final String message;
        public final Timestamp timestamp;

        public OperationLogItem(TaskSpec taskSpec, TaskRunMode runMode, TaskUser user, TaskOperation operation, String message, Timestamp timestamp) {
            this.taskSpec = taskSpec;
            this.runMode = runMode;
            this.user = user;
            this.operation = operation;
            this.message = message != null ? message : "";
            this.timestamp = timestamp;
        }
    }

    @SuppressWarnings("unchecked")
    public List<OperationLogItem> getLastOperationLogItems(int number) {
        return (List<OperationLogItem>) jdbcTemplate.query("SELECT TYPE,ACCESSION,RUNMODE,USERNAME,OPERATION,MESSAGE,TIME FROM (SELECT * FROM A2_TASKMAN_OPERATIONLOG ORDER BY TIME DESC) WHERE ROWNUM <= ? ORDER BY TIME ASC",
                new Object[] { number },
                new RowMapper() {
                    public Object mapRow(ResultSet rs, int i) throws SQLException {
                        return new OperationLogItem(
                                new TaskSpec(rs.getString(1), decodeAccession(rs.getString(2))),
                                rs.getString(3) != null ? TaskRunMode.valueOf(rs.getString(3)) : null,
                                new TaskUser(rs.getString(4)),
                                TaskOperation.valueOf(rs.getString(5)),
                                rs.getString(6),
                                rs.getTimestamp(7)
                        );
                    }
                });
    }

    public static class TaskEventLogItem {
        public final TaskSpec taskSpec;
        public final TaskStage stage;
        public final TaskStageEvent event;
        public final String message;
        public final Timestamp timestamp;

        public TaskEventLogItem(TaskSpec taskSpec, TaskStage stage, TaskStageEvent event, String message, Timestamp timestamp) {
            this.taskSpec = taskSpec;
            this.stage = stage;
            this.event = event;
            this.message = message != null ? message : "";
            this.timestamp = timestamp;
        }
    }

    @SuppressWarnings("unchecked")
    public List<TaskEventLogItem> getLastTaskEventLogItems(int number) {
        return (List<TaskEventLogItem>) jdbcTemplate.query("SELECT TYPE,ACCESSION,STAGE,EVENT,MESSAGE,TIME FROM (SELECT * FROM A2_TASKMAN_TASKSTAGELOG ORDER BY TIME DESC) WHERE ROWNUM <= ? ORDER BY TIME ASC",
                new Object[] { number },
                new RowMapper() {
                    public Object mapRow(ResultSet rs, int i) throws SQLException {
                        return new TaskEventLogItem(
                                new TaskSpec(rs.getString(1), decodeAccession(rs.getString(2))),
                                TaskStage.valueOf(rs.getString(3)),
                                TaskStageEvent.valueOf(rs.getString(4)),
                                rs.getString(5),
                                rs.getTimestamp(6)
                        );
                    }
                });
    }

    public Map<TaskSpec,TaskStage> getTaskStagesByType(final String type) {
        final Map<TaskSpec,TaskStage> result = new HashMap<TaskSpec, TaskStage>();
        jdbcTemplate.query("SELECT TYPE,ACCESSION,STAGE FROM A2_TASKMAN_TASKSTAGE WHERE TYPE = ?",
                new Object[] { type },
                new ResultSetExtractor() {
                    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                        while(rs.next()) {
                            result.put(
                                    new TaskSpec(rs.getString(1), decodeAccession(rs.getString(2))),
                                    TaskStage.valueOf(rs.getString(3))
                            );
                        }
                        return null;
                    }
                });
        return result;
    }
}
