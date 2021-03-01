package io.quarkiverse.jberet.it.programmatic;

import java.util.Properties;

import javax.batch.runtime.BatchRuntime;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;
import org.jberet.operations.AbstractJobOperator;
import org.jberet.operations.DelegatingJobOperator;

@Path("/batch")
@Produces(MediaType.TEXT_PLAIN)
public class ProgrammaticResource {
    @GET
    @Path("/job/execute/")
    public Response executeProgrammaticJob() {
        Job job = new JobBuilder("programmatic")
                .step(new StepBuilder("programmaticStep")
                        .batchlet("programmaticBatchlet")
                        .build())
                .build();

        AbstractJobOperator jobOperator = (AbstractJobOperator) ((DelegatingJobOperator) BatchRuntime.getJobOperator())
                .getDelegate();
        long executionId = jobOperator.start(job, new Properties());
        return Response.ok().entity(executionId).build();
    }
}
