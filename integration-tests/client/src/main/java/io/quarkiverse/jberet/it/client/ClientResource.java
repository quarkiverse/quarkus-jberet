package io.quarkiverse.jberet.it.client;

import java.util.Properties;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jberet.rest.client.BatchClient;
import org.jberet.rest.entity.JobExecutionEntity;

@Path("/client")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ClientResource {
    @Inject
    BatchClient batchClient;

    @POST
    @Path("/startJob/{jobXmlName}")
    public JobExecutionEntity startJob(@PathParam("jobXmlName") final String jobXmlName) throws Exception {
        return batchClient.startJob(jobXmlName, new Properties());
    }
}
