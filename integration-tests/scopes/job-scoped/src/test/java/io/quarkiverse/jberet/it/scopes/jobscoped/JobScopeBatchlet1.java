/*
 * Copyright (c) 2015-2017 Red Hat, Inc. and/or its affiliates.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package io.quarkiverse.jberet.it.scopes.jobscoped;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Typed;
import jakarta.inject.Named;

@Named
@Dependent
@Alternative
@Priority(1)
@Typed(value = org.jberet.testapps.cdiscopes.jobscoped.JobScopeBatchlet1.class)
public class JobScopeBatchlet1 extends org.jberet.testapps.cdiscopes.jobscoped.JobScopeBatchlet1 {
}
