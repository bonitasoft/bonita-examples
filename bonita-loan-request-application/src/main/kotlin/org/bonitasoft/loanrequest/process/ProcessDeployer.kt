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
        val actor1 = Actor(requesterActor)
        actor1.addUser(requesterUser.userName)
        val actor2 = Actor(validatorActor)
        actor2.addUser(validatorUser.userName)
        val actorMapping = ActorMapping()
        actorMapping.addActor(actor1)
        actorMapping.addActor(actor2)
        val businessArchive = BusinessArchiveBuilder().createNewBusinessArchive()
                .setProcessDefinition(designProcessDefinition)
                .setActorMapping(actorMapping)
                .done()

        with(apiClient.processAPI) {
            val processDefinition = deploy(businessArchive)
            enableProcess(processDefinition.id)
            return processDefinition
        }
    }
}
