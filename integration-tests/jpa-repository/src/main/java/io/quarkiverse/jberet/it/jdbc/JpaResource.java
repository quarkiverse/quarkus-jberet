package io.quarkiverse.jberet.it.jdbc;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jberet.jpa.repository.entity.JobInstanceJpa;
import org.jberet.jpa.repository.entity.JobInstanceJpa_;
import org.jberet.runtime.JobInstanceImpl;

import io.quarkiverse.jberet.jpa.job.repository.JBeretJpaJobRepositoryConfig;
import io.quarkus.hibernate.orm.PersistenceUnit;

@Path("/jpa")
@Produces(MediaType.APPLICATION_JSON)
public class JpaResource {

    @Inject
    protected JBeretJpaJobRepositoryConfig config;

    @Any
    @Inject
    protected Instance<EntityManager> entityManager;

    @GET
    @Path("/instances/{name}")
    public Response instances(@PathParam("name") final String name) throws Exception {
        EntityManager em = entityManager.select(
                new PersistenceUnit.PersistenceUnitLiteral(
                        config.repository().jpa().persistenceUnitName()))
                .get();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<JobInstanceJpa> cq = cb.createQuery(JobInstanceJpa.class);

        Root<JobInstanceJpa> jobInstance = cq.from(JobInstanceJpa.class);
        Predicate jobNamePredicate = cb.equal(jobInstance.get(JobInstanceJpa_.jobName), name);
        cq.where(jobNamePredicate);
        cq.orderBy(cb.desc(jobInstance.get(JobInstanceJpa_.id)));

        TypedQuery<JobInstanceJpa> query = em.createQuery(cq);

        List<JobInstanceImpl> toList = query.getResultList().stream().map(
                result -> {
                    JobInstanceImpl jobInstanceImpl = new JobInstanceImpl(null, null, result.getJobName());
                    jobInstanceImpl.setId(result.getId());
                    return jobInstanceImpl;
                }).collect(Collectors.toList());

        return Response.ok(toList).build();
    }

}
