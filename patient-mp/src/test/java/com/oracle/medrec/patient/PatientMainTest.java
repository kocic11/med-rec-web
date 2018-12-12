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
    private Config config = ConfigProvider.getConfig();


    @BeforeAll
    public static void startTheServer() throws Exception {
        server = PatientMain.startServer();
    }

    @Test
    void testPost() {
        Client client = ClientBuilder.newClient();

        Patient patient = new Patient();
        String ssn = config.getValue("test.patient.ssn", String.class);
        patient.setSsn(ssn);
        PersonName patientName = new PersonName();
        patientName.setFirstName(config.getValue("test.patient.firstName", String.class));
        String lastName = config.getValue("test.patient.lastName", String.class);
        patientName.setLastName(lastName);
        patient.setName(patientName);
        patient.setStatus(Patient.Status.APPROVED);
        Response response = client.target(getConnectionString("/api/v1/patients"))
                                  .request()
                                  .post(Entity.entity(patient, MediaType.APPLICATION_JSON));

        String message = response.readEntity(String.class);
        System.out.println(message);
        Assertions.assertEquals(Response.Status
                                        .ACCEPTED
                                        .getStatusCode(), response.getStatus(), "Status code");

        JsonArray jsonArray = client.target(new StringBuffer(getConnectionString("/api/v1/patients?lastname=")).append(lastName)
                                                                                                                     .append("&ssn=")
                                                                                                                     .append(ssn)
                                                                                                                     .toString())
                                    .request()
                                    .get(JsonArray.class);
        Assertions.assertEquals(1, jsonArray.size(), "Size of 1");
    }

    //    @Test
    //    void testQuery() {
    //
    //        Client client = ClientBuilder.newClient();
    //        JsonArray jsonArray = client.target(getConnectionString("/api/v1/patients/query?lastname=LastName&ssn=111222333"))
    //                                    .request()
    //                                    .get(JsonArray.class);
    //        Assertions.assertEquals(1, jsonArray.size(), "Size of 1");
    //    }

    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
    }

    private String getConnectionString(String path) {
        return "http://localhost:" + server.getPort() + path;
    }
}
