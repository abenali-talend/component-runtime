/**
 * Copyright (C) 2006-2022 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.talend.sdk.component.tools.validator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.ClassesArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.talend.sdk.component.api.configuration.action.Proposable;
import org.talend.sdk.component.api.configuration.action.Updatable;
import org.talend.sdk.component.api.configuration.type.DataSet;
import org.talend.sdk.component.api.configuration.type.DataStore;
import org.talend.sdk.component.api.record.Schema;
import org.talend.sdk.component.api.service.Service;
import org.talend.sdk.component.api.service.completion.DynamicValues;
import org.talend.sdk.component.api.service.completion.Values;
import org.talend.sdk.component.api.service.discovery.DiscoverDataset;
import org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult;
import org.talend.sdk.component.api.service.discovery.DiscoverDatasetResult.DatasetDescription;
import org.talend.sdk.component.api.service.healthcheck.HealthCheck;
import org.talend.sdk.component.api.service.healthcheck.HealthCheckStatus;
import org.talend.sdk.component.api.service.schema.DiscoverSchema;
import org.talend.sdk.component.api.service.update.Update;

class ActionValidatorTest {

    @Test
    void validateDatasetDiscoverAction() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());
        AnnotationFinder finderKO = new AnnotationFinder(new ClassesArchive(ActionDatasetDiscoveryKo.class));
        final Stream<String> errorsStreamKO =
                validator.validate(finderKO, Arrays.asList(ActionDatasetDiscoveryKo.class));

        final String error = errorsStreamKO.collect(Collectors.joining(" / "));
        final String expected =
                "public java.lang.String org.talend.sdk.component.tools.validator.ActionValidatorTest$ActionDatasetDiscoveryKo.discoverBadReturnType() should have a datastore as first parameter (marked with @DataStore)";
        Assertions.assertEquals(expected, error);
    }

    @Test
    void validate() {
        final ActionValidator validator = new ActionValidator(new FakeHelper());

        AnnotationFinder finder =
                new AnnotationFinder(new ClassesArchive(ActionClassOK.class, FakeDataSet.class, FakeDataStore.class));
        final Stream<String> errorsStream =
                validator.validate(finder, Arrays.asList(ActionClassOK.class, FakeDataSet.class, FakeDataStore.class));
        final List<String> errors = errorsStream.collect(Collectors.toList());
        Assertions.assertTrue(errors.isEmpty(), () -> errors.get(0) + " as first error");

        AnnotationFinder finderKO = new AnnotationFinder(new ClassesArchive(ActionClassKO.class));
        final Stream<String> errorsStreamKO = validator.validate(finderKO, Arrays.asList(ActionClassKO.class));
        final List<String> errorsKO = errorsStreamKO.collect(Collectors.toList());
        Assertions.assertEquals(6, errorsKO.size(), () -> errorsKO.get(0) + " as first error");

        Assertions
                .assertAll(() -> assertContains(errorsKO,
                        "hello() doesn't return a class org.talend.sdk.component.api.service.completion.Values"),
                        () -> assertContains(errorsKO, "is not declared into a service class"),
                        () -> assertContains(errorsKO,
                                "health() should have its first parameter being a datastore (marked with @DataStore)"),
                        () -> assertContains(errorsKO, "updatable() should return an object"));
    }

    private void assertContains(List<String> errors, String contentPart) {
        final boolean present =
                errors.stream().filter((String err) -> err.contains(contentPart)).findFirst().isPresent();
        Assertions.assertTrue(present, "Errors don't have '" + contentPart + "'");
    }

    static class FakeData {

    }

    @DataStore
    static class FakeDataStore {

    }

    @DataSet
    static class FakeDataSet {

        @Proposable("testOK")
        private String prop;

        @Updatable("updatable")
        private FakeData data;
    }

    @Service
    static class ActionClassOK {

        @DynamicValues("testOK")
        public Values hello() {
            return new Values(Arrays.asList(new Values.Item("", "")));

        }

        @HealthCheck("tests")
        public HealthCheckStatus health(FakeDataStore dataStore) {
            return new HealthCheckStatus(HealthCheckStatus.Status.OK, "ok");
        }

        @DiscoverSchema("tests")
        public Schema discover(FakeDataSet ds) {
            return null;
        }

        @Update("updatable")
        public FakeData updatable() {
            return new FakeData();
        }

        @DiscoverDataset("tests")
        public DiscoverDatasetResult datasetDiscover(FakeDataStore dataStore) {
            final DatasetDescription datasetA = new DatasetDescription("datasetA");
            datasetA.addMetadata("type", "typeA");
            final DatasetDescription datasetB = new DatasetDescription("datasetB");
            datasetA.addMetadata("type", "typeB");
            return new DiscoverDatasetResult(Arrays.asList(datasetA, datasetB));
        }
    }

    static class ActionClassKO {

        @DynamicValues("testKO")
        public String hello() {
            return "";
        }

        @HealthCheck("tests")
        public HealthCheckStatus health() {
            return new HealthCheckStatus(HealthCheckStatus.Status.OK, "ok");
        }

        @Update("updatable")
        public String updatable() {
            return "";
        }
    }

    static class ActionDatasetDiscoveryKo {

        @DiscoverDataset
        public String discoverBadReturnType() {
            return "Should return " + DiscoverDatasetResult.class;
        }
    }
}