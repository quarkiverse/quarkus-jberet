package io.quarkiverse.jberet.it.jdbc;

import java.util.List;
import java.util.stream.Collectors;

import javax.batch.runtime.JobInstance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
                    final JobInstanceImpl newJobInstance = new JobInstanceImpl(null, null, jobInstance.getJobName());
                    newJobInstance.setId(jobInstance.getInstanceId());
                    return newJobInstance;
                }).collect(Collectors.toList());

        return Response.ok(jobInstances).build();
    }
}
