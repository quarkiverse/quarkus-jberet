/*
 * Copyright (c) 2015 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.quarkiverse.jberet.it.scopes.partitionscoped;

import java.lang.reflect.Field;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Set;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Named;

@Named
@Dependent
@Alternative
@Priority(1)
@Typed(org.jberet.testapps.cdiscopes.partitionscoped.PartitionScopePartitionAnalyzer.class)
public class PartitionScopePartitionAnalyzer
        extends org.jberet.testapps.cdiscopes.partitionscoped.PartitionScopePartitionAnalyzer {
    static public Set<String> getExpectedData() {
        try {
            Field f = org.jberet.testapps.cdiscopes.partitionscoped.PartitionScopePartitionAnalyzer.class
                    .getDeclaredField("expectedData");
            f.setAccessible(true);
            return (Set<String>) f.get(null);
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new UndeclaredThrowableException(e.getCause());
        }
    }
}
