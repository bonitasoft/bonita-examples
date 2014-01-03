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

    /**
     * Spring context used to deploy a data source
     */
    private static ConfigurableApplicationContext springContext;

    public static void main(String[] args) throws Exception {
        // check that Bonita Home is set
        checkBonitaHome();

        // deploy a data source. This step is not necessary if you already have a data source available, for instance,
        // a data source supplied by an application server
        deployDataSource();

        // perform all necessary actions to create the Bonita platform
        createPlatform();

        // create a an user that will deploy and execute processes
        User user = createUser();

        // deploy a process
        ProcessDefinition processDefinition = deployProcess();

        // execute actions chosen from a menu
        executeActions(processDefinition);

        // --- clean all information ----
        // undeploy the process
        undeployProcess(processDefinition);

        // delete the created user
        deleteUser(user);

        // perform all necessary actions to destroy the platform
        destroyPlatform();

        // undeploy the data source
        undeployDataSource();
        System.out.println("Completed sucessfully!!!");
    }

    /**
     * Check if the system property bonita.home is set. If not a RuntimeException is thrown.
     */
    private static void checkBonitaHome() {
        String bonitaHome = System.getProperty(BONITA_HOME_KEY);
        if (bonitaHome == null) {
            throw new RuntimeException("The system property bonita.home is not set: please, set this property with the path to the bonita home folder.\n "
                    + "You can get a bonita home from BonitaBPMCommunity-<bonita.engine.version>-deploy.zip or use the one generate under \n"
                    + "the folder target/home on this project by runing the command 'mvn clean install'.");
        }
    }

    /**
     * Do all necessary actions to create the Bonita platform
     * 
     * @throws BonitaException
     *             if an exception occurs when creating the platform
     */
    private static void createPlatform() throws BonitaException {
        // login as platform administrator
        PlatformSession session = doPlatformLogin(PLATFORM_ADMIN, PLATFORM_PASSWORD);
        try {
            System.out.println("Creating and initializing the platform ...");
            // create and initialize the platform
            getPlatformAPI(session).createAndInitializePlatform();
            System.out.println("Platform created and initialized!");

            System.out.println("Starting node ...");
            // start the node (make scheduler service to start)
            getPlatformAPI(session).startNode();
            System.out.println("Node started!");
        } finally {
            // logout
            doPlatformLogout(session);
        }
    }

    /**
     * Perform all necessary actions to destroy the platform
     * 
     * @throws BonitaException
     *             if an exception occurs when destroying the platform
     */
    private static void destroyPlatform() throws BonitaException {
        // login as platform administrator
        PlatformSession session = doPlatformLogin(PLATFORM_ADMIN, PLATFORM_PASSWORD);
        try {
            System.out.println("Stopping node ...");
            // stop the node (this will stop the scheduler service)
            getPlatformAPI(session).stopNode();
            System.out.println("Node stopped!");

            System.out.println("Cleaning and deleting the platform ...");
            // clean and delete the platform
            getPlatformAPI(session).cleanAndDeletePlaftorm();;
            System.out.println("Platform cleaned and deleted!");
        } finally {
            // logout
            doPlatformLogout(session);
        }
    }

    /**
     * Deploy a process example
     * 
     * @return the deployed process
     * @throws BonitaException
     *             if an exception occurs during process deployment
     */
    private static ProcessDefinition deployProcess() throws BonitaException {
        // log in with the end user previously created
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Deploying process ... ");
            // build the process example
            DesignProcessDefinition designProcessDefinition = buildProcessDefinition();
            // deploy the process
            ProcessDefinition processDefinition = getProcessAPI(session).deploy(designProcessDefinition);
            System.out.println("Process deployed!");

            System.out.println("Mapping actors ... ");
            // map the process actor to the end user. Before enabling the process all actors must be mapped.
            getProcessAPI(session).addUserToActor(ACTOR_NAME, processDefinition, session.getUserId());
            System.out.println("Actors mapped!");

            System.out.println("Enabling process ... ");
            // enable the process. Up to now this process can be instantiated.
            getProcessAPI(session).enableProcess(processDefinition.getId());
            System.out.println("Process enabled!");
            return processDefinition;

        } finally {
            // do logout
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

        // create a new process definition with name and version
        ProcessDefinitionBuilder pdb = new ProcessDefinitionBuilder().createNewInstance("My first process", "1.0");
        // add actor defined as initiator
        pdb.addActor(ACTOR_NAME, true);
        // add a start event
        pdb.addStartEvent(startName);
        // add an automatic task
        pdb.addAutomaticTask(autoStepName);
        // add an user task having the previously defined actor
        pdb.addUserTask(firstUserStepName, ACTOR_NAME);
        // add another user task having the previously defined actor
        pdb.addUserTask(secondUserStepName, ACTOR_NAME);
        // add add an end event
        pdb.addEndEvent(endName);
        // defined transitions
        pdb.addTransition(startName, autoStepName);
        pdb.addTransition(autoStepName, firstUserStepName);
        pdb.addTransition(firstUserStepName, secondUserStepName);
        pdb.addTransition(secondUserStepName, endName);

        return pdb.done();
    }

    /**
     * Display a menu and prompt for a action to perform. The chosen action is performed and a new action is prompted
     * until the user decides to quit the application
     * 
     * @param processDefinition
     *            the ProcesssDefinition
     * @throws IOException
     *             if an exception occurs when prompting for an action
     * @throws BonitaException
     *             if an exception occurs when executing an action
     */
    private static void executeActions(ProcessDefinition processDefinition) throws IOException, BonitaException {
        String message = getMenutTextContent();
        String choice = null;
        do {
            // show the menu and read the action chosen by the user
            choice = readLine(message);
            if ("1".equals(choice)) {
                // if user choose 1 then start a new process instance
                startProcess(processDefinition);
            } else if ("2".equals(choice)) {
                // if user choose 2 then list opened process instances
                listOpenedProcessInstances();
            } else if ("3".equals(choice)) {
                // if user choose 3 then list archived process instances
                listArchivedProcessInstances();
            } else if ("4".equals(choice)) {
                // if user choose 4 then list pending tasks
                listPendingTasks();
            } else if ("5".equals(choice)) {
                // if user choose 5 execute the task chosen by the user
                executeATask();
            } else if (!"0".equals(choice)) {
                System.out.println("Invalid choice!");
            }
        } while (!"0".equals(choice));
    }

    /**
     * Get the content of menu to be displayed
     * 
     * @return the content of menu to be displayed
     */
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

    /**
     * Start a process
     * 
     * @param processDefinition
     *            the process definition to start
     * @throws BonitaException
     *             if an exception occurs when starting the process
     */
    private static void startProcess(ProcessDefinition processDefinition) throws BonitaException {
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Instantiating process ... ");
            // create a new process instance
            ProcessInstance processInstance = getProcessAPI(session).startProcess(processDefinition.getId());
            System.out.println("Process instantiated! Id: " + processInstance.getId());
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * List all opened process instances
     * 
     * @throws BonitaException
     *             if an Exception occurs when listing the process instances
     */
    private static void listOpenedProcessInstances() throws BonitaException {
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Active process instances: ");
            // the result will be retrieved by pages of PAGE_SIZE size
            int startIndex = 0;
            int page = 1;
            SearchResult<ProcessInstance> result = null;
            do {
                // get the current page of opened process instances
                result = getOpenProcessInstancePage(session, startIndex);
                // print the current page
                printOpenedProcessIntancesPage(page, result);

                // go to next page
                startIndex += PAGE_SIZE;
                page++;
            } while (result.getResult().size() == PAGE_SIZE);
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * Get the page of opened process instances based on the start index
     * 
     * @param session
     *            the current session
     * @param startIndex
     *            the index of the first element of the page
     * @return the page of opened process instances based on the start index
     * @throws BonitaException
     *             if an exception occurs when getting the ProcessAPI
     */
    private static SearchResult<ProcessInstance> getOpenProcessInstancePage(APISession session, int startIndex) throws BonitaException {
        // create a new SeachOptions with given start index and PAGE_SIZE as max number of elements
        SearchOptionsBuilder optionsBuilder = new SearchOptionsBuilder(startIndex, PAGE_SIZE);
        // sort the result by the process instance id in the ascending order
        optionsBuilder.sort(ProcessInstanceSearchDescriptor.ID, Order.ASC);
        // perform the request and return the result
        return getProcessAPI(session).searchProcessInstances(optionsBuilder.done());
    }

    /**
     * Print a page of opened process instances
     * 
     * @param page
     *            the page number
     * @param result
     *            the page content
     */
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

    /**
     * List all archived process instances
     * 
     * @throws BonitaException
     *             if an Exception occurs when listing process instances
     */
    private static void listArchivedProcessInstances() throws BonitaException {
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Archived process instances: ");
            // the result will be retrieved by pages of PAGE_SIZE size
            int startIndex = 0;
            int page = 1;
            SearchResult<ArchivedProcessInstance> result = null;
            do {
                // get the current page of opened process instances
                result = getArchivedProcessInstancePage(session, startIndex);
                // print the current page
                printArchivedProcessInstancePage(page, result);

                // go to the next page
                startIndex += PAGE_SIZE;
                page++;
            } while (result.getResult().size() == PAGE_SIZE);
        } finally {
            // logout
            doTenantLogout(session);
        }

    }

    /**
     * Get the page of archived process instances based on the start index
     * 
     * @param session
     *            the current session
     * @param startIndex
     *            the index of the first element of the page
     * @return the page of archived process instances based on the start index
     * @throws BonitaException
     */
    private static SearchResult<ArchivedProcessInstance> getArchivedProcessInstancePage(APISession session, int startIndex) throws BonitaException {
        // create a new SeachOptions with given start index and PAGE_SIZE as max number of elements
        SearchOptionsBuilder optionsBuilder = new SearchOptionsBuilder(startIndex, PAGE_SIZE);
        // when process instances are archived the original process instance id is supplied by SOURCE_OBJECT_ID,
        // so the result will be sort by the SOURCE_OBJECT_ID
        optionsBuilder.sort(ArchivedProcessInstancesSearchDescriptor.SOURCE_OBJECT_ID, Order.ASC);
        // perform the request and return the result;
        return getProcessAPI(session).searchArchivedProcessInstances(optionsBuilder.done());
    }

    /**
     * Print a page of archived process instances
     * 
     * @param page
     *            the page number
     * @param result
     *            the page content
     */
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
            // remember: when you deal with archived elements the original id is supplied by the sourceObjectId
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

    /**
     * List all pending tasks for the logged user
     * 
     * @throws BonitaException
     *             if an exception occurs when listing the pending tasks
     */
    private static void listPendingTasks() throws BonitaException {
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            ProcessAPI processAPI = getProcessAPI(session);
            // the result will be retrieved by pages of PAGE_SIZE size
            int startIndex = 0;
            int page = 1;
            List<HumanTaskInstance> pendingTasks = null;
            do {
                // get the current page
                pendingTasks = processAPI.getPendingHumanTaskInstances(session.getUserId(), startIndex, PAGE_SIZE, ActivityInstanceCriterion.LAST_UPDATE_ASC);
                // print the current page
                printTasksPage(page, pendingTasks);

                // got to next page
                startIndex += PAGE_SIZE;
                page++;
            } while (pendingTasks.size() == PAGE_SIZE);
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * Print a tasks page
     * 
     * @param page
     *            the page number
     * @param pendingTasks
     *            the page content
     */
    private static void printTasksPage(int page, List<HumanTaskInstance> pendingTasks) {
        if (pendingTasks.isEmpty()) {
            if (page == 1) {
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

    /**
     * Execute the task chosen by the user
     * 
     * @throws BonitaException
     *             if an exception occurs when executing the task
     * @throws IOException
     *             if an exception occurs when reading the task id to be executed
     */
    private static void executeATask() throws BonitaException, IOException {
        // get the task id to be executed
        Long taskId = readTaskId();
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            ProcessRuntimeAPI processAPI = getProcessAPI(session);
            // retrieve the task to be executed in order to print information like, task name and process instance id
            // if you don't need these information you can assign and execute it directly without retrieving it
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
            // catch ActivityInstanceNotFoundException to cover the case where the user enter an invalid taks id
            System.out.println("No task found with id " + taskId);
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * Prompt for the task id to be executed
     * 
     * @return the task id to be executed
     * @throws IOException
     */
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

    /**
     * Undeploy the process definition
     * 
     * @param processDefinition
     *            the process definition
     * @throws BonitaException
     *             if an exception occurs when undeploying the process
     */
    private static void undeployProcess(ProcessDefinition processDefinition) throws BonitaException {
        // login
        APISession session = doTenantLogin(USER_NAME, PWD);
        try {
            System.out.println("Undeploying process...");
            // before deleting a process is necessary to delete all its instances (opened or archived)
            deleteOpenedProcessInstance(processDefinition, session);
            deleteArchivedProcessInstance(processDefinition, session);

            // disable and delete the process definition
            getProcessAPI(session).disableAndDeleteProcessDefinition(processDefinition.getId());
            System.out.println("Process disabled and undeployed!");
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * Delete all opened process instances for the given process definition
     * 
     * @param processDefinition
     *            the process definition
     * @param session
     *            the current session
     * @throws BonitaException
     *             if an exception occurs when deleting process instances
     */
    public static void deleteOpenedProcessInstance(ProcessDefinition processDefinition, APISession session) throws BonitaException {
        // delete opened process instances by block of PAGE_SIZE
        long nbOfDeletedProcess = 0;
        do {
            nbOfDeletedProcess = getProcessAPI(session).deleteProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        System.out.println("Deleted opened processs instances.");
    }

    /**
     * Delete all archived process instances for the given process definition
     * 
     * @param processDefinition
     *            the process definition
     * @param session
     *            the current session
     * @throws BonitaException
     *             if an exception occurs when deleting process instances
     */
    public static void deleteArchivedProcessInstance(ProcessDefinition processDefinition, APISession session) throws BonitaException {
        // delete archived process instances by block of PAGE_SIZE
        long nbOfDeletedProcess = 0;
        do {
            nbOfDeletedProcess = getProcessAPI(session).deleteArchivedProcessInstances(processDefinition.getId(), 0, PAGE_SIZE);
        } while (nbOfDeletedProcess != 0);
        System.out.println("Deleted archived processs instances.");
    }

    /**
     * Delete the given user
     * 
     * @param user
     *            the user to delete
     * @throws BonitaException
     *             if an exception occurs when deleting the user
     */
    private static void deleteUser(User user) throws BonitaException {
        // In order to delete the only end user, it's necessary to log in with the technical user
        APISession session = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_USER_NAME);
        try {
            // delete user
            getIdentityAPI(session).deleteUser(user.getId());;
            System.out.println("Deleted user '" + user.getUserName() + "'.");
        } finally {
            // logout
            doTenantLogout(session);
        }
    }

    /**
     * Create a end user
     * 
     * @return the created user
     * @throws BonitaException
     *             if an exception occurs during user creation
     */
    private static User createUser() throws BonitaException {
        // no end users are created during the platform initialization, so only the technical user is available.
        // logged in as technical user you are able to create the end user that will be able to deploy process, execute tasks, ...
        APISession session = doTenantLogin(TECHNICAL_USER_NAME, TECHNICAL_PASSWORD);
        User user = null;
        try {
            // create end user
            user = getIdentityAPI(session).createUser(USER_NAME, PWD);
            System.out.println("Created user '" + USER_NAME + "'.");
        } finally {
            // technical user do logout
            doTenantLogout(session);
        }
        return user;
    }

    /**
     * Read a line from standard input
     * 
     * @param message
     *            the message to be displayed
     * @return the line read from standard input
     * @throws IOException
     *             if an exception occurs when reading a line
     */
    private static String readLine(String message) throws IOException {
        System.out.println(message);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
        return buffer.readLine();
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

    /**
     * Deploy a data source
     */
    private static void deployDataSource() {
        springContext = new ClassPathXmlApplicationContext("engine.cfg.xml");
    }

    /**
     * Undeploy the data source
     */
    private static void undeployDataSource() {
        springContext.close();
    }

}
