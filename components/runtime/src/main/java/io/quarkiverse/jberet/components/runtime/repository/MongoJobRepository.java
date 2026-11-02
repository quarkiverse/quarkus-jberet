package io.quarkiverse.jberet.components.runtime.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.descending;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.JobExecution;
import jakarta.batch.runtime.JobInstance;
import jakarta.batch.runtime.Metric;
import jakarta.batch.runtime.StepExecution;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.jberet.job.model.Job;
import org.jberet.repository.ApplicationAndJobName;
import org.jberet.repository.JobExecutionSelector;
import org.jberet.repository.JobRepository;
import org.jberet.runtime.AbstractStepExecution;
import org.jberet.runtime.JobExecutionImpl;
import org.jberet.runtime.JobInstanceImpl;
import org.jberet.runtime.PartitionExecutionImpl;
import org.jberet.runtime.StepExecutionImpl;
import org.jberet.util.BatchUtil;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.UpdateResult;

/**
 * A {@link JobRepository} that stores the batch metadata in MongoDB, using the Quarkus MongoDB client.
 * <p>
 * Collection and field names are the same as the ones used by the JBeret <code>MongoRepository</code>, which cannot be
 * used in Quarkus because it requires the legacy <code>mongo-java-driver</code>.
 * <p>
 * Running executions must keep their identity, because the batch runtime and the job operator coordinate through the
 * same instance, so instances and executions are cached with soft references, like all the JBeret repositories.
 */
public class MongoJobRepository implements JobRepository {
    static final String SEQ = "seq";
    static final String ID = "_id";

    static final String JOB_INSTANCE = "JOB_INSTANCE";
    static final String JOB_EXECUTION = "JOB_EXECUTION";
    static final String STEP_EXECUTION = "STEP_EXECUTION";
    static final String PARTITION_EXECUTION = "PARTITION_EXECUTION";

    static final String JOBINSTANCEID = "JOBINSTANCEID";
    static final String JOBEXECUTIONID = "JOBEXECUTIONID";
    static final String STEPEXECUTIONID = "STEPEXECUTIONID";
    static final String PARTITIONEXECUTIONID = "PARTITIONEXECUTIONID";

    static final String JOBNAME = "JOBNAME";
    static final String APPLICATIONNAME = "APPLICATIONNAME";
    static final String STEPNAME = "STEPNAME";
    static final String CREATETIME = "CREATETIME";
    static final String STARTTIME = "STARTTIME";
    static final String ENDTIME = "ENDTIME";
    static final String LASTUPDATEDTIME = "LASTUPDATEDTIME";
    static final String BATCHSTATUS = "BATCHSTATUS";
    static final String EXITSTATUS = "EXITSTATUS";
    static final String JOBPARAMETERS = "JOBPARAMETERS";
    static final String RESTARTPOSITION = "RESTARTPOSITION";
    static final String EXECUTIONEXCEPTION = "EXECUTIONEXCEPTION";
    static final String PERSISTENTUSERDATA = "PERSISTENTUSERDATA";
    static final String READERCHECKPOINTINFO = "READERCHECKPOINTINFO";
    static final String WRITERCHECKPOINTINFO = "WRITERCHECKPOINTINFO";
    static final String READCOUNT = "READCOUNT";
    static final String WRITECOUNT = "WRITECOUNT";
    static final String COMMITCOUNT = "COMMITCOUNT";
    static final String ROLLBACKCOUNT = "ROLLBACKCOUNT";
    static final String READSKIPCOUNT = "READSKIPCOUNT";
    static final String PROCESSSKIPCOUNT = "PROCESSSKIPCOUNT";
    static final String FILTERCOUNT = "FILTERCOUNT";
    static final String WRITESKIPCOUNT = "WRITESKIPCOUNT";

    private static final int EXECUTION_EXCEPTION_LENGTH_LIMIT = 2048;

    private final ConcurrentMap<ApplicationAndJobName, SoftReference<Job>> jobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SoftReference<JobInstanceImpl>> jobInstances = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SoftReference<JobExecutionImpl>> jobExecutions = new ConcurrentHashMap<>();

    private final MongoCollection<Document> sequences;
    private final MongoCollection<Document> instances;
    private final MongoCollection<Document> executions;
    private final MongoCollection<Document> steps;
    private final MongoCollection<Document> partitions;

