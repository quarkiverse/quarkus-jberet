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
public class StepExecutionTests extends com.ibm.jbatch.tck.tests.jslxml.StepExecutionTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testOneStepExecutionStatus() throws Exception {
        super.testOneStepExecutionStatus();
    }

    @Test
    public void testFourStepExecutionStatus() throws Exception {
        super.testFourStepExecutionStatus();
    }

    @Test
    public void testFailedStepExecutionStatus() throws Exception {
        super.testFailedStepExecutionStatus();
    }

    @Test
    public void testStoppedStepExecutionStatus() throws Exception {
        super.testStoppedStepExecutionStatus();
    }

    @Test
    public void testPersistedStepData() throws Exception {
        super.testPersistedStepData();
    }

    @Test
    public void testStepExecutionExitStatus() throws Exception {
        super.testStepExecutionExitStatus();
    }

    @Test
    public void testStepInFlowStepExecution() throws Exception {
        super.testStepInFlowStepExecution();
    }

    @Test
    public void testStepInFlowInSplitStepExecution() throws Exception {
        super.testStepInFlowInSplitStepExecution();
    }
}
