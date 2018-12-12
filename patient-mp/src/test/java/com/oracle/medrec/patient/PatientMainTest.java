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

import com.oracle.medrec.model.Patient;

import com.oracle.medrec.model.PersonName;

import io.helidon.microprofile.server.Server;

import java.io.IOException;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;

import javax.inject.Inject;

import javax.json.JsonArray;

import javax.json.JsonObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;

import org.eclipse.microprofile.config.ConfigProvider;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PatientMainTest {
    private static Server server;
    private static Config config = ConfigProvider.getConfig();
    private static String ssn;
    private static String lastName;
    private static Patient patient = new Patient();


    @BeforeAll
    public static void startTheServer() throws Exception {
        server = PatientMain.startServer();
        createPatient();
    }

    @Test
    void testQuery() {
        JsonArray jsonArray = ClientBuilder.newClient()
                                           .target(new StringBuffer(getConnectionString("/api/v1/patients?lastname=")).append(lastName)
                                                                                                                      .append("&ssn=")
                                                                                                                      .append(ssn)
                                                                                                                      .toString())
                                           .request()
                                           .get(JsonArray.class);
        Assertions.assertEquals(1, jsonArray.size(), "Size of 1");
        Assertions.assertTrue(ssn.equals(jsonArray.getJsonObject(0).getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testGetPatient() {
        JsonObject jsonObject = ClientBuilder.newClient()
                                             .target(getConnectionString("/api/v1/patients/1"))
                                             .request()
                                             .get(JsonObject.class);
        Assertions.assertTrue(ssn.equals(jsonObject.getString("ssn")), "SSNs are not equal");
    }

    @Test
    void testApprovePatient() {
        Response response = ClientBuilder.newClient()
                                         .target(getConnectionString("/api/v1/patients/approve/1"))
                                         .request()
                                         .get();
        Assertions.assertEquals(200, response.getStatus(), "Approve status code");
    }

    @Test
    void testAuthenticatePatient() {
        Response response =
            ClientBuilder.newClient()
                                         .target(getConnectionString("/api/v1/patients/authenticate"))
                                         .request()
                                         .post(Entity.entity(new UserCredentials(patient.getUsername(),
                                                                                 patient.getPassword()),
                                                             MediaType.APPLICATION_JSON));
        Assertions.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Authenticate status code");
    }
    
    @Test
    void testAuthenticatePatientFail() {
        Response response =
            ClientBuilder.newClient()
                                         .target(getConnectionString("/api/v1/patients/authenticate"))
                                         .request()
                                         .post(Entity.entity(new UserCredentials("FakeName",
                                                                                 "FakePassword"),
                                                             MediaType.APPLICATION_JSON));
        Assertions.assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Authenticate status code");
    }


    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
    }

    private static String getConnectionString(String path) {
        return "http://localhost:" + server.getPort() + path;
    }

    private static Response createPatient() {
        ssn = String.valueOf(Math.random() * 1000000);
        patient.setSsn(ssn);
        PersonName patientName = new PersonName();
        patientName.setFirstName("FirstName");
        lastName = config.getValue("test.patient.lastName", String.class);
        patientName.setLastName(lastName);
        patient.setName(patientName);
        patient.setStatus(Patient.Status.APPROVED);
        patient.setUsername("username");
        patient.setPassword("password");
        patient.setEmail("email@oracle.com");
        return ClientBuilder.newClient()
                            .target(getConnectionString("/api/v1/patients"))
                            .request()
                            .post(Entity.entity(patient, MediaType.APPLICATION_JSON));
    }
}
