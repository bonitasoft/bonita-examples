package org.bonitasoft.loanrequest

import org.awaitility.Awaitility
import org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS
import org.awaitility.Duration.TEN_SECONDS
import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.exception.BonitaException
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.loanrequest.api.processIdAsString
import org.bonitasoft.loanrequest.process.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.util.concurrent.Callable

const val TENANT_ADMIN_NAME = "install"
const val TENANT_ADMIN_PASSWORD = "install"

@SpringBootApplication
class LoanRequestApplication

val apiClient = APIClient()

fun main(args: Array<String>) {

    // Let's bootstrap SpringBoot application:
    runApplication<LoanRequestApplication>(*args)

    // Log in on Engine API:
    loginAsTenantAdministrator()
    try {
        // Create business users to interact with the process:
        val requester = createNewUser("requester", "bpm", "Requester", "LoanRequester")
        val validator = createNewUser("validator", "bpm", "Validator", "LoanValidator")
        // Use this newly created users to create and execute the process flow:
        loginWithAnotherUser(requester)
        val processDefinition = createAndDeployProcess(requester, validator)
        executeProcess(requester, validator, processDefinition)
        // apiClient.logout()
        // loginAsTenantAdministrator()
        // removeUser(newUser)
    } finally {
        apiClient.logout()
    }
}

private fun loginAsTenantAdministrator() {
    apiClient.logout()
    apiClient.login(TENANT_ADMIN_NAME, TENANT_ADMIN_PASSWORD)
}

private fun loginWithAnotherUser(newUser: User) {
    apiClient.logout()
    apiClient.login(newUser.userName, "bpm")
}

private fun createNewUser(userName: String, password: String, firstName: String, lastName: String): User {
    return apiClient.identityAPI.createUser(userName, password, firstName, lastName)
}

@Throws(BonitaException::class)
fun executeProcess(initiator: User, validator: User, processDefinition: ProcessDefinition) {
    // Start a new Loan request with an amount of 12000.0 (€ Euro):
    val processInstance = apiClient.processAPI.startProcessWithInputs(processDefinition.id, mapOf(Pair(CONTRACT_AMOUNT, 12000.0)))

    // Now the validator needs to review it:
    loginWithAnotherUser(validator)
    // Wait for the user task named "fillLoanRequestForm" to be ready to execute:
    val reviewRequestTask = waitForUserTask(validator, processInstance.id, REVIEW_REQUEST_TASK)

    // Take the task and execute it:
    apiClient.processAPI.assignAndExecuteUserTask(validator.id, reviewRequestTask.id, emptyMap())

    val signContractTask = waitForUserTask(initiator, processInstance.id, SIGN_CONTRACT_TASK)
    apiClient.processAPI.assignAndExecuteUserTask(initiator.id, signContractTask.id, emptyMap())

    // Wait for the whole process instance to finish executing:
    waitForProcessToFinish()
    // Thread.sleep(5000)

    println("Instance of Process LoanRequest(1.0) with id ${processInstance.id} has finished executing.")

    // Deactivate and remove the process previously created:
    // apiClient.processAPI.disableAndDeleteProcessDefinition(processDefinition.id)

    // println("Process LoanRequest(1.0) uninstalled.")
    apiClient.processAPI.searchProcessDeploymentInfos(SearchOptionsBuilder(0, 100).done()).result.forEach { println(it.processIdAsString) }
}

private fun createAndDeployProcess(initiator: User, validator: User): ProcessDefinition {
    // Create the process:
    val designProcessDefinition = LoanRequestProcessBuilder().buildExampleProcess()
    // Deploy the process and enable it:
    return ProcessDeployer().deployAndEnableProcessWithActor(
            designProcessDefinition, ACTOR_REQUESTER, initiator, ACTOR_VALIDATOR, validator)
}

fun waitForUserTask(user: User, processInstanceId: Long, userTaskName: String): HumanTaskInstance {
    Awaitility.await("User task should not last so long to be ready :-(")
            .atMost(TEN_SECONDS)
            .pollInterval(FIVE_HUNDRED_MILLISECONDS)
            .until(Callable<Boolean> {
                apiClient.processAPI.getNumberOfPendingHumanTaskInstances(user.id) == 1L
            })
    return apiClient.processAPI.getHumanTaskInstances(processInstanceId, userTaskName, 0, 1)[0]
}

private fun removeUser(newUser: User) {
    apiClient.identityAPI.deleteUser(newUser.id)
}

fun waitForProcessToFinish() {
    Awaitility.await("Process instance lasts long to complete")
            .atMost(TEN_SECONDS)
            .pollInterval(FIVE_HUNDRED_MILLISECONDS)
            .until(Callable<Boolean> {
                apiClient.processAPI.numberOfArchivedProcessInstances == 1L
            })
}
