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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import io.helidon.microprofile.server.Server;

/**
 * Main method simulating trigger of main method of the server.
 */
public final class PatientMain {

    private static final Logger logger = LogManager.getLogManager().getLogger(PatientMain.class.getName());

    /**
     * Cannot be instantiated.
     */
    private PatientMain() { }

    /**
     * Application main entry point.
     * @param args command line arguments
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(final String[] args) throws IOException {
        setupLogging();

        Server server = startServer();

        logger.log(Level.INFO, "http://localhost:" + server.getPort());
    }

    /**
     * Start the server.
     * @return the created {@link Server} instance
     */
    static Server startServer() {
        // Server will automatically pick up configuration from
        // microprofile-config.properties
        // and Application classes annotated as @ApplicationScoped
        return Server.create().start();
    }

    /**
     * Configure logging from logging.properties file.
     */
    private static void setupLogging() throws IOException {
        // load logging configuration
        LogManager.getLogManager().readConfiguration(
                PatientMain.class.getResourceAsStream("/logging.properties"));
    }
}
