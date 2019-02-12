package org.bonitasoft.example.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.api.ApiAccessType
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.util.APITypeManager
import org.junit.Before
import org.junit.Test
import java.util.*


private const val REMOTE_SERVER_URL = "http://localhost:8080"
private const val REMOTE_APPLICATION_NAME = "bonita"

class BonitaClientTest {

    // create a new client:
    private val apiClient = APIClient()
    private val bonitaClient = BonitaClient(apiClient)

    @Before
    fun cleanup() {
//        bonitaClient.loginOnDefaultTenantAdmin()
//        bonitaClient.cleanArchiveProcessInstances()
//        bonitaClient.cleanProcessInstances()
//        bonitaClient.cleanProcessDefinitions()
//        bonitaClient.logout()
    }

    @Test
    fun should_be_able_to_deploy_start_and_delete_process() {

        // setup connection to a running remote server:
        setHttpAPIType()
        // setEjbAPIType()

        // delete and create a new organization from provided file 'organization.xml':
        bonitaClient.replaceEntireOrganization()

        // login with provided user 'walter.bates':
        bonitaClient.login("walter.bates", "bpm")

        // create a new process and deploy it on the remote server:
        val (processDefinition, processInstance) = bonitaClient.createDeployAndStartProcess()

        // Simulate a human wait time:
        Thread.sleep(200)

        try {
            checkDataInstanceHasChangedValue(apiClient, processInstance)
        } finally {

            // Cleanup: disable and delete process:
            bonitaClient.deleteProcessInstances(processInstance)
            bonitaClient.disableAndDeleteProcessDefinition(processDefinition)

            bonitaClient.logout()
        }
    }

    private fun checkDataInstanceHasChangedValue(apiClient: APIClient, processInstance: ProcessInstance) {
        val data1 = apiClient.processAPI.getProcessDataInstance("data1", processInstance.id)
        assertThat(data1.value).isEqualTo(mutableListOf("listValue"))
    }

    private fun setHttpAPIType() {
        val serverURL = System.getProperty("server.url", REMOTE_SERVER_URL)
        val applicationName = System.getProperty("application.name", REMOTE_APPLICATION_NAME)
        println("Using Bonita server URL: $serverURL/$applicationName")

        val map = HashMap<String, String>()
        map["org.bonitasoft.engine.api-type.parameters"] = "server.url, application.name"
        map["server.url"] = serverURL
        map["application.name"] = applicationName

        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map)
    }

    private fun setEjbAPIType() {
        println("Accessing Bonita using EJB3")

        val parameters = HashMap<String, String>()
        parameters["java.naming.factory.url.pkgs"] = "org.jboss.ejb.client.naming"
        parameters["org.bonitasoft.engine.ejb.naming.reference"] =
            "ejb:bonita-ear/bonita-ejb/serverAPIBean!org.bonitasoft.engine.api.internal.ServerAPI"

        APITypeManager.setAPITypeAndParams(ApiAccessType.EJB3, parameters)
    }

}