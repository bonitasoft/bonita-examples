/**
 * Copyright (C) 2019 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.example;

import org.bonitasoft.engine.api.APIClient;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.test.TestEngine;
import org.bonitasoft.engine.test.TestEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    /**
     * Tenant technical user username
     */
    private static final String TECHNICAL_USER_NAME = "install";

    /**
     * Tenant technical user password
     */
    private static final String TECHNICAL_PASSWORD = "install";

    /**
     * End user username
     */
    private static final String USER_NAME = "walter.bates";

    /**
     * End user password
     */
    private static final String PWD = "bpm";

    /**
     * Actor name used in the process example
     */
    private static final String ACTOR_NAME = "MyActor";

    /**
     * The maximum number of elements retrieved by paged requests
     */
    private static int PAGE_SIZE = 5;

    private static Logger LOGGER = LoggerFactory.getLogger(App.class);

    private TestEngine testEngine = TestEngineImpl.getInstance();

    private void startEngine() throws Exception {
        // Do not drop the whole platform each time Bonita Engine starts! :
        testEngine.setDropOnStart(false);
        // Do not drop the whole platform each time Bonita Engine stops! :
        testEngine.setDropOnStop(false);

        final boolean start = testEngine.start();
        if (start) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    stopEngine();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }));
        }
    }

    private void stopEngine() throws Exception {
        LOGGER.warn("Stopping Bonita Engine...");
        testEngine.stop();
    }

    public static void main(String[] args) throws Exception {
        final App app = new App();
        app.startEngine();
        app.doSomeStuff();

        System.exit(0); // clean shutdown will be handled by the shutdown hook
    }

    private void doSomeStuff() throws Exception {

        // create a user that will deploy and execute processes
        User user = createUser();

        // deploy a process
        ProcessDefinition processDefinition = deployProcess();

        startNewProcessInstance(processDefinition.getId());

        // Simulate a human wait for the first human step to be available:
        Thread.sleep(400);

        // Do some actions on the process instance / on the tasks...

        // clean all information at the end ...

        // undeploy the process
        undeployProcess(processDefinition);

        // delete the created user
        deleteUser(user);

        // perform all necessary actions to delete the platform
//        deletePlatform();

        LOGGER.info("Completed successfully!!!");
    }


    /**
     * Deploy a process example
     *
     * @return the deployed process
     * @throws BonitaException if an exception occurs during process deployment
     */
    private ProcessDefinition deployProcess() throws BonitaException {
        // log in with the end user previously created
        APIClient apiClient = doTenantLogin(USER_NAME, PWD);
        try {
            LOGGER.info("Deploying process ... ");
            // build the process example
            DesignProcessDefinition designProcessDefinition = buildProcessDefinition();
            // deploy the process
            ProcessDefinition processDefinition = apiClient.getProcessAPI().deploy(designProcessDefinition);
            LOGGER.info("Process deployed!");

            LOGGER.info("Mapping actors ... ");
            // map the process actor to the end user. Before enabling the process all actors must be mapped.
            apiClient.getProcessAPI().addUserToActor(ACTOR_NAME, processDefinition, apiClient.getSession().getUserId());
            LOGGER.info("Actors mapped!");

            LOGGER.info("Enabling process ... ");
            // enable the process. Up to now this process can be instantiated.
            apiClient.getProcessAPI().enableProcess(processDefinition.getId());
            LOGGER.info("Process enabled!");
            return processDefinition;

        } finally {
            // do logout
            doTenantLogout(apiClient);
        }
    }

    private ProcessInstance startNewProcessInstance(long processDefinitionId) throws BonitaException {
        APIClient apiClient = doTenantLogin(USER_NAME, PWD);
        try {
            LOGGER.info("Starting new process ... ");
            final ProcessInstance processInstance = apiClient.getProcessAPI().startProcess(processDefinitionId);
            LOGGER.info("Process started.");
            return processInstance;
        } finally {
            // do logout
            doTenantLogout(apiClient);
        }
    }

    /**
     * Build a simple process: Start -> My Automatic Step -> My first step -> My second step -> End
     *
     * @return the built process
     */
    private DesignProcessDefinition buildProcessDefinition() throws InvalidProcessDefinitionException {
        String startName = "Start";
        String firstUserStepName = "My first step";
        String secondUserStepName = "My second step";
        String autoStepName = "My Automatic Step";
        String endName = "End";

        // create a new process definition with name and version
        ProcessDefinitionBuilder pdb = new ProcessDefinitionBuilder().createNewInstance("My first process", "1.0");
        // add actor defined as initiator
        pdb.addActor(ACTOR_NAME, true);
        // add a start event
        pdb.addStartEvent(startName);
        // add an automatic task
        pdb.addAutomaticTask(autoStepName);
        // add a user task having the previously defined actor
        pdb.addUserTask(firstUserStepName, ACTOR_NAME);
        // add another user task assigned to the previously defined actor
        pdb.addUserTask(secondUserStepName, ACTOR_NAME);
        // add an end event
        pdb.addEndEvent(endName);
        // defined transitions
        pdb.addTransition(startName, autoStepName);
        pdb.addTransition(autoStepName, firstUserStepName);
        pdb.addTransition(firstUserStepName, secondUserStepName);
        pdb.addTransition(secondUserStepName, endName);

        return pdb.done();
    }


    /**
     * Undeploy the process definition
     *
     * @param processDefinition the process definition
     * @throws BonitaException if an exception occurs when undeploying the process
     */
    private void undeployProcess(ProcessDefinition processDefinition) throws BonitaException {
        // login
        APIClient apiClient = doTenantLogin(USER_NAME, PWD);
        try {
            LOGGER.info("Undeploying process...");
            // before deleting a process it is necessary to delete all its instances (opened or archived)
            deleteOpenedProcessInstance(processDefinition, apiClient);
            deleteArchivedProcessInstance(processDefinition, apiClient);

            // disable and delete the process definition
            apiClient.getProcessAPI().disableAndDeleteProcessDefinition(processDefinition.getId());
            LOGGER.info("Process disabled and undeployed!");
        } finally {
            // logout
            doTenantLogout(apiClient);
        }
    }

    /**
     * Delete all opened process instances for the given process definition
     *
     * @param processDefinition the process definition
     * @param apiClient         the current apiClient
     * @throws BonitaException if an exception occurs when deleting process instances
     */
    private void deleteOpenedProcessInstance(ProcessDefinition processDefinition, APIClient apiClient) throws BonitaException {
        // delete opened process instances by block of PAGE_SIZE
        long nbOfDeletedProcess;
        do {
            nbOfDeletedProcess = apiClient.getProcessAPI().deleteProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        LOGGER.info("Deleted opened process instances.");
    }

    /**
     * Delete all archived process instances for the given process definition
     *
     * @param processDefinition the process definition
     * @param apiClient         the current apiClient
     * @throws BonitaException if an exception occurs when deleting process instances
     */
    private void deleteArchivedProcessInstance(ProcessDefinition processDefinition, APIClient apiClient) throws BonitaException {
        // delete archived process instances by block of PAGE_SIZE
        long nbOfDeletedProcess;
        do {
            nbOfDeletedProcess = apiClient.getProcessAPI().deleteArchivedProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        LOGGER.info("Deleted archived process instances.");
    }

    /**
     * Delete the given user
     *
     * @param user the user to delete
     * @throws BonitaException if an exception occurs when deleting the user
     */
    private void deleteUser(User user) throws BonitaException {
        // In order to delete the only end user, it's necessary to log in as the technical user
        APIClient apiClient = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_USER_NAME);
        try {
            // delete user
            apiClient.getIdentityAPI().deleteUser(user.getId());
            LOGGER.info("Deleted user '" + user.getUserName() + "'.");
        } finally {
            // logout
            doTenantLogout(apiClient);
        }
    }

    /**
     * Create an end user
     *
     * @return the created user
     * @throws BonitaException if an exception occurs during user creation
     */
    private User createUser() throws BonitaException {
        // no end users are created during the platform initialization, so only the technical user is available.
        // logged in as technical user you are able to create the end user that will be able to deploy process, execute tasks, ...
        APIClient apiClient = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_PASSWORD);
        User user;
        try {
            // create end user
            user = apiClient.getIdentityAPI().createUser(USER_NAME, PWD);
            LOGGER.info("Created user '" + USER_NAME + "'.");
        } finally {
            // technical user logs out
            doTenantLogout(apiClient);
        }
        return user;
    }

    private APIClient doTenantLogin(String username, String password) throws BonitaException {
        final APIClient apiClient = new APIClient();
        apiClient.login(username, password);
        LOGGER.info("User '" + username + "' has logged in!");
        return apiClient;
    }

    private void doTenantLogout(APIClient apiClient) throws BonitaException {
        final String userName = apiClient.getSession().getUserName();
        apiClient.logout();
        LOGGER.info("User '" + userName + "' has logged out!");
    }

}