    public MongoJobRepository(final MongoClient mongoClient, final String database) {
        MongoDatabase db = mongoClient.getDatabase(database);
        this.sequences = db.getCollection(SEQ);
        this.instances = db.getCollection(JOB_INSTANCE);
        this.executions = db.getCollection(JOB_EXECUTION);
        this.steps = db.getCollection(STEP_EXECUTION);
        this.partitions = db.getCollection(PARTITION_EXECUTION);

        instances.createIndex(ascending(JOBINSTANCEID));
        instances.createIndex(ascending(JOBNAME));
        executions.createIndex(ascending(JOBEXECUTIONID));
        executions.createIndex(ascending(JOBINSTANCEID));
        steps.createIndex(ascending(STEPEXECUTIONID));
        steps.createIndex(ascending(JOBEXECUTIONID));
        partitions.createIndex(ascending(STEPEXECUTIONID, PARTITIONEXECUTIONID));
    }

    @Override
    public void addJob(final ApplicationAndJobName applicationAndJobName, final Job job) {
        jobs.values().removeIf(reference -> reference.get() == null);
        jobs.put(applicationAndJobName, new SoftReference<>(job));
    }

    @Override
    public void removeJob(final String jobId) {
        jobs.keySet().removeIf(key -> key.jobName.equals(jobId));
        jobInstances.values().removeIf(reference -> {
            JobInstanceImpl jobInstance = reference.get();
            return jobInstance != null && jobInstance.getJobName().equals(jobId);
        });
        jobExecutions.values().removeIf(reference -> {
            JobExecutionImpl jobExecution = reference.get();
            return jobExecution != null && jobExecution.getJobName().equals(jobId);
        });
    }

    @Override
    public Job getJob(final ApplicationAndJobName applicationAndJobName) {
        return cached(jobs, applicationAndJobName);
    }

    @Override
    public Set<String> getJobNames() {
        Set<String> jobNames = new HashSet<>();
        for (ApplicationAndJobName key : jobs.keySet()) {
            jobNames.add(key.jobName);
        }
        return jobNames;
    }

    @Override
    public boolean jobExists(final String jobName) {
        return jobs.keySet().stream().anyMatch(key -> key.jobName.equals(jobName));
    }

    @Override
    public JobInstanceImpl createJobInstance(final Job job, final String applicationName, final ClassLoader classLoader) {
        jobInstances.values().removeIf(reference -> reference.get() == null);

        JobInstanceImpl jobInstance = new JobInstanceImpl(job, applicationName, job.getId());
        long id = nextId(JOBINSTANCEID);
        jobInstance.setId(id);
        instances.insertOne(new Document(JOBINSTANCEID, id)
                .append(JOBNAME, jobInstance.getJobName())
                .append(APPLICATIONNAME, jobInstance.getApplicationName()));
        jobInstances.put(id, new SoftReference<>(jobInstance));
        return jobInstance;
    }

    @Override
    public void removeJobInstance(final long jobInstanceId) {
        jobInstances.remove(jobInstanceId);
    }

    @Override
    public JobInstanceImpl getJobInstance(final long jobInstanceId) {
        JobInstanceImpl jobInstance = cached(jobInstances, jobInstanceId);
        if (jobInstance != null) {
            return jobInstance;
        }
        Document document = instances.find(eq(JOBINSTANCEID, jobInstanceId)).first();
        return document != null ? toJobInstance(document) : null;
    }

    @Override
    public List<JobInstance> getJobInstances(final String jobName) {
        boolean selectAll = jobName == null || jobName.equals("*");
        FindIterable<Document> documents = selectAll ? instances.find() : instances.find(eq(JOBNAME, jobName));

        List<JobInstance> result = new ArrayList<>();
        for (Document document : documents.sort(descending(JOBINSTANCEID))) {
            result.add(toJobInstance(document));
        }
        return result;
    }

    @Override
    public int getJobInstanceCount(final String jobName) {
        return (int) instances.countDocuments(eq(JOBNAME, jobName));
    }

