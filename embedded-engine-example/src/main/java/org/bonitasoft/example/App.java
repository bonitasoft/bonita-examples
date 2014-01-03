/**
 * Copyright (C) 2014 BonitaSoft S.A.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.bonitasoft.engine.api.IdentityAPI;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.PlatformAPIAccessor;
import org.bonitasoft.engine.api.PlatformLoginAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.ProcessRuntimeAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceCriterion;
import org.bonitasoft.engine.bpm.flownode.ActivityInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.Order;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.PlatformSession;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {

    /**
     * Bonita home property key
     */
    private static final String BONITA_HOME_KEY = "bonita.home";

    /**
     * Platform administrator user name
     */
    private static final String PLATFORM_PASSWORD = "platform";

    /**
     * Platform administrator password
     */
    private static final String PLATFORM_ADMIN = "platformAdmin";

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

    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) throws Exception {
        checkBonitaHome();
        deployDataSource();
        createPlatform();
        User user = createUser();
        ProcessDefinition processDefinition = deployProcess();
        executeActions(processDefinition);
        undeployProcess(processDefinition);
        deleteUser(user);
        detroyPlatform();
        undeployDataSource();
        System.out.println("Completed sucessfully!!!");
    }

    private static void checkBonitaHome() throws IOException {
        String bonitaHome = System.getProperty(BONITA_HOME_KEY);
        if (bonitaHome == null) {
            throw new RuntimeException("The system property bonita.home is not set: please, set this property with the path to the bonita home folder.\n "
                    + "You can get a bonita home from BonitaBPMCommunity-<bonita.engine.version>-deploy.zip or use the one generate under \n"
                    + "the folder target/home on this project by runing the command 'mvn clean install'.");
        }
    }

    private static void createPlatform() throws BonitaException {
        PlatformSession session = doPlatformLogin(PLATFORM_ADMIN, PLATFORM_PASSWORD);
        try {
            System.out.println("Creating and initializing the platform ...");
            getPlatformAPI(session).createAndInitializePlatform();
            System.out.println("Platform created and initialized!");

            System.out.println("Starting node ...");
            getPlatformAPI(session).startNode();
            System.out.println("Node started!");
        } finally {
            doPlatformLogout(session);
        }
    }

    private static void detroyPlatform() throws BonitaException {
        PlatformSession session = doPlatformLogin(PLATFORM_ADMIN, PLATFORM_PASSWORD);
        try {
            System.out.println("Stopping node ...");
            getPlatformAPI(session).stopNode();
            System.out.println("Node stopped!");

            System.out.println("Cleaning and deleting the platform ...");
            getPlatformAPI(session).cleanAndDeletePlaftorm();;
            System.out.println("Platform cleaned and deleted!");
        } finally {
            doPlatformLogout(session);
        }
    }

    private static ProcessDefinition deployProcess() throws BonitaException {
        // log in with the real user previously created
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Deploying process ... ");
            DesignProcessDefinition designProcessDefinition = buildProcessDefinition();
            ProcessDefinition processDefinition = getProcessAPI(session).deploy(designProcessDefinition);
            System.out.println("Process deployed!");

            System.out.println("Mapping actors ... ");
            getProcessAPI(session).addUserToActor(ACTOR_NAME, processDefinition, session.getUserId());
            System.out.println("Actors mapped!");

            System.out.println("Enabling process ... ");
            getProcessAPI(session).enableProcess(processDefinition.getId());
            System.out.println("Process enabled!");
            return processDefinition;

        } finally {
            doTenantLogout(session);
        }
    }

    /**
     * Build a simple process: Start -> My Automatic Step -> My first step -> My second step -> End
     * 
     * @return the built process
     * @throws InvalidProcessDefinitionException
     */
    private static DesignProcessDefinition buildProcessDefinition() throws InvalidProcessDefinitionException {
        String startName = "Start";
        String firstUserStepName = "My first step";
        String secondUserStepName = "My second step";
        String autoStepName = "My Automatic Step";
        String endName = "End";

        ProcessDefinitionBuilder pdb = new ProcessDefinitionBuilder().createNewInstance("My first process", "1.0");
        pdb.addActor(ACTOR_NAME, true);
        pdb.addStartEvent(startName);
        pdb.addAutomaticTask(autoStepName);
        pdb.addUserTask(firstUserStepName, ACTOR_NAME);
        pdb.addUserTask(secondUserStepName, ACTOR_NAME);
        pdb.addEndEvent(endName);
        pdb.addTransition(startName, autoStepName);
        pdb.addTransition(autoStepName, firstUserStepName);
        pdb.addTransition(firstUserStepName, secondUserStepName);
        pdb.addTransition(secondUserStepName, endName);

        return pdb.done();
    }

    private static void executeActions(ProcessDefinition processDefinition) throws IOException, BonitaException {
        String message = getMenutTextContent();
        String choice = null;
        do {
            choice = readLine(message);
            if ("1".equals(choice)) {
                startProcess(processDefinition);
            } else if ("2".equals(choice)) {
                listOpenProcessInstances();
            } else if ("3".equals(choice)) {
                listArchivedProcessInstances();
            } else if ("4".equals(choice)) {
                listPendingTasks();
            } else if ("5".equals(choice)) {
                executeATask();
            } else if (!"0".equals(choice)) {
                System.out.println("Invalid choice!");
            }
        } while (!"0".equals(choice));
    }

    private static String getMenutTextContent() {
        StringBuilder stb = new StringBuilder("\nChoose the action to be executed:\n");
        stb.append("0 - exit\n");
        stb.append("1 - start a process\n");
        stb.append("2 - list open process instances\n");
        stb.append("3 - list archived process instances\n");
        stb.append("4 - list pending tasks \n");
        stb.append("5 - execute a task\n");
        stb.append("Choice:");
        String message = stb.toString();
        return message;
    }

    private static void startProcess(ProcessDefinition processDefinition) throws BonitaException {
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Instantiating process ... ");
            ProcessInstance processInstance = getProcessAPI(session).startProcess(processDefinition.getId());
            System.out.println("Process instantiated! Id: " + processInstance.getId());
        } finally {
            doTenantLogout(session);
        }
    }

    private static void listOpenProcessInstances() throws BonitaException {
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Active process instances: ");
            int startIndex = 0;
            int page = 1;
            SearchResult<ProcessInstance> result = null;
            do {
                SearchOptionsBuilder optionsBuilder = new SearchOptionsBuilder(startIndex, PAGE_SIZE);
                optionsBuilder.sort(ProcessInstanceSearchDescriptor.ID, Order.ASC);
                result = getProcessAPI(session).searchProcessInstances(optionsBuilder.done());
                printOpenedProcessIntancesPage(page, result);

                startIndex += PAGE_SIZE;
                page++;
            } while (result.getResult().size() == PAGE_SIZE);
        } finally {
            doTenantLogout(session);
        }
    }

    private static void printOpenedProcessIntancesPage(int page, SearchResult<ProcessInstance> result) {
        if (result.getCount() == 0) {
            System.out.println("There are no opened process instances!");
        }
        if (!result.getResult().isEmpty()) {
            System.out.println("----- Page " + page + "-----");
        }
        for (ProcessInstance processInstance : result.getResult()) {
            StringBuilder stb = new StringBuilder();
            stb.append("id: ");
            stb.append(processInstance.getId());
            stb.append(", name: ");
            stb.append(processInstance.getName());
            stb.append(", started on: ");
            stb.append(processInstance.getStartDate());
            System.out.println(stb.toString());
        }
    }

    private static void listArchivedProcessInstances() throws BonitaException {
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Archived process instances: ");
            int startIndex = 0;
            int page = 1;
            SearchResult<ArchivedProcessInstance> result = null;
            do {
                SearchOptionsBuilder optionsBuilder = new SearchOptionsBuilder(startIndex, PAGE_SIZE);
                optionsBuilder.sort(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, Order.ASC);
                result = getProcessAPI(session).searchArchivedProcessInstances(optionsBuilder.done());
                printArchivedProcessInstancePage(page, result);

                startIndex += PAGE_SIZE;
                page++;
            } while (result.getResult().size() == PAGE_SIZE);
        } finally {
            doTenantLogout(session);
        }

    }

    private static void printArchivedProcessInstancePage(int page, SearchResult<ArchivedProcessInstance> result) {
        if (result.getCount() == 0) {
            System.out.println("There are no archived process instances!");
        }
        if (!result.getResult().isEmpty()) {
            System.out.println("----- Page " + page + "-----");
        }
        for (ArchivedProcessInstance processInstance : result.getResult()) {
            StringBuilder stb = new StringBuilder();
            stb.append("id: ");
            stb.append(processInstance.getSourceObjectId());
            stb.append(", name: ");
            stb.append(processInstance.getName());
            stb.append(", started on: ");
            stb.append(processInstance.getStartDate());
            stb.append(", archived on: ");
            stb.append(processInstance.getEndDate());
            System.out.println(stb.toString());
        }
    }

    private static void listPendingTasks() throws BonitaException {
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            ProcessAPI processAPI = getProcessAPI(session);
            // get the list of pending tasks for the logged user.
            int startIndex = 0;
            int page = 1;
            List<HumanTaskInstance> pendingTasks = null;
            do {
                pendingTasks = processAPI.getPendingHumanTaskInstances(session.getUserId(), startIndex, PAGE_SIZE, ActivityInstanceCriterion.LAST_UPDATE_ASC);
                printTasksPage(page, pendingTasks);

                startIndex += PAGE_SIZE;
                page++;
            } while (pendingTasks.size() == PAGE_SIZE);
        } finally {
            doTenantLogout(session);
        }
    }

    private static void printTasksPage(int page, List<HumanTaskInstance> pendingTasks) {
        if (pendingTasks.isEmpty()) {
            if(page == 1) {
                System.out.println("There are no pending tasks!");
            }
        } else {
            System.out.println("----- Page " + page + "-----");
        }
        for (HumanTaskInstance task : pendingTasks) {
            StringBuilder stb = new StringBuilder();
            stb.append("id: ");
            stb.append(task.getId());
            stb.append(", process instance id: ");
            stb.append(task.getRootContainerId());
            stb.append(", task name: ");
            stb.append(task.getName());
            System.out.println(stb.toString());
        }

    }

    private static void executeATask() throws BonitaException, IOException {
        Long taskId = readTaskId();
        if (taskId != -1) {
            APISession session = doTenantLogin(USER_NAME, PWD);
            try {
                ProcessRuntimeAPI processAPI = getProcessAPI(session);
                HumanTaskInstance taskToExecute = processAPI.getHumanTaskInstance(taskId);
                // assign the task
                processAPI.assignUserTask(taskToExecute.getId(), session.getUserId());
                System.out.println("Task '" + taskToExecute.getName() + "' of process instance '" + taskToExecute.getRootContainerId() + "' assigned to '"
                        + session.getUserName() + ".");

                // execute the task
                processAPI.executeFlowNode(taskToExecute.getId());
                System.out.println("Task '" + taskToExecute.getName() + "' of process instance '" + taskToExecute.getRootContainerId() + "' executed by '"
                        + session.getUserName() + ".");
            } catch (ActivityInstanceNotFoundException e) {
                System.out.println("No task found with id " + taskId);
            } finally {
                doTenantLogout(session);
            }
        }
    }

    private static Long readTaskId() throws IOException {
        String message = "Enter the id of task to be executed:";
        String strId = readLine(message);
        long taskId = -1;
        try {
            taskId = Long.parseLong(strId);
        } catch (Exception e) {
            System.out.println(strId + " is not a valid id. You can find all taks's ids using the menu 'list pending tasks'.");
        }
        return taskId;
    }

    private static void undeployProcess(ProcessDefinition processDefinition) throws BonitaException, InterruptedException {
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Undeplyoing process...");
            // before deleting a process is necessary to delete all its instances (opened or archived)
            deleteOpenProcessInstance(processDefinition, session);
            deleteArchivedProcessInstance(processDefinition, session);

            disableAndDeleteProcess(processDefinition, session);
            System.out.println("Process undeployed!");
        } finally {
            doTenantLogout(session);
        }
    }

    public static void deleteArchivedProcessInstance(ProcessDefinition processDefinition, APISession session) throws BonitaException, InterruptedException {
        // delete archived process instances by block of PAGE_SIZE
        long nbOfDeletedProcess = 0;
        do {
            nbOfDeletedProcess = getProcessAPI(session).deleteArchivedProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        System.out.println("Deleted archived processs instances.");
    }

    public static void deleteOpenProcessInstance(ProcessDefinition processDefinition, APISession session) throws BonitaException, InterruptedException {
        // delete opened process instances by block of PAGE_SIZE
        long nbOfDeletedProcess = 0;
        do {
            nbOfDeletedProcess = getProcessAPI(session).deleteProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        System.out.println("Deleted opened processs instances.");
    }

    public static void disableAndDeleteProcess(ProcessDefinition processDefinition, APISession session) throws BonitaException {
        getProcessAPI(session).disableAndDeleteProcessDefinition(processDefinition.getId());
        System.out.println("Process disabled.");
    }

    private static void deleteUser(User user) throws BonitaException {
        // In order to delete the only real user, lets log in with technical user
        APISession session = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_USER_NAME);
        try {
            getIdentityAPI(session).deleteUser(user.getId());;
            System.out.println("Deleted user '" + user.getUserName() + "'.");
        } finally {
            doTenantLogout(session);
        }
    }

    private static User createUser() throws BonitaException {
        // no end users are created during the platform initialization, so only the technical user is available
        // logged in as technical user you are able to create end user that will be able to deploy process, execute tasks, ...
        APISession session = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_PASSWORD);
        User user = null;
        try {
            user = getIdentityAPI(session).createUser(USER_NAME, PWD);
            System.out.println("Created user '" + USER_NAME + "'.");
        } finally {
            doTenantLogout(session);
        }
        return user;
    }

    private static String readLine(String message) throws IOException {
        System.out.println(message);
        String choice = null;
        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        choice = buffer.readLine();
        return choice;
    }

    public static PlatformSession doPlatformLogin(String platformUsername, String password) throws BonitaException {
        return getPlaformLoginAPI().login(platformUsername, password);
    }

    public static void doPlatformLogout(PlatformSession session) throws BonitaException {
        getPlaformLoginAPI().logout(session);
    }

    public static APISession doTenantLogin(String username, String password) throws BonitaException {
        APISession session = getLoginAPI().login(username, password);
        System.out.println("User '" + username + "' has logged in!");
        return session;
    }

    public static void doTenantLogout(APISession session) throws BonitaException {
        getLoginAPI().logout(session);
        System.out.println("User '" + session.getUserName() + "' has logged out!");
    }

    private static LoginAPI getLoginAPI() throws BonitaException {
        return TenantAPIAccessor.getLoginAPI();
    }

    private static PlatformLoginAPI getPlaformLoginAPI() throws BonitaException {
        return PlatformAPIAccessor.getPlatformLoginAPI();
    }

    private static PlatformAPI getPlatformAPI(PlatformSession platformSession) throws BonitaException {
        return PlatformAPIAccessor.getPlatformAPI(platformSession);
    }

    private static IdentityAPI getIdentityAPI(APISession session) throws BonitaException {
        return TenantAPIAccessor.getIdentityAPI(session);
    }

    private static ProcessAPI getProcessAPI(APISession session) throws BonitaException {
        return TenantAPIAccessor.getProcessAPI(session);
    }

    private static void deployDataSource() {
        springContext = new ClassPathXmlApplicationContext("engine.cfg.xml");
    }

    private static void undeployDataSource() {
        springContext.close();
    }

}
