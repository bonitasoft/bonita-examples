package org.bonitasoft.example;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
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
	 * POST http://localhost:8080/bonita/API/bpm/case/ {"processDefinitionId":
	 * <processId>}
	 */
	@Test
	public void testStartACasePOSTOnCaseURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		long processDefinitionId = 42l;

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		app.startACase(processDefinitionId);

		// Assert
		Mockito.verify(mockClient)
				.execute(Mockito.argThat(new PostOnURIMatcher("/API/bpm/case")), Mockito.any(HttpContext.class));
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

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		String userid = "12";
		app.listPendingTasks(userid);

		// Assert
		Mockito.verify(mockClient).execute(Mockito.argThat(new GetOnURIMatcher("/API/bpm/humanTask")),
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

		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, ""));
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpContext.class))).thenReturn(response);

		// When

		long activityId = 12l;
		app.executeActivity(activityId);

		// Assert
		Mockito.verify(mockClient).execute(Mockito.argThat(new PutOnURIMatcher("/API/bpm/activity")),
				Mockito.any(HttpContext.class));
	}

	@Test
	public void testImportOrgaExecutePostOnIdentityURI() throws Exception {
		// Given
		HttpClient mockClient = Mockito.mock(HttpClient.class);

		App app = new App(mockClient, "http://domain.com/app");
		HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), 201, ""));
		response.setEntity(new StringEntity("path_to_uploaded_file"));
		Mockito.when(mockClient.execute(Mockito.any(HttpUriRequest.class), Mockito.any(HttpContext.class))).thenReturn(response);
		String organizationFilePath = getClass().getClassLoader().getResource("ACME.xml").getPath();
		// When
		app.importOrganizationFromFile(new File(organizationFilePath));

		// Assert
		Mockito.verify(mockClient).execute(Mockito.argThat(new PostOnURIMatcher("/services/organization/import")),
				Mockito.any(HttpContext.class));
	}

}