    @Override
    public JobExecutionImpl createJobExecution(final JobInstanceImpl jobInstance, final Properties jobParameters) {
        jobExecutions.values().removeIf(reference -> reference.get() == null);

        JobExecutionImpl jobExecution = new JobExecutionImpl(jobInstance, jobParameters);
        long id = nextId(JOBEXECUTIONID);
        jobExecution.setId(id);
        executions.insertOne(new Document(JOBEXECUTIONID, id)
                .append(JOBINSTANCEID, jobInstance.getInstanceId())
                .append(CREATETIME, jobExecution.getCreateTime())
                .append(BATCHSTATUS, jobExecution.getBatchStatus().name())
                .append(JOBPARAMETERS, BatchUtil.propertiesToString(jobExecution.getJobParameters())));
        jobExecutions.put(id, new SoftReference<>(jobExecution));
        jobInstance.addJobExecution(jobExecution);
        return jobExecution;
    }

    @Override
    public JobExecutionImpl getJobExecution(final long jobExecutionId) {
        JobExecutionImpl jobExecution = cached(jobExecutions, jobExecutionId);
        if (jobExecution != null) {
            return jobExecution;
        }
        Document document = executions.find(eq(JOBEXECUTIONID, jobExecutionId)).first();
        return document != null ? toJobExecution(document) : null;
    }

    @Override
    public List<JobExecution> getJobExecutions(final JobInstance jobInstance) {
        FindIterable<Document> documents = jobInstance == null ? executions.find()
                : executions.find(eq(JOBINSTANCEID, jobInstance.getInstanceId()));

        List<JobExecution> result = new ArrayList<>();
        for (Document document : documents.sort(ascending(JOBEXECUTIONID))) {
            result.add(toJobExecution(document));
        }
        return result;
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName) {
        return getJobExecutionsByJob(jobName, null);
    }

    @Override
    public List<Long> getJobExecutionsByJob(final String jobName, final Integer limit) {
        FindIterable<Document> documents = executions.find(in(JOBINSTANCEID, jobInstanceIds(jobName)))
                .projection(include(JOBEXECUTIONID))
                .sort(descending(JOBEXECUTIONID));
        if (limit != null) {
            documents = documents.limit(limit);
        }

        List<Long> result = new ArrayList<>();
        for (Document document : documents) {
            result.add(document.getLong(JOBEXECUTIONID));
        }
        return result;
    }

    @Override
    public List<Long> getRunningExecutions(final String jobName) {
        List<Long> result = new ArrayList<>();
        Bson running = and(
                in(JOBINSTANCEID, jobInstanceIds(jobName)),
                in(BATCHSTATUS, BatchStatus.STARTING.name(), BatchStatus.STARTED.name()));
        for (Document document : executions.find(running).projection(include(JOBEXECUTIONID))) {
            result.add(document.getLong(JOBEXECUTIONID));
        }
        return result;
    }

