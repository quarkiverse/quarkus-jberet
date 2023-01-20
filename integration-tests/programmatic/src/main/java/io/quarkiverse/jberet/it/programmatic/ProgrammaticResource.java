package io.quarkiverse.jberet.it.programmatic;

import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jberet.job.model.Job;
import org.jberet.job.model.JobBuilder;
import org.jberet.job.model.StepBuilder;

import io.quarkiverse.jberet.runtime.QuarkusJobOperator;

@Path("/batch")
@Produces(MediaType.TEXT_PLAIN)
public class ProgrammaticResource {
    @Inject
    QuarkusJobOperator quarkusJobOperator;

    @GET
    @Path("/job/execute/")
    public Response executeProgrammaticJob() {
        Job job = new JobBuilder("programmatic")
                .step(new StepBuilder("programmaticStep")
                        .batchlet("programmaticBatchlet")
                        .build())
                .build();

        long executionId = quarkusJobOperator.start(job, new Properties());
        return Response.ok().entity(executionId).build();
    }
}
