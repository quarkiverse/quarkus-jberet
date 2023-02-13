package io.quarkiverse.jberet.jpa.job.repository.deployment;

import static io.quarkiverse.jberet.runtime.JBeretRepositoryTypeUtil.normalize;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
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

import io.quarkiverse.jberet.jpa.job.repository.JBeretJpaJobRepository;
import io.quarkiverse.jberet.jpa.job.repository.JBeretJpaJobRepositoryConfig;
import io.quarkiverse.jberet.runtime.JBeretConfig;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.runtime.configuration.ConfigurationException;

public class JBeretJpaJobRepositoryProcessor {

    public final static Collection<Class> ENTITY_CLASSES = Arrays.asList(
            JobInstanceJpa.class,
            JobExecutionJpa.class,
            StepExecutionJpa.class,
            PartitionExecutionJpa.class,
            PropertiesConverter.class);

    public final static Collection<Class> METADATA_CLASSES = Arrays.asList(
            JobInstanceJpa_.class,
            JobExecutionJpa_.class,
            StepExecutionJpa_.class,
            PartitionExecutionJpa_.class);

    public final static String ENTITY_PACKAGE = "org.jberet.jpa.repository.entity";

    @BuildStep
    public void registerExtension(BuildProducer<FeatureBuildItem> feature, BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem("jberet-jpa-job-repository"));
    }

    @BuildStep
    public void additionalBeans(
            JBeretConfig config,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (JBeretJpaJobRepository.TYPE.equals(normalize(config.repository().type()))) {
            additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(JBeretJpaJobRepository.class));
        }
    }

    @BuildStep
    public void additionalEntities(
            JBeretConfig config,
            JBeretJpaJobRepositoryConfig jpaJobRepositoryConfig,
            HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModelBuildItemsBuildProducer) {
        if (!JBeretJpaJobRepository.TYPE.equals(normalize(config.repository().type()))) {
            return;
        }

        String persistenceUnitName = jpaJobRepositoryConfig.repository().jpa().persistenceUnitName();

        if (!PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME.equals(persistenceUnitName)
                && !hibernateOrmConfig.persistenceUnits.containsKey(persistenceUnitName)) {
            throw new ConfigurationException("There is no persistence unit with name : " + persistenceUnitName);
        }

        HibernateOrmConfigPersistenceUnit persistenceUnit = Optional.ofNullable(
                hibernateOrmConfig.persistenceUnits.get(persistenceUnitName)).orElse(
                        hibernateOrmConfig.defaultPersistenceUnit);

        Set<String> packages = persistenceUnit.packages.orElse(new HashSet<>());
        packages.add(ENTITY_PACKAGE);
        persistenceUnit.packages = Optional.of(packages);

        ENTITY_CLASSES.forEach(
                entityClass -> additionalJpaModelBuildItemsBuildProducer.produce(
                        new AdditionalJpaModelBuildItem(
                                entityClass.getName())));
    }

    @BuildStep
    public void nativeImage(BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        METADATA_CLASSES.forEach(
                metadataClass -> reflectiveClasses.produce(
                        new ReflectiveClassBuildItem(
                                true,
                                true,
                                true,
                                metadataClass)));
    }

}
