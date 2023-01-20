package io.quarkiverse.jberet.it.chunk;

import java.util.Properties;

import jakarta.batch.operations.JobOperator;
import jakarta.batch.runtime.BatchRuntime;
import jakarta.batch.runtime.JobExecution;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * To verify that BatchRuntime.getJobOperator() returns the right operator in native mode.
 */
@Path("/batch")
@Produces(MediaType.APPLICATION_JSON)
public class BatchResource {
    @GET
    @Path("/job/execute/{auctionsFile}")
    public Response executeJob(@PathParam("auctionsFile") final String auctionsFile) {
        JobOperator jobOperator = BatchRuntime.getJobOperator();
        Properties jobParameters = new Properties();
        jobParameters.put("auctions.file", auctionsFile);
        long executionId = jobOperator.start("auctions", jobParameters);
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
