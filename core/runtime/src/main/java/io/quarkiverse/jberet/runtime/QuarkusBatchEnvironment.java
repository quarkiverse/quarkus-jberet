package io.quarkiverse.jberet.runtime;

import java.util.Map;
import java.util.Properties;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.transaction.TransactionManager;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jberet.creation.AbstractArtifactFactory;
import org.jberet.repository.JobRepository;
import org.jberet.spi.ArtifactFactory;
import org.jberet.spi.BatchEnvironment;
import org.jberet.spi.JobExecutor;
import org.jberet.spi.JobTask;
import org.jberet.spi.JobXmlResolver;

import io.quarkiverse.jberet.runtime.JBeretDataHolder.JBeretData;
import io.quarkus.arc.Arc;

class QuarkusBatchEnvironment implements BatchEnvironment {
    private final ArtifactFactory artifactFactory;
    private final JobExecutor jobExecutor;
    private final JobRepository jobRepository;
    private final TransactionManager transactionManager;

    private static final Properties PROPS = new Properties();

    public QuarkusBatchEnvironment(
            final JobRepository jobRepository,
            final JobExecutor jobExecutor,
            final TransactionManager transactionManager,
            final JBeretData data) {

        this.artifactFactory = new QuarkusArtifactFactory(data.getBatchArtifactsAliases());
        this.jobExecutor = jobExecutor;
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
    }

    @Override
    public ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = QuarkusBatchEnvironment.class.getClassLoader();
        }
        return cl;
    }

    @Override
    public ArtifactFactory getArtifactFactory() {
        return artifactFactory;
    }

    @Override
    public void submitTask(JobTask jobTask) {
        jobExecutor.execute(jobTask);
    }

    @Override
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public JobRepository getJobRepository() {
        return jobRepository;
    }

    @Override
    public JobXmlResolver getJobXmlResolver() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Properties getBatchConfigurationProperties() {
        return PROPS;
    }

    @Override
    public String getApplicationName() {
        return ConfigProvider.getConfig().getConfigValue("quarkus.application.name").getValue();
    }

    static class QuarkusArtifactFactory extends AbstractArtifactFactory {
        private final Map<String, String> aliases;

        public QuarkusArtifactFactory(final Map<String, String> aliases) {
            this.aliases = aliases;
        }

        @Override
        public Object create(String ref, Class<?> cls, ClassLoader classLoader) {
            BeanManager bm = Arc.container().beanManager();
            Bean<?> bean = bm.resolve(bm.getBeans(aliases.getOrDefault(ref, ref)));
            return bean == null ? null : bm.getReference(bean, Object.class, bm.createCreationalContext(bean));
        }

        @Override
        public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
            BeanManager bm = Arc.container().beanManager();
            Bean<?> bean = bm.resolve(bm.getBeans(aliases.getOrDefault(ref, ref)));
            return bean == null ? null : bean.getBeanClass();
        }
    }
}
