package io.quarkiverse.jberet.it.programmatic;

import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
