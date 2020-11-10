package io.quarkiverse.jberet.it.jdbc;

import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Path("/batch")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class BatchResource {
    @Inject
    JobOperator jobOperator;

    @GET
    @Path("/job/execute")
    public Response executeJob() {
        Properties jobParameters = new Properties();
        long executionId = jobOperator.start("jdbc", jobParameters);
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        return Response.ok(new JobData(executionId, jobExecution.getBatchStatus().toString())).build();
    }

    @RegisterForReflection
    public static class JobData {
        private Long executionId;
        private String status;

        public JobData() {
        }

        public JobData(final Long executionId, final String status) {
            this.executionId = executionId;
            this.status = status;
        }

        public Long getExecutionId() {
            return executionId;
        }

        public void setExecutionId(final Long executionId) {
            this.executionId = executionId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(final String status) {
            this.status = status;
        }
    }
}
