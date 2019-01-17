package org.bonitasoft.example.kotlin

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder
import org.bonitasoft.engine.bpm.process.*
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder
import org.bonitasoft.engine.exception.BonitaException
import org.bonitasoft.engine.exception.DeletionException
import org.bonitasoft.engine.expression.ExpressionBuilder
import org.bonitasoft.engine.operation.Operation
import org.bonitasoft.engine.operation.OperationBuilder
import java.io.Serializable
import java.util.*


private const val TENANT_USERNAME = "install"
private const val TENANT_PASSWORD = "install"

class BonitaClient(private val apiClient: APIClient) {

    fun replaceEntireOrganization() {
        loginOnDefaultTenantAdmin()
        apiClient.identityAPI.deleteOrganization()
        apiClient.identityAPI.importOrganization(this.javaClass.getResource("/organization.xml").readText())
        logout()
    }

    fun loginOnDefaultTenantAdmin() {
        login(TENANT_USERNAME, TENANT_PASSWORD)
    }

    fun createDeployAndStartProcess(): Pair<ProcessDefinition, ProcessInstance> {
        val processAPI = apiClient.processAPI

        // Programmatically build process:
        val processWithOps = ProcessDefinitionBuilder()
            .createNewInstance("processWithOperations", "1.0")
        val actorForProcess = "actor"
        processWithOps.addActor(actorForProcess)
        processWithOps.addData(
            "data1",
            ArrayList::class.java.name,
            ExpressionBuilder().createGroovyScriptExpression(
                "create New ArrayList",
                "new java.util.ArrayList<String>()",
                ArrayList::class.java.name
            )
        )
        processWithOps.addUserTask("userTask", actorForProcess)

        // Create a business archive from this definition
        val businessArchive =
            BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(processWithOps.done()).done()
        val processDefinition = processAPI.deploy(businessArchive)
        val user = apiClient.identityAPI.getUserByUserName("walter.bates")

        // Allow walter.bates to start the process and execute the task:
        processAPI.addUserToActor(actorForProcess, processDefinition, user.id)

        processAPI.enableProcess(processDefinition.id)

        val processInstance = processAPI.startProcess(
            processDefinition.id,
            Arrays.asList<Operation>(
                OperationBuilder().createJavaMethodOperation(
                    "data1",
                    "add",
                    String::class.java.name,
                    ExpressionBuilder().createConstantStringExpression("listValue")
                )
            ),
            HashMap<String, Serializable>()
        )

        return Pair(processDefinition, processInstance)
    }

    fun disableAndDeleteProcessDefinition(processDefinition: ProcessDefinition) {
        apiClient.processAPI.disableAndDeleteProcessDefinition(processDefinition.id)
    }

    fun login(username: String, password: String) {
        apiClient.login(username, password)
    }

    fun logout() {
        apiClient.logout()
    }

    fun deleteProcessInstances(vararg processInstances: ProcessInstance) {
        val processAPI = apiClient.processAPI
        processInstances.forEach { processInstance ->
            processAPI.deleteArchivedProcessInstancesInAllStates(processInstance.id)
            processAPI.deleteProcessInstance(processInstance.id)
        }
    }

    @Throws(BonitaException::class)
    public fun cleanProcessDefinitions() {
        val processes = apiClient.processAPI.getProcessDeploymentInfos(0, 200, ProcessDeploymentInfoCriterion.DEFAULT)
        if (processes.size > 0) {
            for (processDeploymentInfo in processes) {
                if (ActivationState.ENABLED == processDeploymentInfo.getActivationState()) {
                    apiClient.processAPI.disableProcess(processDeploymentInfo.getProcessId())
                }
                apiClient.processAPI.deleteProcessDefinition(processDeploymentInfo.getProcessId())
            }
        }
    }

    @Throws(DeletionException::class)
    public fun cleanProcessInstances() {
        val processInstances = apiClient.processAPI.getProcessInstances(0, 1000, ProcessInstanceCriterion.DEFAULT)
        if (!processInstances.isEmpty()) {
            for (processInstance in processInstances) {
                apiClient.processAPI.deleteProcessInstance(processInstance.id)
            }
        }
    }

    @Throws(DeletionException::class)
    public fun cleanArchiveProcessInstances() {
        val archivedProcessInstances =
            apiClient.processAPI.getArchivedProcessInstances(0, 1000, ProcessInstanceCriterion.DEFAULT)
        if (!archivedProcessInstances.isEmpty()) {
            for (archivedProcessInstance in archivedProcessInstances) {
                apiClient.processAPI.deleteArchivedProcessInstancesInAllStates(archivedProcessInstance.sourceObjectId)
            }
        }
    }
}