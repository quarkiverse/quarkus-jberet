package io.quarkiverse.jberet.it.client;

import java.util.Properties;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
