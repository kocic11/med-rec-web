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

import com.oracle.medrec.common.persistence.CriteriaPersistenceSupport;
import com.oracle.medrec.common.persistence.PredicationFactory;
import com.oracle.medrec.model.Patient;
import com.oracle.medrec.service.DuplicateSsnException;
import com.oracle.medrec.service.DuplicateUsernameException;
import com.oracle.medrec.service.PatientService;
import com.oracle.medrec.service.impl.BaseUserServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;


/**
 * Provider for Patients.
 */
@ApplicationScoped
public class PatientProvider extends BaseUserServiceImpl<Patient> implements PatientService {

    private Patient patient;
    @Produces
    EntityManager entityManager;
    CriteriaBuilder criteriaBuilder;
    
    private Config config = ConfigProvider.getConfig();
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    
    public PatientProvider() {
        super();
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(config.getValue("persistence.unit.name", String.class));
        entityManager = entityManagerFactory.createEntityManager();
        criteriaBuilder = entityManager.getCriteriaBuilder();
    }

    @Override
    public void createPatient(Patient patient) throws DuplicateUsernameException, DuplicateSsnException {
        isDuplicateSsn(patient);
        entityManager.getTransaction().begin();
        entityManager.persist(patient);
        entityManager.getTransaction().commit();
    }

    @Override
    public Patient getPatient(Long patientId) {
        // TODO Implement this method
        return this.patient;
    }

    @Override
    public Patient findApprovedPatientBySsn(String ssn) {
        // TODO Implement this method
        return this.patient;
    }

    @Override
    public List<Patient> findApprovedPatientsByLastName(String lastName) {
        // TODO Implement this method
        List<Patient> patients = new ArrayList<>();
        patients.add(patient);
        return patients;
    }

    @Override
    public List<Patient> fuzzyFindApprovedPatientsByLastNameAndSsn(String lastName, String ssn) {
        return CriteriaPersistenceSupport.find(entityManager, criteriaBuilder, entityClass, 0, 10,
                                               PredicationFactory.createEqualPredication(Patient.Status.APPROVED,
                                                                                         "status"),
                                               PredicationFactory.createLikePredication(lastName + "%", "name",
                                                                                        "lastName"),
                                               PredicationFactory.createLikePredication(ssn + "%", "ssn"));
    }

    @Override
    public boolean authenticatePatient(String username, String password) {
        // TODO Implement this method
        return true;
    }

    @Override
    public void approvePatient(Long patientId) {
        // TODO Implement this method
    }

    @Override
    public List<Patient> getNewlyRegisteredPatients() {
        // TODO Implement this method
        List<Patient> patients = new ArrayList<>();
        patients.add(patient);
        return patients;
    }

    @Override
    public void denyPatient(Long patientId) {
        // TODO Implement this method
    }

    @Override
    public Patient authenticateAndReturnPatient(String username, String password) {
        // TODO Implement this method
        return patient;
    }

    @Override
    public Patient updatePatient(Patient patient) throws DuplicateSsnException {
        // TODO Implement this method
        this.patient = patient;
        return patient;
    }

    /**
     * Find out if the ssn of this patient has existed in database. If it has,
     * throw @{link.DuplicateSsnException}.
     *
     * @param patient
     * @throws com.oracle.medrec.service.DuplicateSsnException
     */
    private void isDuplicateSsn(Patient patient) throws DuplicateSsnException {
        // count patient with this ssn
        int ssnExistedAmount =
            CriteriaPersistenceSupport.count(entityManager, criteriaBuilder, entityClass,
                                             PredicationFactory.createEqualPredication(patient.getSsn(), "ssn"));
        // if the very ssn has existed in database
        if (ssnExistedAmount > 0) {
            throw new DuplicateSsnException(patient.getSsn());
        }
    }
}
