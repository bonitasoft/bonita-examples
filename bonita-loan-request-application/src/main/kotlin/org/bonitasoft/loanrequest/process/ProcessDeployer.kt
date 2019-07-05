package org.bonitasoft.loanrequest.process

import org.bonitasoft.engine.bpm.bar.BusinessArchiveBuilder
import org.bonitasoft.engine.bpm.bar.actorMapping.Actor
import org.bonitasoft.engine.bpm.bar.actorMapping.ActorMapping
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
        // Create the Actor Mapping with our Users:
        val requester = Actor(requesterActor)
        requester.addUser(requesterUser.userName)
        val validator = Actor(validatorActor)
        validator.addUser(validatorUser.userName)
        val actorMapping = ActorMapping()
        actorMapping.addActor(requester)
        actorMapping.addActor(validator)

        // Create the Business Archive to deploy:
        val businessArchive = BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition)
                // set the actor mapping so that the process is resolved and can then be enabled:
                .setActorMapping(actorMapping)
                .done()

        with(apiClient.processAPI) {
            val processDefinition = deploy(businessArchive)
            enableProcess(processDefinition.id)
            return processDefinition
        }
    }
}
