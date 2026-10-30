package io.quarkiverse.jberet.it.mongo.repository;

import java.util.List;

import jakarta.batch.runtime.JobInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jberet.repository.JobRepository;
import org.jberet.runtime.JobInstanceImpl;

@Path("/repository")
@Produces(MediaType.APPLICATION_JSON)
public class RepositoryResource {
    @Inject
    JobRepository jobRepository;

    @GET
    @Path("/instances/{name}")
    public Response instances(@PathParam("name") final String name) {
        final List<JobInstance> jobInstances = jobRepository.getJobInstances(name).stream().map(
                jobInstance -> {
                    final JobInstance newJobInstance = new JobInstanceImpl(null, null, jobInstance.getJobName());
                    ((JobInstanceImpl) newJobInstance).setId(jobInstance.getInstanceId());
                    return newJobInstance;
                }).toList();

        return Response.ok(jobInstances).build();
    }

    @DELETE
    @Path("/jobs/{jobId}")
    public Response removeJob(@PathParam("jobId") final String jobId) {
        jobRepository.removeJob(jobId);
        return Response.noContent().build();
    }
}
