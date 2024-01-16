package io.quarkiverse.jberet.runtime.scope;

import java.lang.annotation.Annotation;

import jakarta.enterprise.context.spi.Contextual;
import jakarta.enterprise.context.spi.CreationalContext;

import org.jberet.cdi.PartitionScoped;
import org.jberet.creation.PartitionScopedContextImpl;

import io.quarkus.arc.InjectableContext;

public class QuarkusPartitionScopedContextImpl implements InjectableContext {
    private final PartitionScopedContextImpl impl;

    public QuarkusPartitionScopedContextImpl() {
        this.impl = PartitionScopedContextImpl.getInstance();
    }

    @Override
    public void destroy(Contextual<?> contextual) {
        this.impl.destroy(contextual);
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return PartitionScoped.class;
    }

    @Override
    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        return impl.get(contextual, creationalContext);
    }

    @Override
    public <T> T get(Contextual<T> contextual) {
        return impl.get(contextual);
    }

    @Override
    public boolean isActive() {
        return impl.isActive();
    }

    @Override
    public void destroy() {
        this.impl.destroy(null);
    }

    @Override
    public ContextState getState() {
        return null;
    }
}
