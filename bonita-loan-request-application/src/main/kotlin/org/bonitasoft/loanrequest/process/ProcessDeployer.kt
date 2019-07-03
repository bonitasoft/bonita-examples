package org.bonitasoft.loanrequest.process

import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition
import org.bonitasoft.engine.bpm.process.ProcessDefinition
import org.bonitasoft.engine.exception.BonitaException
import org.bonitasoft.engine.identity.User
import org.bonitasoft.loanrequest.apiClient

/**
 * @author Emmanuel Duchastenier
 */
class ProcessDeployer {

    @Throws(BonitaException::class)
    fun deployAndEnableProcessWithActor(designProcessDefinition: DesignProcessDefinition,
                                        requesterActor: String,
                                        requesterUser: User,
                                        validatorActor: String,
                                        validatorUser: User): ProcessDefinition {
        val processDefinition = apiClient.processAPI.deploy(
                BusinessArchiveBuilder().createNewBusinessArchive().setProcessDefinition(designProcessDefinition).done()
        )
        with(apiClient.processAPI) {
            addUserToActor(requesterActor, processDefinition, requesterUser.id)
            addUserToActor(validatorActor, processDefinition, validatorUser.id)
            enableProcess(processDefinition.id)
        }
        return processDefinition
    }
}