    @Override
    public void updateJobExecution(final JobExecutionImpl jobExecution, final boolean fullUpdate,
            final boolean saveJobParameters) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());

        Document update = new Document(LASTUPDATEDTIME, jobExecution.getLastUpdatedTime())
                .append(STARTTIME, jobExecution.getStartTime())
                .append(BATCHSTATUS, jobExecution.getBatchStatus().name());
        if (fullUpdate) {
            update.append(ENDTIME, jobExecution.getEndTime())
                    .append(EXITSTATUS, jobExecution.getExitStatus())
                    .append(RESTARTPOSITION, jobExecution.combineRestartPositionAndUser());
            if (saveJobParameters) {
                update.append(JOBPARAMETERS, BatchUtil.propertiesToString(jobExecution.getJobParameters()));
            }
        }
        executions.updateOne(eq(JOBEXECUTIONID, jobExecution.getExecutionId()), new Document("$set", update));
    }

    @Override
    public void stopJobExecution(final JobExecutionImpl jobExecution) {
        jobExecution.setLastUpdatedTime(System.currentTimeMillis());

        long jobExecutionId = jobExecution.getExecutionId();
        Bson started = eq(BATCHSTATUS, BatchStatus.STARTED.name());
        Bson stopping = set(BATCHSTATUS, BatchStatus.STOPPING.name());

        executions.updateOne(and(eq(JOBEXECUTIONID, jobExecutionId), started), stopping);
        steps.updateMany(and(eq(JOBEXECUTIONID, jobExecutionId), started), stopping);

        List<Long> stepExecutionIds = new ArrayList<>();
        for (Document document : steps.find(eq(JOBEXECUTIONID, jobExecutionId)).projection(include(STEPEXECUTIONID))) {
            stepExecutionIds.add(document.getLong(STEPEXECUTIONID));
        }
        partitions.updateMany(and(in(STEPEXECUTIONID, stepExecutionIds), started), stopping);
    }

    @Override
    public void removeJobExecutions(final JobExecutionSelector jobExecutionSelector) {
        Set<Long> jobExecutionIds = jobExecutions.keySet();
        jobExecutions.values().removeIf(reference -> {
            JobExecutionImpl jobExecution = reference.get();
            return jobExecution != null
                    && (jobExecutionSelector == null || jobExecutionSelector.select(jobExecution, jobExecutionIds));
        });
    }

    @Override
    public List<StepExecution> getStepExecutions(final long jobExecutionId, final ClassLoader classLoader) {
        JobExecutionImpl jobExecution = cached(jobExecutions, jobExecutionId);
        if (jobExecution != null && !jobExecution.getStepExecutions().isEmpty()) {
            return jobExecution.getStepExecutions();
        }

        List<StepExecution> result = new ArrayList<>();
        for (Document document : steps.find(eq(JOBEXECUTIONID, jobExecutionId)).sort(ascending(STEPEXECUTIONID))) {
            result.add(toStepExecution(document));
        }
        return result;
    }

    @Override
    public StepExecutionImpl createStepExecution(final String stepName) {
        return new StepExecutionImpl(stepName);
    }

    @Override
    public void addStepExecution(final JobExecutionImpl jobExecution, final StepExecutionImpl stepExecution) {
        jobExecution.addStepExecution(stepExecution);

        long id = nextId(STEPEXECUTIONID);
        stepExecution.setId(id);
        steps.insertOne(new Document(STEPEXECUTIONID, id)
                .append(JOBEXECUTIONID, jobExecution.getExecutionId())
                .append(STEPNAME, stepExecution.getStepName())
                .append(STARTTIME, stepExecution.getStartTime())
                .append(BATCHSTATUS, stepExecution.getBatchStatus().name()));
    }

    @Override
    public void updateStepExecution(final StepExecution stepExecution) {
        updateStepExecution((StepExecutionImpl) stepExecution, eq(STEPEXECUTIONID, stepExecution.getStepExecutionId()));
    }

    @Override
    public StepExecutionImpl findOriginalStepExecutionForRestart(final String stepName,
            final JobExecutionImpl jobExecutionToRestart, final ClassLoader classLoader) {
        for (StepExecution stepExecution : jobExecutionToRestart.getStepExecutions()) {
            if (stepName.equals(stepExecution.getStepName())) {
                return (StepExecutionImpl) stepExecution;
            }
        }

        Document document = steps.find(and(
                in(JOBEXECUTIONID, jobExecutionIds(jobExecutionToRestart.getJobInstance().getInstanceId())),
                eq(STEPNAME, stepName)))
                .sort(descending(STEPEXECUTIONID))
                .first();
        return document != null ? toStepExecution(document) : null;
    }

    @Override
    public int countStepStartTimes(final String stepName, final long jobInstanceId) {
        return (int) steps.countDocuments(and(in(JOBEXECUTIONID, jobExecutionIds(jobInstanceId)), eq(STEPNAME, stepName)));
    }

    @Override
    public void addPartitionExecution(final StepExecutionImpl enclosingStepExecution,
            final PartitionExecutionImpl partitionExecution) {
        enclosingStepExecution.getPartitionExecutions().add(partitionExecution);

        partitions.insertOne(new Document(PARTITIONEXECUTIONID, partitionExecution.getPartitionId())
                .append(STEPEXECUTIONID, partitionExecution.getStepExecutionId())
                .append(BATCHSTATUS, partitionExecution.getBatchStatus().name()));
    }

    @Override
    public List<PartitionExecutionImpl> getPartitionExecutions(final long stepExecutionId,
            final StepExecutionImpl stepExecution, final boolean notCompletedOnly, final ClassLoader classLoader) {
        List<PartitionExecutionImpl> result = new ArrayList<>();
        if (stepExecution != null && !stepExecution.getPartitionExecutions().isEmpty()) {
            for (PartitionExecutionImpl partitionExecution : stepExecution.getPartitionExecutions()) {
                if (!notCompletedOnly || partitionExecution.getBatchStatus() != BatchStatus.COMPLETED) {
                    result.add(partitionExecution);
                }
            }
            return result;
        }

        for (Document document : partitions.find(eq(STEPEXECUTIONID, stepExecutionId)).sort(ascending(PARTITIONEXECUTIONID))) {
            BatchStatus batchStatus = BatchStatus.valueOf(document.getString(BATCHSTATUS));
            if (!notCompletedOnly || batchStatus != BatchStatus.COMPLETED) {
                result.add(new PartitionExecutionImpl(
                        document.getInteger(PARTITIONEXECUTIONID),
                        document.getLong(STEPEXECUTIONID),
                        stepExecution != null ? stepExecution.getStepName() : null,
                        batchStatus,
                        document.getString(EXITSTATUS),
                        bytes(document, PERSISTENTUSERDATA),
                        bytes(document, READERCHECKPOINTINFO),
                        bytes(document, WRITERCHECKPOINTINFO)));
            }
        }
        return result;
    }

    @Override
    public void savePersistentData(final JobExecution jobExecution, final AbstractStepExecution stepOrPartitionExecution) {
        if (stepOrPartitionExecution instanceof StepExecutionImpl stepExecution) {
            updateStepExecution(stepExecution, eq(STEPEXECUTIONID, stepExecution.getStepExecutionId()));
        } else {
            updatePartitionExecution((PartitionExecutionImpl) stepOrPartitionExecution,
                    partitionFilter(stepOrPartitionExecution));
        }
    }

    @Override
    public int savePersistentDataIfNotStopping(final JobExecution jobExecution,
            final AbstractStepExecution stepOrPartitionExecution) {
        Bson notStopping = ne(BATCHSTATUS, BatchStatus.STOPPING.name());
        UpdateResult result;
        if (stepOrPartitionExecution instanceof StepExecutionImpl stepExecution) {
            result = updateStepExecution(stepExecution,
                    and(eq(STEPEXECUTIONID, stepExecution.getStepExecutionId()), notStopping));
        } else {
            result = updatePartitionExecution((PartitionExecutionImpl) stepOrPartitionExecution,
                    and(partitionFilter(stepOrPartitionExecution), notStopping));
        }
        return (int) result.getMatchedCount();
    }

    private UpdateResult updateStepExecution(final StepExecutionImpl stepExecution, final Bson filter) {
        Document update = new Document(ENDTIME, stepExecution.getEndTime())
                .append(BATCHSTATUS, stepExecution.getBatchStatus().name())
                .append(EXITSTATUS, stepExecution.getExitStatus())
                .append(EXECUTIONEXCEPTION, formatException(stepExecution.getException()))
                .append(PERSISTENTUSERDATA, stepExecution.getPersistentUserDataSerialized())
                .append(READCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.READ_COUNT))
                .append(WRITECOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.WRITE_COUNT))
                .append(COMMITCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.COMMIT_COUNT))
                .append(ROLLBACKCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.ROLLBACK_COUNT))
                .append(READSKIPCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.READ_SKIP_COUNT))
                .append(PROCESSSKIPCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.PROCESS_SKIP_COUNT))
                .append(FILTERCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.FILTER_COUNT))
                .append(WRITESKIPCOUNT, stepExecution.getStepMetrics().get(Metric.MetricType.WRITE_SKIP_COUNT))
                .append(READERCHECKPOINTINFO, stepExecution.getReaderCheckpointInfoSerialized())
                .append(WRITERCHECKPOINTINFO, stepExecution.getWriterCheckpointInfoSerialized());
        return steps.updateOne(filter, new Document("$set", update));
    }

    private UpdateResult updatePartitionExecution(final PartitionExecutionImpl partitionExecution, final Bson filter) {
        Document update = new Document(BATCHSTATUS, partitionExecution.getBatchStatus().name())
                .append(EXITSTATUS, partitionExecution.getExitStatus())
                .append(EXECUTIONEXCEPTION, formatException(partitionExecution.getException()))
                .append(PERSISTENTUSERDATA, partitionExecution.getPersistentUserDataSerialized())
                .append(READERCHECKPOINTINFO, partitionExecution.getReaderCheckpointInfoSerialized())
                .append(WRITERCHECKPOINTINFO, partitionExecution.getWriterCheckpointInfoSerialized());
        return partitions.updateOne(filter, new Document("$set", update));
    }

    private static Bson partitionFilter(final AbstractStepExecution partitionExecution) {
        return and(
                eq(STEPEXECUTIONID, partitionExecution.getStepExecutionId()),
                eq(PARTITIONEXECUTIONID, ((PartitionExecutionImpl) partitionExecution).getPartitionId()));
    }

    private List<Long> jobInstanceIds(final String jobName) {
        List<Long> ids = new ArrayList<>();
        for (Document document : instances.find(eq(JOBNAME, jobName)).projection(include(JOBINSTANCEID))) {
            ids.add(document.getLong(JOBINSTANCEID));
        }
        return ids;
    }

    private List<Long> jobExecutionIds(final long jobInstanceId) {
        List<Long> ids = new ArrayList<>();
        for (Document document : executions.find(eq(JOBINSTANCEID, jobInstanceId)).projection(include(JOBEXECUTIONID))) {
            ids.add(document.getLong(JOBEXECUTIONID));
        }
        return ids;
    }

    private long nextId(final String sequence) {
        // The upsert creates the sequence on first use, and $inc is atomic, so multiple nodes never get the same id
        Document document = sequences.findOneAndUpdate(eq(ID, sequence), inc(SEQ, 1L),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return document.getLong(SEQ);
    }

    private JobInstanceImpl toJobInstance(final Document document) {
        long id = document.getLong(JOBINSTANCEID);
        JobInstanceImpl jobInstance = cached(jobInstances, id);
        if (jobInstance == null) {
            String applicationName = document.getString(APPLICATIONNAME);
            String jobName = document.getString(JOBNAME);
            jobInstance = new JobInstanceImpl(getJob(new ApplicationAndJobName(applicationName, jobName)), applicationName,
                    jobName);
            jobInstance.setId(id);
            jobInstances.put(id, new SoftReference<>(jobInstance));
        }
        return jobInstance;
    }

    private JobExecutionImpl toJobExecution(final Document document) {
        long id = document.getLong(JOBEXECUTIONID);
        JobExecutionImpl jobExecution = cached(jobExecutions, id);
        if (jobExecution == null) {
            jobExecution = new JobExecutionImpl(
                    getJobInstance(document.getLong(JOBINSTANCEID)),
                    id,
                    BatchUtil.stringToProperties(document.getString(JOBPARAMETERS)),
                    document.get(CREATETIME, Date.class),
                    document.get(STARTTIME, Date.class),
                    document.get(ENDTIME, Date.class),
                    document.get(LASTUPDATEDTIME, Date.class),
                    document.getString(BATCHSTATUS),
                    document.getString(EXITSTATUS),
                    document.getString(RESTARTPOSITION));
            jobExecutions.put(id, new SoftReference<>(jobExecution));
        }
        return jobExecution;
    }

    private static StepExecutionImpl toStepExecution(final Document document) {
        return new StepExecutionImpl(
                document.getLong(STEPEXECUTIONID),
                document.getString(STEPNAME),
                document.get(STARTTIME, Date.class),
                document.get(ENDTIME, Date.class),
                document.getString(BATCHSTATUS),
                document.getString(EXITSTATUS),
                bytes(document, PERSISTENTUSERDATA),
                count(document, READCOUNT),
                count(document, WRITECOUNT),
                count(document, COMMITCOUNT),
                count(document, ROLLBACKCOUNT),
                count(document, READSKIPCOUNT),
                count(document, PROCESSSKIPCOUNT),
                count(document, FILTERCOUNT),
                count(document, WRITESKIPCOUNT),
                bytes(document, READERCHECKPOINTINFO),
                bytes(document, WRITERCHECKPOINTINFO));
    }

    private static <K, V> V cached(final Map<K, SoftReference<V>> cache, final K key) {
        SoftReference<V> reference = cache.get(key);
        return reference != null ? reference.get() : null;
    }

    private static byte[] bytes(final Document document, final String field) {
        Binary binary = document.get(field, Binary.class);
        return binary != null ? binary.getData() : null;
    }

    private static long count(final Document document, final String field) {
        Number count = document.get(field, Number.class);
        return count != null ? count.longValue() : 0;
    }

    private static String formatException(final Exception exception) {
        if (exception == null) {
            return null;
        }
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        return stackTrace.getBuffer().length() > EXECUTION_EXCEPTION_LENGTH_LIMIT
                ? stackTrace.getBuffer().substring(0, EXECUTION_EXCEPTION_LENGTH_LIMIT)
                : stackTrace.toString();
    }
}
