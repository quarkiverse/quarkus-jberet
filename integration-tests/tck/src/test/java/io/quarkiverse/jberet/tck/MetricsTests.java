/*
 * Copyright 2012 International Business Machines Corp.
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.quarkiverse.jberet.tck;

import static io.quarkiverse.jberet.tck.JobOperatorSetter.setJobOperator;

import jakarta.batch.operations.JobOperator;
import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MetricsTests extends com.ibm.jbatch.tck.tests.jslxml.MetricsTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testMetricsInApp() throws Exception {
        super.testMetricsInApp();
    }

    @Test
    public void testMetricsSkipRead() throws Exception {
        super.testMetricsSkipRead();
    }

    @Test
    public void testMetricsSkipWrite() throws Exception {
        super.testMetricsSkipWrite();
    }

    @Test
    public void testMetricsSkipProcess() throws Exception {
        super.testMetricsSkipProcess();
    }

    @Test
    public void testReadMetric() throws Exception {
        super.testReadMetric();
    }

    @Test
    public void testWriteMetric() throws Exception {
        super.testWriteMetric();
    }

    @Test
    public void testMetricsFilterCount() throws Exception {
        super.testMetricsFilterCount();
    }

    @Test
    public void testMetricsCommitCount() throws Exception {
        super.testMetricsCommitCount();
    }

    @Test
    public void testMetricsStepTimestamps() throws Exception {
        super.testMetricsStepTimestamps();
    }

    @Test
    public void testMetricsJobExecutionTimestamps() throws Exception {
        super.testMetricsJobExecutionTimestamps();
    }
}
