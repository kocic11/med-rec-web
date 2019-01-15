/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.oracle.medrec.patient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Random;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.oracle.medrec.model.Patient;
import com.oracle.medrec.model.PersonName;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.persistence.jpa.jpql.Assert;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.helidon.microprofile.server.Server;

class PatientMainTest {
    private static Server server;
    private static Config config = ConfigProvider.getConfig();
    private static String ssn;
    private static String lastName;
    private static String patientId;
    private static Patient patient = new Patient();
    private final static Logger logger = Logger.getLogger(PatientMainTest.class.getName());

    @BeforeAll
    public static void startTheServer() throws Exception {
        LogManager.getLogManager().readConfiguration(PatientMain.class.getResourceAsStream("/logging.properties"));
        server = PatientMain.startServer();
    }

    @BeforeEach
    public void createPatient() {
        Random random = new Random();
        ssn = String.valueOf(100000000 + random.nextInt(900000000));
        patient.setSsn(ssn);
        PersonName patientName = new PersonName();
        patientName.setFirstName("FirstName");
        lastName = config.getValue("test.patient.lastName", String.class);
        patientName.setLastName(lastName);
        patient.setName(patientName);
        patient.setStatus(Patient.Status.APPROVED);
        patient.setUsername("username" + ssn);
        patient.setPassword("password");
        patient.setEmail("email" + ssn + "@oracle.com");
        patient.setGender(Patient.Gender.MALE);
        logger.finest(patient.toString());

        String location = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).request()
                .post(Entity.entity(patient, MediaType.APPLICATION_JSON)).getHeaderString("Location");

        logger.finest("Location: " + location);
        if (location != null && !location.isEmpty()) {
            patientId = location.substring(location.lastIndexOf("/") + 1, location.length());
        }
    }

    @AfterEach
    public void denyPatient() {
        ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).path(patientId).path("status")
                .request().build("PATCH", Entity.entity(Patient.Status.DENIED.toString(), MediaType.APPLICATION_JSON))
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).invoke();
    }

    @Test
    public void testFindPatientsByLastNameAndSsn() {
        logger.finest("ssn: " + ssn);
        logger.finest("lastName: " + lastName);
        JsonArray response = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .queryParam("lastName", lastName).queryParam("ssn", ssn).request().get(JsonArray.class);
        logger.finest("response: " + response);
        Assertions.assertEquals(1, response.size(), "Size of 1");
        Assertions.assertTrue(ssn.equals(response.getJsonObject(0).getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testFindPatientBySsn() {
        JsonArray jsonArray = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .queryParam("ssn", ssn).request().get(JsonArray.class);
        Assertions.assertEquals(1, jsonArray.size(), "Size of 1");
        Assertions.assertTrue(ssn.equals(jsonArray.getJsonObject(0).getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testFindPatientByLastName() {
        JsonArray jsonArray = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .queryParam("lastName", lastName).request().get(JsonArray.class);
        Assertions.assertEquals(1, jsonArray.size(), "Size of 1");
        Assertions.assertTrue(ssn.equals(jsonArray.getJsonObject(0).getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testGetPatient() {
        JsonObject jsonObject = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .path(patientId).request().get(JsonObject.class);
        Assertions.assertTrue(ssn.equals(jsonObject.getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testApprovePatient() {
        Response response = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).path(patientId)
                .path("status").request()
                .build("PATCH", Entity.entity(Patient.Status.APPROVED.toString(), MediaType.APPLICATION_JSON))
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).invoke();
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Approve status code");
        JsonObject jsonObject = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .path(patientId).request().get(JsonObject.class);
        Assertions.assertEquals(Patient.Status.APPROVED.toString(), jsonObject.getString("status"), "Patient status");
    }

    @Test
    void testDenyPatient() {
        Response response = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).path(patientId)
                .path("status").request()
                .build("PATCH", Entity.entity(Patient.Status.DENIED.toString(), MediaType.APPLICATION_JSON))
                .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true).invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Deny status code");
        JsonObject jsonObject = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
                .path(patientId).request().get(JsonObject.class);
        assertEquals(Patient.Status.DENIED.toString(), jsonObject.getString("status"), "Patient status");
    }

    // @Test
    // void testUpdatePatient() {
    //     JsonObject jsonObject = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients"))
    //             .path(patientId).request().get(JsonObject.class);
    //     System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " + jsonObject);
    //     ObjectMapper objectMapper = new ObjectMapper();
    //     objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    //     objectMapper.setVisibility(
    //             VisibilityChecker.Std.defaultInstance().withFieldVisibility(JsonAutoDetect.Visibility.ANY));
    //     try {
    //         Patient patient = objectMapper.readValue(jsonObject.toString(), Patient.class);
    //         patient.setGender(Patient.Gender.FEMALE);
    //         patient.setVersion(patient.getVersion() + 1);
    //         System. out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " + patient.getGender().toString());
    //         ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).path(patientId).request()
    //                 .put(Entity.entity(patient, MediaType.APPLICATION_JSON));
    //         jsonObject = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients")).path(patientId)
    //                 .request().get(JsonObject.class);
    //         System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " + jsonObject);
    //         assertEquals(patient.getGender(), jsonObject.getString("gender"));
    //     } catch (IOException e) {
    //         fail(e.getMessage());
    //     }

    // }

    @Test
    void testAuthenticatePatient() {
        Response response = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients/authenticate"))
                .request().post(Entity.entity(new UserCredentials(patient.getUsername(), patient.getPassword()),
                        MediaType.APPLICATION_JSON));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Authenticate status code");
    }

    @Test
    void testAuthenticatePatientFail() {
        Response response = ClientBuilder.newClient().target(getConnectionString("/api/v1/patients/authenticate"))
                .request()
                .post(Entity.entity(new UserCredentials("FakeName", "FakePassword"), MediaType.APPLICATION_JSON));
        Assertions.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(),
                "Authenticate status code");
    }

    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
    }

    private static String getConnectionString(String path) {
        return "http://localhost:" + server.getPort() + path;
        // return "http://localhost:8081" + path;
    }
}
