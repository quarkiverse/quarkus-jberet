package io.quarkiverse.jberet.components.deployment.jpa;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.jberet.jpa.repository.PropertiesConverter;
import org.jberet.jpa.repository.entity.JobExecutionJpa;
import org.jberet.jpa.repository.entity.JobExecutionJpa_;
import org.jberet.jpa.repository.entity.JobInstanceJpa;
import org.jberet.jpa.repository.entity.JobInstanceJpa_;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa;
import org.jberet.jpa.repository.entity.PartitionExecutionJpa_;
import org.jberet.jpa.repository.entity.StepExecutionJpa;
import org.jberet.jpa.repository.entity.StepExecutionJpa_;

import io.quarkiverse.jberet.components.runtime.repository.JobRepositoryConfig;
import io.quarkiverse.jberet.components.runtime.repository.JpaJobRepositorySupplier;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;

class JpaProcessor {

    private final static Collection<Class<?>> ENTITY_CLASSES = Arrays.asList(
            JobInstanceJpa.class,
            JobExecutionJpa.class,
            StepExecutionJpa.class,
            PartitionExecutionJpa.class,
            PropertiesConverter.class);

    private final static Collection<Class<?>> METADATA_CLASSES = Arrays.asList(
            JobInstanceJpa_.class,
            JobExecutionJpa_.class,
            StepExecutionJpa_.class,
            PartitionExecutionJpa_.class);

    @BuildStep(onlyIf = JpaJobRepositoryBooleanSupplier.class)
    void isHibernateAvailable(
            Capabilities capabilities,
            BuildProducer<FeatureBuildItem> feature) {
        if (capabilities.isMissing(Capability.HIBERNATE_ORM)) {
            throw new IllegalStateException("""
                    JBeret Components JPA Repository requires Quarkus Hibernate ORM Extension. Please add
                    `quarkus-hibernate-orm` to the project dependencies.""");
        }
    }

    @BuildStep(onlyIf = JpaJobRepositoryBooleanSupplier.class)
    void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JpaJobRepositorySupplier.class));
    }

    @BuildStep(onlyIf = JpaJobRepositoryBooleanSupplier.class)
    void additionalEntities(
            JobRepositoryConfig jobRepositoryConfig,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModelBuildItemsBuildProducer) {

        Set<String> persistenceUnits = Set.of(jobRepositoryConfig.repository().jpa().persistenceUnitName());
        ENTITY_CLASSES.forEach(entityClass -> additionalJpaModelBuildItemsBuildProducer
                .produce(new AdditionalJpaModelBuildItem(entityClass.getName(), persistenceUnits)));
    }

    @BuildStep(onlyIf = JpaJobRepositoryBooleanSupplier.class)
    void nativeImage(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        METADATA_CLASSES.forEach(metadataClass -> reflectiveClasses
                .produce(ReflectiveClassBuildItem.builder(metadataClass).constructors().methods().fields().build()));
    }
}
