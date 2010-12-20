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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
                    }, String.class));
        } catch (EmptyResultDataAccessException e) {
            return TaskStatus.DONE; // no status means it's complete from migration
        } catch (DataAccessException e) {
            log.error("Can't retrieve task stage " + task, e);
            return TaskStatus.NONE;
        }
    }

    public void logTaskEvent(Task task, TaskEvent event, String message, TaskUser user) {
        try {
            if (jdbcTemplate.update(
                    "INSERT INTO A2_TASKMAN_LOG (TASKID, TYPE, ACCESSION, RUNMODE, USERNAME, EVENT, MESSAGE) VALUES (?,?,?,?,?,?,?)",
                    task.getTaskId(),
                    task.getTaskSpec().getType(),
                    encodeAccession(task.getTaskSpec().getAccession()),
                    task.getRunMode() == null ? "" : task.getRunMode().toString(),
                    user != null ? user.getUserName() : task.getUser().getUserName(),
                    event.toString(),
                    message == null ? "" : message) != 1)
                throw new IncorrectResultSizeDataAccessException(1);
        } catch (DataAccessException e) {
            log.error("Can't store task stage log " + task + " " + event + " " + message, e);
        }
    }


    public void addTag(Task task, TaskTagType type, String tag) {
        try {
            if("".equals(tag))
                return;
            
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

    public static class TaskEventLogItemList extends ArrayList<TaskEventLogItem> {
        private int numTotal;
        private int start;
        private List<String> userNameFacet;
        private List<String> typeFacet;
        private List<TaskEvent> eventFacet;

        public int getNumTotal() {
            return numTotal;
        }

        public void setNumTotal(int numTotal) {
            this.numTotal = numTotal;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public List<String> getUserNameFacet() {
            return userNameFacet;
        }

        public void setUserNameFacet(List<String> userNameFacet) {
            this.userNameFacet = userNameFacet;
        }

        public List<String> getTypeFacet() {
            return typeFacet;
        }

        public void setTypeFacet(List<String> typeFacet) {
            this.typeFacet = typeFacet;
        }

        public List<TaskEvent> getEventFacet() {
            return eventFacet;
        }

        public void setEventFacet(List<TaskEvent> eventFacet) {
            this.eventFacet = eventFacet;
        }
    }

    @SuppressWarnings("unchecked")
    public TaskEventLogItemList findTaskLogItems(TaskEvent eventFilter, TaskUser userFilter,
                                                String typeFilter,
                                                String accessionFilter,
                                                int start, int number) {

        List<Object> parameters = new ArrayList<Object>();
        StringBuilder where = new StringBuilder("WHERE 1=1");

        if(eventFilter != null) {
            where.append(" AND event=?");
            parameters.add(eventFilter.toString());
        }

        if(userFilter != null) {
            where.append(" AND username=?");
            parameters.add(userFilter.getUserName());
        }

        if(typeFilter != null) {
            where.append(" AND type=?");
            parameters.add(typeFilter);
        }

        if(accessionFilter != null) {
            where.append(" AND LOWER(accession) LIKE ?");
            parameters.add(likeifyString(accessionFilter));
        }

        final String whereStr = where.toString();

        final int total = jdbcTemplate.queryForInt("SELECT COUNT(1) FROM A2_TASKMAN_LOG " + whereStr,
                parameters.toArray(new Object[parameters.size()]));

        if((start > total || start < 0) && total > 0) {
            int page = (total - 1) / number;
            start = page * number;
        } else if(total == 0) {
            start = 0;
        }

        parameters.add(start + number + 1);
        parameters.add(start + 1);

        final TaskEventLogItemList results = new TaskEventLogItemList();
        results.setNumTotal(total);
        results.setStart(start);
        jdbcTemplate.query("" +
                "SELECT TYPE,ACCESSION,USERNAME,RUNMODE,EVENT,MESSAGE,TIME FROM " +
                "(SELECT l.*, rownum rn FROM (SELECT * FROM A2_TASKMAN_LOG " + whereStr +  "ORDER BY TIME ASC) l WHERE ROWNUM < ?) " +
                "WHERE rn >= ?",
                parameters.toArray(new Object[parameters.size()]),
                new ResultSetExtractor() {
                    public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                        while(rs.next()) {
                            results.add((TaskEventLogItem)LOG_ROWMAPPER.mapRow(rs, 0));
                        }
                        return null;
                    }
                });

        results.setUserNameFacet(jdbcTemplate.queryForList("SELECT DISTINCT username from A2_TASKMAN_LOG ORDER BY username", null, String.class));
        results.setTypeFacet(jdbcTemplate.queryForList("SELECT DISTINCT type from A2_TASKMAN_LOG ORDER BY type", null, String.class));
        results.setEventFacet((List<TaskEvent>)jdbcTemplate.query("SELECT DISTINCT event from A2_TASKMAN_LOG ORDER BY event",
                new Object[0], new RowMapper() {
                    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return TaskEvent.valueOf(rs.getString(1));
                    }
                }));

        return results;
    }

    @SuppressWarnings("unchecked")
    public List<TaskEventLogItem> getTaggedHistory(TaskTagType tagtype, String tag) {
        String type = tagtype.toString().toLowerCase();
        return (List<TaskEventLogItem>) jdbcTemplate.query("SELECT TYPE,ACCESSION,USERNAME,RUNMODE,EVENT,MESSAGE,TIME FROM A2_TASKMAN_LOG " +
                "WHERE TASKID IN (" +
                "  select taskcloudid from a2_taskman_tag where tagtype=? and tag=? " +
                "  union " +
                "  select taskid from a2_taskman_tagtasks tt join a2_taskman_tag t on t.taskcloudid=tt.taskcloudid and t.tagtype=? and t.tag=?" +
                ") ORDER BY TIME ASC",
                new Object[] { type, tag, type, tag },
                LOG_ROWMAPPER);
    }

    public boolean isAnyIncomplete(String... taskType) {
        try {
            NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(jdbcTemplate);
            MapSqlParameterSource parameters = new MapSqlParameterSource();
            parameters.addValue("types", Arrays.<String>asList(taskType));
            return namedTemplate.queryForInt(
                    "SELECT count(1) FROM A2_TASKMAN_STATUS ts WHERE ts.type in (:types) AND ts.status in ('INCOMPLETE', 'NONE')",
                    parameters) > 0;
        } catch (DataAccessException e) {
            log.error("Can't retrieve task statuses", e);
            return false;
        }
    }


    public static class ExperimentWithStatus extends Experiment {
        private boolean netcdfComplete;
        private boolean analyticsComplete;
        private boolean indexComplete;

        public boolean isNetcdfComplete() {
            return netcdfComplete;
        }

        public void setNetcdfComplete(boolean netcdfComplete) {
            this.netcdfComplete = netcdfComplete;
        }

        public boolean isAnalyticsComplete() {
            return analyticsComplete;
        }

        public void setAnalyticsComplete(boolean analyticsComplete) {
            this.analyticsComplete = analyticsComplete;
        }

        public boolean isIndexComplete() {
            return indexComplete;
        }

        public void setIndexComplete(boolean indexComplete) {
            this.indexComplete = indexComplete;
        }
    }

    public static class ExperimentList extends ArrayList<ExperimentWithStatus> {
        private int numTotal;

        public int getNumTotal() {
            return numTotal;
        }

        private void setNumTotal(int numTotal) {
            this.numTotal = numTotal;
        }
    }

    public enum ExperimentIncompleteness {
        ALL,
        COMPLETE,
        INCOMPLETE,
        INCOMPLETE_ANALYTICS,
        INCOMPLETE_NETCDF,
        INCOMPLETE_INDEX
    }

    public ExperimentList findExperiments(String search,
                                          Date from, Date to,
                                          ExperimentIncompleteness incompleteness,
                                          int start, int number) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM (SELECT e.accession, e.description, e.performer, e.lab, e.experimentid, e.loaddate, " +
                        "COUNT(CASE s.type WHEN 'analytics' THEN s.status ELSE null END) as incanalytics, " +
                        "COUNT(CASE s.type WHEN 'updateexperiment' THEN s.status ELSE null END) as incnetcdf, " +
                        "COUNT(CASE s.type WHEN 'indexexperiment' THEN s.status ELSE null END) as incindex " +
                        "FROM a2_experiment e LEFT JOIN a2_taskman_status s " +
                        "ON e.accession=s.accession and s.type in ('analytics', 'updateexperiment', 'indexexperiment') AND s.status='INCOMPLETE'" +
                        "GROUP BY e.accession, e.description, e.performer, e.lab, e.experimentid, e.loaddate " +
                        "ORDER BY e.loaddate DESC NULLS LAST, e.accession) " +
                        "WHERE 1=1 ");

        List<Object> parameters = new ArrayList<Object>();

        if(to == null && from != null) {
            sql.append(" AND loaddate >= ?");
            parameters.add(from);
        } else if(from == null && to != null) {
            sql.append(" AND loaddate <= ?");
            parameters.add(to);
        } else if(from != null && to.after(from)) {
            sql.append(" AND loaddate BETWEEN ? AND ?");
            parameters.add(from);
            parameters.add(to);
        }

        String searchStr = StringUtils.trimToEmpty(search);
        if(searchStr.length() > 0) {
            sql.append(" AND (lower(accession) LIKE ? OR lower(description) LIKE ? OR lower(performer) LIKE ? OR lower(lab) LIKE ?)");
            searchStr = likeifyString(searchStr);
            parameters.add(searchStr);
            parameters.add(searchStr);
            parameters.add(searchStr);
            parameters.add(searchStr);
        }

        switch(incompleteness) {
            case COMPLETE:
                sql.append(" AND incanalytics = 0 AND incindex = 0 AND incnetcdf = 0");
                break;

            case INCOMPLETE:
                sql.append(" AND (incanalytics > 0 OR incindex > 0 OR incnetcdf > 0)");
                break;

            case INCOMPLETE_ANALYTICS:
                sql.append(" AND incanalytics > 0");
                break;


            case INCOMPLETE_NETCDF:
                sql.append(" AND incnetcdf > 0");
                break;


            case INCOMPLETE_INDEX:
                sql.append(" AND incindex > 0");
                break;
        }


        final int numTotal;

        if(number > 0 && start >= 0) {
            numTotal = jdbcTemplate.queryForInt("SELECT COUNT(1) FROM (" + sql.toString() + ")",
                    parameters.toArray(new Object[parameters.size()]));

            sql.insert(0, "SELECT * FROM (SELECT l.*, rownum rn FROM (");
            sql.append(") l WHERE ROWNUM < ?) WHERE rn >= ?");
            parameters.add(start + number + 1);
            parameters.add(start + 1);
        } else {
            numTotal = -1;
        }

        return (ExperimentList)jdbcTemplate.query(sql.toString(),
                parameters.toArray(new Object[parameters.size()]),
                new ResultSetExtractor() {
                    public Object extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                        ExperimentList results = new ExperimentList();
                        int total = 0;
                        while (resultSet.next()) {
                            ExperimentWithStatus experiment = new ExperimentWithStatus();

                            experiment.setAccession(resultSet.getString(1));
                            experiment.setDescription(resultSet.getString(2));
                            experiment.setPerformer(resultSet.getString(3));
                            experiment.setLab(resultSet.getString(4));
                            experiment.setExperimentID(resultSet.getLong(5));
                            experiment.setLoadDate(resultSet.getDate(6));

                            experiment.setAnalyticsComplete(resultSet.getInt(7) == 0);
                            experiment.setNetcdfComplete(resultSet.getInt(8) == 0);
                            experiment.setIndexComplete(resultSet.getInt(9) == 0);
                            results.add(experiment);
                            ++total;
                        }
                        results.setNumTotal(numTotal == -1 ? total : numTotal);
                        return results;
                    }
                });
    }

    private static String likeifyString(String searchStr) {
        return "%" + searchStr.replaceAll("[%_*\\[\\]]", "").toLowerCase().replaceAll("\\s+", "%") + "%";
    }
}
