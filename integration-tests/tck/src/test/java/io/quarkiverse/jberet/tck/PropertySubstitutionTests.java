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

import javax.batch.operations.JobOperator;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PropertySubstitutionTests extends com.ibm.jbatch.tck.tests.jslxml.PropertySubstitutionTests {
    @Inject
    JobOperator jobOperator;

    @BeforeEach
    void beforeEach() throws Exception {
        setUp();
        setJobOperator(this, jobOperator);
    }

    @Test
    public void testBatchArtifactPropertyInjection() throws Exception {
        super.testBatchArtifactPropertyInjection();
    }

    @Test
    public void testInitializedPropertyIsOverwritten() throws Exception {
        super.testInitializedPropertyIsOverwritten();
    }

    @Test
    public void testPropertyWithJobParameter() throws Exception {
        super.testPropertyWithJobParameter();
    }

    @Test
    public void testDefaultPropertyName() throws Exception {
        super.testDefaultPropertyName();
    }

    @Test
    public void testGivenPropertyName() throws Exception {
        super.testGivenPropertyName();
    }

    @Test
    public void testPropertyInnerScopePrecedence() throws Exception {
        super.testPropertyInnerScopePrecedence();
    }

    @Test
    public void testPropertyQuestionMarkSimple() throws Exception {
        super.testPropertyQuestionMarkSimple();
    }

    @Test
    public void testPropertyQuestionMarkComplex() throws Exception {
        super.testPropertyQuestionMarkComplex();
    }

    @Test
    public void testPropertyWithConcatenation() throws Exception {
        super.testPropertyWithConcatenation();
    }

    @Test
    public void testJavaSystemProperty() throws Exception {
        super.testJavaSystemProperty();
    }
}
