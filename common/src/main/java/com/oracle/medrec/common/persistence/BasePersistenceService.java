package com.oracle.medrec.common.persistence;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import javax.persistence.EntityManager;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * Basic persistence service.
 *
 * @author Xiaojun Wu. <br>
 *         Copyright (c) 2007, 2017, Oracle and/or its
 *         affiliates. All rights reserved.
 */
//@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@ApplicationScoped
public abstract class BasePersistenceService {

    private static final Logger logger = Logger.getLogger(BasePersistenceService.class.getName());

    @Inject
    protected EntityManager entityManager;

    /**
     * Just for test.
     *
     * @return
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Just for test.
     *
     * @param entityManager
     */
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * EntityManager persists entity.
     *
     * @param object
     */
    protected void save(Object object) {
        try {
            entityManager.persist(object);
        } catch (ConstraintViolationException e) {
            throw handleValidationError(e);
        }
    }

    /**
     * EntityManager merges entity.
     *
     * @param object
     */
    protected <E> E update(E object) {
        logger.finest("entityManager: " + entityManager);
        try {
            return entityManager.merge(object);
        } catch (ConstraintViolationException e) {
            throw handleValidationError(e);
        }
    }

    private PersistenceException handleValidationError(ConstraintViolationException e) {
        StringBuilder sb =
            new StringBuilder("There are " + e.getConstraintViolations().size() + " issues of entity by " +
                              "validation.");
        int i = 1;
        for (ConstraintViolation<?> cv : e.getConstraintViolations()) {
            sb.append(" ")
              .append(i++)
              .append(". ");
            sb.append(cv.getPropertyPath());
            sb.append("(")
              .append(cv.getInvalidValue())
              .append("): ");
            sb.append(cv.getMessage());
        }
        return new PersistenceException(sb.toString());
    }

}
