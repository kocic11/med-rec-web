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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.oracle.medrec.model.Patient;
import com.oracle.medrec.service.DuplicateSsnException;
import com.oracle.medrec.service.DuplicateUsernameException;

/**
 *
 * The patients are created, upated, deleted, and queired.
 *
 */
@Path("/patients")
@RequestScoped
public class PatientResource {
    private final static Logger logger = Logger.getLogger(PatientResource.class.getName());

    /**
     * The patient provider.
     */
    private final PatientProvider patientProvider;

    public PatientResource() {
        this.patientProvider = new PatientProvider();
    }

    /**
     * Return selected patients.
     *
     * @return {@link JsonObject}
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPatients(@Context UriInfo uriInfo) {

        String lastName = uriInfo.getQueryParameters().getFirst("lastName");
        String ssn = uriInfo.getQueryParameters().getFirst("ssn");
        logger.finest("lastName: " + lastName);
        logger.finest("ssn: " + ssn);
        List<Patient> patients = new ArrayList<Patient>();

        if (lastName == null) {
            patients.add(patientProvider.findApprovedPatientBySsn(ssn));
            return Response.ok(patients).type(MediaType.APPLICATION_JSON)
                    .header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                    .header("Access-Control-Expose-Headers", "*").build();
        }

        if (ssn == null) {
            patients.addAll(patientProvider.findApprovedPatientsByLastName(lastName));
            return Response.ok(patients)
                    .type(MediaType.APPLICATION_JSON).header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Credentials", "true")
                    .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                    .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                    .header("Access-Control-Expose-Headers", "*").build();
        }
        
        patients.addAll(patientProvider.fuzzyFindApprovedPatientsByLastNameAndSsn(lastName, ssn));
        logger.finest("List of patients: " + patients.toString());
        return Response.ok(patients)
                .type(MediaType.APPLICATION_JSON).header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Credentials", "true")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .header("Access-Control-Expose-Headers", "*").build();
    }

    /**
     * Return a patient.
     *
     * @return {@link JsonObject}
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Patient getPatientById(@PathParam("id") String patientId) {
        logger.finest("id: " + patientId);
        return patientProvider.getPatient(Long.valueOf(patientId));
    }

    /**
     * Return response.
     *
     * @return {@link Response}
     */
    @PATCH
    @Path("/{id}/status")
    public Response approvePatient(@PathParam("id") String patientId, String status) {
        logger.finest("id: " + patientId);
        logger.finest("status: " + status);
        try {
            if (Patient.Status.APPROVED.toString().equals(status)) {
                patientProvider.approvePatient(Long.valueOf(patientId));
            } else {
                patientProvider.denyPatient(Long.valueOf(patientId));
            }
        } catch (Exception e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        }
        return Response.ok().build();
    }

    /**
     * Create a patient.
     *
     * @param patient the patient to create
     * @return {@link Response}
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPatient(Patient patient, @Context UriInfo uriInfo) {
        try {
            return Response
                    .created(new URI(uriInfo.getPath() + "/" + patientProvider.createPatient(patient).toString()))
                    .build();
        } catch (DuplicateSsnException | DuplicateUsernameException e) {
            return Response.status(Response.Status.CONFLICT).entity(patient).build();
        } catch (Exception e) {
            return Response.serverError().type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
        }
    }

    @POST
    @Path("/authenticate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response authenticatePatient(UserCredentials credentials) {

        String username = credentials.getUsername();
        String password = credentials.getPassword();
        logger.finest("username: " + username);
        logger.finest("password: " + password);
        if (patientProvider.authenticatePatient(username, password)) {
            return Response.ok().build();
        }

        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

}
