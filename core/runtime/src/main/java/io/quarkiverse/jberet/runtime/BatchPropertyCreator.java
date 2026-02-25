package io.quarkiverse.jberet.runtime;

import static org.jberet._private.BatchMessages.MESSAGES;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import jakarta.batch.api.BatchProperty;
import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.eclipse.microprofile.config.spi.Converter;
import org.jberet._private.BatchMessages;
import org.jberet.creation.ArtifactCreationContext;
import org.jberet.job.model.Properties;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.impl.InjectionPointProvider;
import io.smallrye.config.Config;

public class BatchPropertyCreator implements BeanCreator<Object> {
    @Override
    public Object create(SyntheticCreationalContext<Object> context) {
        InjectionPoint injectionPoint = InjectionPointProvider.getCurrent(context);

        Annotated annotated = injectionPoint.getAnnotated();
        BatchProperty batchProperty = annotated.getAnnotation(BatchProperty.class);
        String name = batchProperty.name().trim();
        if (name.isEmpty()) {
            name = injectionPoint.getMember().getName();
        }

        if (name.isEmpty()) {
            throw BatchMessages.MESSAGES.batchPropertyNameMissing(injectionPoint.getMember());
        }

        String type = (String) context.getParams().get("type");
        Class<?> klass;
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            klass = classLoader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load required type: " + type);
        }

        Properties properties = ArtifactCreationContext.getCurrentArtifactCreationContext().getProperties();
        String value = properties.get(name);
        if (value == null) {
            return null;
        }

        Optional<? extends Converter<?>> converter = Config.get().getConverter(klass);
        if (converter.isPresent()) {
            return converter.get().convert(value);
        } else {
            throw MESSAGES.unsupportedInjectionType(value, new AnnotatedElement() {
                @Override
                public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
                    return batchProperty.annotationType().getAnnotation(annotationClass);
                }

                @Override
                public Annotation[] getAnnotations() {
                    return new Annotation[0];
                }

                @Override
                public Annotation[] getDeclaredAnnotations() {
                    return new Annotation[0];
                }
            }, klass);
        }
    }
}
