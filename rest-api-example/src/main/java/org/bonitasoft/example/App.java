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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.process.InvalidProcessDefinitionException;
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder;

/**
 * @author Nicolas Chabanoles
 * 
 */
public class App {

	private static final String BONITA_URI = "http://localhost:8080/bonita";
	
	/**
	 * Tenant technical user username
	 */
	private static final String TECHNICAL_USER_NAME = "install";

	/**
	 * Tenant technical user password
	 */
	private static final String TECHNICAL_PASSWORD = "install";

	/**
	 * Human user username
	 */
	private static final String USERNAME = "walter.bates";

	/**
	 * Human user password
	 */
	private static final String PASSWORD = "bpm";

	/**
	 * Actor name used in the process example
	 */
	private static final String ACTOR_NAME = "MyActor";

	/**
	 * Resource file containing an example of organization
	 */
	private static final String ORGANIZATION_FILE = "/ACME.xml";

	/**
	 * Resource file containing an actor mapping between an actor of the process
	 * and the organization
	 */
	private static final String ACTOR_MAPPING_FILE = "/actorMapping.xml";

	/**
	 * Name of the process created and deployed
	 */
	private static final String PROCESS_NAME = "My first process";

	private final HttpClient httpClient;
	private HttpContext httpContext;
	
	private final String bonitaURI;

	

	public static void main(String[] args) {

		PoolingClientConnectionManager conMan = getConnectionManager();

		App app = new App(new DefaultHttpClient(conMan), BONITA_URI);
		app.start();
	}

	private static PoolingClientConnectionManager getConnectionManager() {
		PoolingClientConnectionManager conMan = new PoolingClientConnectionManager(SchemeRegistryFactory.createDefault());
		conMan.setMaxTotal(200);
		conMan.setDefaultMaxPerRoute(200);
		return conMan;
	}

	public App(HttpClient client, String bonitaURI) {
		httpClient = client;
		this.bonitaURI = bonitaURI;
	}

	public void start() {

		// Ensure minimal configuration such as Organization
		loginAsTechnicalUser();
		makeSureLocaleIsActive(); // this is necessary to make API working after Tomcat restart.
		importOrganization();
		logout();

		long processId = 0;
		try {
			// Behave as a user
			loginAs(USERNAME, PASSWORD);
			String userId = getUserIdFromSession();
			processId = deployProcess();

			// execute actions chosen from a menu
			executeActions(userId, processId);
		} finally {
			// --- clean all information ----
			if (processId > 0) {
				// undeploy the process
				undeployProcess(processId);
			}

			logout();
			httpClient.getConnectionManager().shutdown();
		}
	}

	private void loginAsTechnicalUser() {
		loginAs(TECHNICAL_USER_NAME, TECHNICAL_PASSWORD);
	}

