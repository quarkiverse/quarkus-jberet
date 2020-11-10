package io.quarkiverse.jberet.it.chunk;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.Optional;
import java.util.Properties;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.JobExecution;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.runtime.annotations.RegisterForReflection;

@Path("/auctions")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class AuctionResource {
    @Inject
    JobOperator jobOperator;
    @Inject
    AuctionDatabase database;

    @GET
    @Path("/job/execute/{fileName}")
    public Response executeJob(@PathParam("fileName") final String fileName) {
        Properties jobParameters = new Properties();
        // TODO - This is not working as @ConfigProperty
        jobParameters.setProperty("auctions.file", fileName);
        long executionId = jobOperator.start("auctions", jobParameters);
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);

        return Response.ok(new JobData(executionId, jobExecution.getBatchStatus().toString())).build();
    }

    @GET
    @Path("/job/execution/{executionId}")
    public Response getStatus(@PathParam("executionId") final Long executionId) {
        JobExecution jobExecution = jobOperator.getJobExecution(executionId);
        return Response.ok(new JobData(executionId, jobExecution.getBatchStatus().toString())).build();
    }

    @GET
    @Path("/{id}")
    public Response getAuction(@PathParam("id") final Long id) {
        return Optional.ofNullable(database.get(id)).map(Response::ok).orElse(Response.status(NOT_FOUND)).build();
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
