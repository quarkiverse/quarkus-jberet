/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.quarkiverse.jberet.it.scopes.stepscoped;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.batch.runtime.BatchStatus;
import jakarta.batch.runtime.context.StepContext;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jberet.testapps.cdiscopes.stepscoped.Foo;

@Named
@Dependent
@Alternative
@Priority(1)
@Typed(org.jberet.testapps.cdiscopes.stepscoped.StepScopePartitionAnalyzer.class)
public class StepScopePartitionAnalyzer extends org.jberet.testapps.cdiscopes.stepscoped.StepScopePartitionAnalyzer {
    @Inject
    private Foo foo;

    @Inject
    private StepContext stepContext;

    static final int numberOfPartitions = 3;
    private int numberOfPartitionsFinished;

    @Override
    public void analyzeStatus(final BatchStatus batchStatus, final String exitStatus) throws Exception {
        if (++numberOfPartitionsFinished == numberOfPartitions) {
            final List<String> stepNames = foo.getStepNames();

            // 3 entries from 3 partitions, plus step listener before step (after step not
            // executed yet)
            if (stepNames.size() == 4) {
                stepContext.setExitStatus(stepNames.toString());
            } else {
                throw new IllegalStateException("Expecting 4 elements, but got " + stepNames);
            }
        }
    }
}
