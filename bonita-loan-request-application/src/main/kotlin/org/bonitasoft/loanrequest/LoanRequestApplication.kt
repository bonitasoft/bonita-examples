package org.bonitasoft.loanrequest

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.api.TenantAPIAccessor.getProcessAPI
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoanRequestApplication

fun main(args: Array<String>) {

    runApplication<LoanRequestApplication>(*args)

    val apiClient = APIClient()
    apiClient.login("install", "install")
    try {
        createNewUser(apiClient)
        createAndExecuteProcess(apiClient)
    } finally {
        apiClient.logout()
    }
}

private fun createNewUser(apiClient: APIClient) {
    apiClient.identityAPI.createUser("manu", "bpm")
}

fun createAndExecuteProcess(apiClient: APIClient) {
    val processBuilder = ProcessDefinitionBuilder().createNewInstance("LoanRequest", "1.0")
    val ACTOR_NAME = "LoanRequester"
    processBuilder.addActor(ACTOR_NAME)
    val designProcessDefinition = processBuilder.addUserTask("step1", ACTOR_NAME).process
    val processDefinition = deployAndEnableProcessWithActor(designProcessDefinition, ACTOR_NAME, user)
    var processInstance = apiClient.processAPI.startProcess(processDefinition.getId())
    waitForUserTask(processInstance, "step1")

    val processInstanceId = processInstance.getId()
    processInstance = apiClient.processAPI.getProcessInstance(processInstanceId)
    assertEquals("started", processInstance.getState())

    apiClient.processAPI.setProcessInstanceState(processInstance, "initializing")
    processInstance = apiClient.processAPI.getProcessInstance(processInstanceId)
    assertEquals("initializing", processInstance.getState())

    apiClient.processAPI.setProcessInstanceState(processInstance, "started")
    processInstance = apiClient.processAPI.getProcessInstance(processInstanceId)
    assertEquals("started", processInstance.getState())

    disableAndDeleteProcess(processDefinition)
}
