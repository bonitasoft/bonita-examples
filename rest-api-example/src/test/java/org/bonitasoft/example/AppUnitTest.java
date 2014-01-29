package org.bonitasoft.example;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.mockito.Mockito;

public class AppUnitTest {

	/*
	 * POST http://localhost:8080/bonita/API/identity/user/
	 * {"userName":"john.doe"
	 * ,"password":"bpm","password_confirm":"bpm","firstname"
	 * :"John","lastname":"Doe"}
	 */
	@Test
	public void testCreateUserShouldPOSTOnUserURI()
			throws ClientProtocolException, IOException {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		String username = "walter.bates";
		String password = "bpm";

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		app.createUser(username, password);

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new PostOnURIMatcher("/API/identity/user")),
				Mockito.any(HttpContext.class));
	}

	/*
	 * DELETE /api/identity/user
	 */
	@Test
	public void testDeleteUserDELETEOnUserURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		String userid = "42";

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		app.deleteUser(userid);

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new DeleteOnURIMatcher("/API/identity/user")),
				Mockito.any(HttpContext.class));
	}

	/*
	 * POST http://localhost:8080/bonita/API/bpm/case/ {"processDefinitionId":
	 * <processId>}
	 */
	@Test 
	public void testStartACasePOSTOnCaseURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		long processDefinitionId = 42l;

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		app.startACase(processDefinitionId);

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new PostOnURIMatcher("/API/bpm/case")),
				Mockito.any(HttpContext.class));
	}

	/*
	 * GET
	 * http://localhost:8080/bonita/API/bpm/humanTask?p=0&c=10&f=state%3dready
	 * &f=user_id%3d104
	 */
	@Test
	public void testListPendingTasksGETOnHumanTaskURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		long userid = 12l;
		app.listPendingTask(userid);

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new GetOnURIMatcher("/API/bpm/humanTask")),
				Mockito.any(HttpContext.class));
	}

	/*
	 * PUT http://localhost:8080/bonita/API/bpm/activity/[activityId] {"state":
	 * "completed"}
	 */
	@Test
	public void testExecuteATaskPUTOnActivityURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		long activityId = 12l;
		app.executeActivity(activityId);

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new PutOnURIMatcher("/API/bpm/activity")),
				Mockito.any(HttpContext.class));
	}

	@Test
	public void testImportOrgaExecutePostOnIdentityURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 201, ""));
		response.setEntity(new StringEntity("path_to_uploaded_file"));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);
		String organizationFilePath= getClass().getClassLoader().getResource("ACME.xml").getPath();
		// When
		app.importOrganizationFromFile(new File(organizationFilePath));

		// Assert
		Mockito.verify(mockClient).execute(
				Mockito.argThat(new PostOnURIMatcher("/services/organization/import")),
				Mockito.any(HttpContext.class));
	}

	// ------- ERROR MANAGEMENT ---------- //

	@Test(expected = RuntimeException.class)
	public void testExceptionOnHTTPResponseCodeOtherThan201()
			throws ClientProtocolException, IOException {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		String username = "walter.bates";
		String password = "bpm";

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(
				new ProtocolVersion("http", 1, 1), 404,
				"Test resource not found."));
		Mockito.when(
				mockClient.execute(Mockito.any(HttpUriRequest.class),
						Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		app.createUser(username, password);

		// Assert
		// Runtime Exception should be thrown
	}

}