	public void loginAs(String username, String password) {

		try {

			CookieStore cookieStore = new BasicCookieStore();
			httpContext = new BasicHttpContext();
			httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

			String loginURL = "/loginservice";

			// If you misspell a parameter you will get a HTTP 500 error
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("username", username));
			urlParameters.add(new BasicNameValuePair("password", password));
			urlParameters.add(new BasicNameValuePair("redirect", "false"));

			// UTF-8 is mandatory otherwise you get a NPE
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(urlParameters, "utf-8");
			executePostRequest(loginURL, entity);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private int executePostRequest(String apiURI, UrlEncodedFormEntity entity) {
		try {
			HttpPost postRequest = new HttpPost(bonitaURI + apiURI);

			postRequest.setEntity(entity);

			HttpResponse response = httpClient.execute(postRequest, httpContext);

			return consumeResponse(response, true);

		} catch (HttpHostConnectException e) {
			throw new RuntimeException("Bonita bundle may not have been started, or the URL is invalid. Please verify hostname and port number. URL used is: " + BONITA_URI,e);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private int consumeResponse(HttpResponse response, boolean printResponse) {

		String responseAsString = consumeResponseIfNecessary(response);
		if(printResponse) {
			System.out.println(responseAsString);
		}

		return ensureStatusOk(response);
	}

	private String consumeResponseIfNecessary(HttpResponse response) {
		if (response.getEntity() != null) {
			BufferedReader rd;
			try {
				rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				return result.toString();
			} catch (Exception e) {
				throw new RuntimeException("Failed to consume response.", e);
			}
		} else {
			return "";
		}
	}
	
	private void makeSureLocaleIsActive() {
		consumeResponse(executeGetRequest("/API/system/i18ntranslation?f=locale%3den"), false);
	}
	
	private int ensureStatusOk(HttpResponse response) {
		if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode() + " : "
					+ response.getStatusLine().getReasonPhrase());
		}
		return response.getStatusLine().getStatusCode();
	}

	private void importOrganization() {
		importOrganizationFromFile(new File(getClass().getResource(ORGANIZATION_FILE).getPath()));

	}

	public int importOrganizationFromFile(File organizationFile) {

		try {
			System.out.println("Deploying organization ... ");
			HttpPost post = new HttpPost(bonitaURI + "/portal/organizationUpload");

			MultipartEntity entity = new MultipartEntity();
			entity.addPart("file", new FileBody(organizationFile));
			post.setEntity(entity);

			HttpResponse response = httpClient.execute(post, httpContext);
			String uploadedFilePath = extractUploadedFilePathFromResponse(response);

			String payloadAsString = "{\"organizationDataUpload\":\"" + uploadedFilePath + "\"}";
			int result = consumeResponse(executePostRequest("/services/organization/import", payloadAsString),false);

			System.out.println("Organization deployed!");
			return result;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private String extractUploadedFilePathFromResponse(HttpResponse response) {
		try {
			return EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private HttpResponse executePostRequest(String apiURI, String payloadAsString) {
		try {
			HttpPost postRequest = new HttpPost(bonitaURI + apiURI);

			StringEntity input = new StringEntity(payloadAsString);
			input.setContentType("application/json");

			postRequest.setEntity(input);

			HttpResponse response = httpClient.execute(postRequest, httpContext);

			return response;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public void logout() {
		consumeResponse(executeGetRequest("/logoutservice"),false);
	}

	private HttpResponse executeGetRequest(String apiURI) {
		try {
			HttpGet getRequest = new HttpGet(bonitaURI + apiURI);

			HttpResponse response = httpClient.execute(getRequest, httpContext);

			return response;

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private String getUserIdFromSession() {
		try {
			HttpGet getRequest = new HttpGet(bonitaURI + "/API/system/session/unusedid");

			HttpResponse response = httpClient.execute(getRequest, httpContext);

			return extractUserIdFrom(response);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private String extractUserIdFrom(HttpResponse response) {
		try {
			String session = EntityUtils.toString(response.getEntity());
			String remain = session.substring(session.indexOf("user_id\":") + 10);
			String userid = remain.substring(0, remain.indexOf("\""));
			return userid;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private long deployProcess() {
		try {
			System.out.println("Deploying process '" + PROCESS_NAME + "'...");
			String uploadedFilePath = uploadGeneratedBar();
			long processId = installProcessFromUploadedBar(uploadedFilePath);
			System.out.println("Process deployed with id: " + processId);

			System.out.println("Enabling process '" + PROCESS_NAME + "' (ID:" + processId + ")...");
			enableProcess(processId);
			System.out.println("Process Enabled!");
			return processId;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private String uploadGeneratedBar() throws IOException, ClientProtocolException {
		// build the process example
		File barFile = buildProcessDefinition();

		HttpPost post = new HttpPost(bonitaURI + "/portal/processUpload");

		MultipartEntity entity = new MultipartEntity();
		entity.addPart("file", new FileBody(barFile));
		post.setEntity(entity);

		HttpResponse response = httpClient.execute(post, httpContext);
		barFile.delete();
		return extractUploadedFilePathFromResponse(response);

	}

	/**
	 * Build a simple process: Start -> My Automatic Step -> My first step -> My
	 * second step -> End Defines an actor mapping that will fit the
	 * organization deployed
	 * 
	 * @return the built process
	 * @throws InvalidProcessDefinitionException
	 */
	private File buildProcessDefinition() {
		try {
			String startName = "Start";
			String firstUserStepName = "My first step";
			String secondUserStepName = "My second step";
			String autoStepName = "My Automatic Step";
			String endName = "End";

			// create a new process definition with name and version
			ProcessDefinitionBuilder pdb = new ProcessDefinitionBuilder().createNewInstance(PROCESS_NAME, "1.0");
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

			BusinessArchive bar = new BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(pdb.done())
					.setActorMapping(getActorMapping()).done(); // embed actor
																// mapping
																// directly in
																// definition
			File barOnFileSystem = new File(System.getProperty("java.io.tmpdir"), "process.bar");
			barOnFileSystem.deleteOnExit();
			BusinessArchiveFactory.writeBusinessArchiveToFile(bar, barOnFileSystem);

			return barOnFileSystem;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] getActorMapping() {

		FileInputStream fileInputStream = null;

		File file = new File(getClass().getResource(ACTOR_MAPPING_FILE).getPath());

		byte[] bFile = new byte[(int) file.length()];

		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return bFile;

	}
	
	private long installProcessFromUploadedBar(String uploadedFilePath) {
		String payloadAsString = "{\"fileupload\":\"" + uploadedFilePath + "\"}";

		return extractProcessId(executePostRequest("/API/bpm/process", payloadAsString));

	}

	private long extractProcessId(HttpResponse response) {
		ensureStatusOk(response);
		try {
			String processInJSON = EntityUtils.toString(response.getEntity());

			String remain = processInJSON.substring(processInJSON.indexOf("id\":") + 5);
			String id = remain.substring(0, remain.indexOf("\""));

			return Long.parseLong(id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
	private void enableProcess(long processId) {
		String payloadAsString = "{\"activationState\":\"ENABLED\"}";
		consumeResponse(executePutRequest("/API/bpm/process/" + processId, payloadAsString),true);
	}
	
	private HttpResponse executePutRequest(String apiURI, String payloadAsString) {
		try {
			HttpPut putRequest = new HttpPut(bonitaURI + apiURI);
			putRequest.addHeader("Content-Type", "application/json");

			StringEntity input = new StringEntity(payloadAsString);
			input.setContentType("application/json");
			putRequest.setEntity(input);

			return httpClient.execute(putRequest, httpContext);

		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Display a menu and prompt for a action to perform. The chosen action is
	 * performed and a new action is prompted until the user decides to quit the
	 * application.
	 */
	private void executeActions(String userId, long processID) {
		String message = getMenutTextContent();
		String choice = null;
		do {
			// show the menu and read the action chosen by the user
			choice = readLine(message);
			if ("1".equals(choice)) {
				// if user chooses 1 then start a new process instance
				startACase(processID);
			} else if ("2".equals(choice)) {
				// if user chooses 2 then list opened process instances
				listOpenedProcessInstances();
			} else if ("3".equals(choice)) {
				// if user chooses 3 then list archived process instances
				listArchivedProcessInstances();
			} else if ("4".equals(choice)) {
				// if user chooses 4 then list pending tasks
				listPendingTasks(userId);
			} else if ("5".equals(choice)) {
				// if user chooses 5 execute the task chosen by the user
				executeATask(userId);
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
	 * Read a line from standard input
	 * 
	 * @param message
	 *            the message to be displayed
	 * @return the line read from standard input
	 * @throws IOException
	 *             if an exception occurs when reading a line
	 */
	private static String readLine(String message) {
		System.out.println(message);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(System.in));
		try {
			return buffer.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
	
	public int startACase(long processDefinitionId) {
		System.out.println("Starting a new case of process " + PROCESS_NAME + " (ID: " + processDefinitionId + ").");
		String apiURI = "/API/bpm/case/";

		String payloadAsString = "{\"processDefinitionId\": " + processDefinitionId + "}";

		return consumeResponse(executePostRequest(apiURI, payloadAsString),true);

	}
	
	private void listOpenedProcessInstances() {
		consumeResponse(executeGetRequest("/API/bpm/case?p=0&c=100"),true);

	}
	
	private void listArchivedProcessInstances() {
		consumeResponse(executeGetRequest("/API/bpm/archivedCase?p=0&c=100"),true);

	}
	
	/**
	 * List 100 first pending tasks for the logged user
	 */
	public void listPendingTasks(String userId) {

		consumeResponse(executeGetRequest("/API/bpm/humanTask?p=0&c=100&f=state%3dready&f=user_id%3d" + userId),true);

	}
	
	private void executeATask(String userId) {
		// get the task id to be executed
		Long taskId = readTaskId();
		// a task cannot be executed if it is not assigned to the user
		assignActivity(taskId, userId);
		// execute the task
		executeActivity(taskId);

	}
	/**
	 * Prompt for the task id to be executed
	 * 
	 * @return the task id to be executed
	 * @throws IOException
	 */
	private long readTaskId() {
		String message = "Enter the id of task to be executed:";
		String strId = readLine(message);
		long taskId = -1;
		try {
			taskId = Long.parseLong(strId);
		} catch (Exception e) {
			System.out.println(strId + " is not a valid id. You can find all task ids using the menu 'list pending tasks'.");
		}
		return taskId;
	}

	private void assignActivity(Long taskId, String userId) {
		String payloadAsString = "{\"assigned_id\":\"" + userId + "\"}";
		consumeResponse(executePutRequest("/API/bpm/humanTask/" + taskId, payloadAsString),true);

	}
	
	public void executeActivity(long activityId) {
		String apiURI = "/API/bpm/activity/" + activityId;
		String payloadAsString = "{\"state\":\"completed\"}";

		consumeResponse(executePutRequest(apiURI, payloadAsString),true);
	}
	
	private void undeployProcess(long processId) {

		disableProcess(processId);
		deleteProcess(processId);
	}
	private void disableProcess(long processId) {
		System.out.println("Disabling process '" + PROCESS_NAME + "' (ID:" + processId + ")...");
		
		String payloadAsString = "{\"activationState\":\"DISABLED\"}";
		consumeResponse(executePutRequest("/API/bpm/process/" + processId, payloadAsString),true);
		
		System.out.println("Process Disabled!");
	}
	
	private void deleteProcess(long processId) {
		System.out.println("Deleting process '" + PROCESS_NAME + "' (ID:" + processId + ")...");

		consumeResponse(executeDeleteRequest("/API/bpm/process/" + processId),true);

		System.out.println("Process deleted!");
	}
	
	private HttpResponse executeDeleteRequest(String deleteURI) {
		try {

			HttpDelete deleteRequest = new HttpDelete(bonitaURI + deleteURI);
			HttpResponse response = httpClient.execute(deleteRequest, httpContext);

			return response;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

}
