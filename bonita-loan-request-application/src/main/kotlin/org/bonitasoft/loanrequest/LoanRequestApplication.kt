package org.bonitasoft.loanrequest

import org.awaitility.Awaitility
import org.awaitility.Duration.ONE_SECOND
import org.awaitility.Duration.TEN_SECONDS
import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder
import org.bonitasoft.engine.exception.BonitaException
import org.bonitasoft.engine.identity.User
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.concurrent.Callable

@SpringBootApplication
class LoanRequestApplication

val apiClient = APIClient()

fun main(args: Array<String>) {

    // Let's bootstrap SpringBoot application:
    runApplication<LoanRequestApplication>(*args)

    // Log in on Engine API:
    loginAsTenantAdministrator()
    try {
        // Create a business user to interact with the process:
        val newUser = createNewUser("scott", "bpm")
        apiClient.logout()
        // Create and execute the user tasks of the process with the created user:
        apiClient.login("scott", "bpm")
        createAndExecuteProcess(newUser)
//        apiClient.logout()
//        loginAsTenantAdministrator()
//        removeUser(newUser)
    } finally {
        apiClient.logout()
    }
}

private fun loginAsTenantAdministrator() {
    apiClient.login("install", "install")
}

private fun createNewUser(userName: String, password: String): User {
    return apiClient.identityAPI.createUser(userName, password, "Scotty", "Dumont")
}

private fun removeUser(newUser: User) {
    apiClient.identityAPI.deleteUser(newUser.id)
}

@Throws(BonitaException::class)
fun createAndExecuteProcess(user: User) {
    val processBuilder = ProcessDefinitionBuilder().createNewInstance("LoanRequest", "1.0")
    val actor = "LoanRequester"
    processBuilder.addActor(actor)
    val userTaskName = "fillLoanRequestForm"
    val designProcessDefinition = processBuilder.addUserTask(userTaskName, actor).process
    val processDefinition = deployAndEnableProcessWithActor(designProcessDefinition, actor, user)
    val processInstance = apiClient.processAPI.startProcess(processDefinition.id)
    val processInstanceId = processInstance.id

    // Wait for the user task named "fillLoanRequestForm" to be ready to execute:
    val userTask = waitForUserTask(user, processInstanceId, userTaskName)

    // Take the task and execute it:
//    apiClient.processAPI.assignAndExecuteUserTask(user.id, userTask.id, emptyMap())

    // Wait for the whole process instance to finish executing:
//    waitForProcessToFinish()
//    Thread.sleep(5000)

//    println("Instance of Process LoanRequest(1.0) with id $processInstanceId has finished executing.")

    // Deactivate and remove the process previously created:
//    apiClient.processAPI.disableAndDeleteProcessDefinition(processDefinition.id)

//    println("Process LoanRequest(1.0) uninstalled.")
}

fun waitForUserTask(user: User, processInstanceId: Long, userTaskName: String): HumanTaskInstance {
    Awaitility.await("User task lasts long to be ready")
            .atMost(TEN_SECONDS)
            .pollInterval(ONE_SECOND)
            .until(Callable<Boolean> {
                apiClient.processAPI.getNumberOfPendingHumanTaskInstances(user.id) == 1L
            })
    return apiClient.processAPI.getHumanTaskInstances(processInstanceId, userTaskName, 0, 1)[0]
}

fun waitForProcessToFinish() {
    Awaitility.await("Process instance lasts long to complete")
            .atMost(TEN_SECONDS)
            .pollInterval(ONE_SECOND)
            .until(Callable<Boolean> {
                apiClient.processAPI.numberOfArchivedProcessInstances == 1L
            })
}

@Throws(BonitaException::class)
fun deployAndEnableProcessWithActor(designProcessDefinition: DesignProcessDefinition,
                                    actorName: String,
                                    user: User): ProcessDefinition {
    val processDefinition = apiClient.processAPI.deploy(
            BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done()
    )
    with(apiClient.processAPI) {
        addUserToActor(actorName, processDefinition, user.id)
        enableProcess(processDefinition.id)
    }
    return processDefinition
}

