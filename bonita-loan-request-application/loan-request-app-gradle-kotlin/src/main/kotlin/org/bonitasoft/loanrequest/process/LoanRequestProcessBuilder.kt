/**
 * Copyright (C) 2019 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.loanrequest.process

import org.bonitasoft.engine.bpm.contract.Type
import org.bonitasoft.engine.bpm.flownode.GatewayType
import org.bonitasoft.engine.bpm.process.DesignProcessDefinition
import org.bonitasoft.engine.bpm.process.impl.ProcessDefinitionBuilder
import org.bonitasoft.engine.expression.ExpressionBuilder

const val ACTOR_REQUESTER = "Requester"
const val ACTOR_VALIDATOR = "Validator"

const val START_EVENT = "Start Request"
const val REVIEW_REQUEST_TASK = "Review Request"
const val DECISION_GATEWAY = "isAccepted"
const val SIGN_CONTRACT_TASK = "Sign contract"
const val NOTIFY_REJECTION_TASK = "Notify rejection"
const val ACCEPTED_END_EVENT = "Accepted"
const val REJECTED_END_EVENT = "Rejected"

const val CONTRACT_AMOUNT = "amount"

/**
 * @author Emmanuel Duchastenier
 */
class LoanRequestProcessBuilder {

    fun buildExampleProcess(): DesignProcessDefinition {
        val processBuilder = ProcessDefinitionBuilder().createNewInstance("LoanRequest", "1.0")
        // Define the actors of the process:
        processBuilder.addActor(ACTOR_REQUESTER, true) // only requester can initiate a new process
        processBuilder.addActor(ACTOR_VALIDATOR) // only requester can initiate a new process
        // Define the tasks
        processBuilder.addUserTask(REVIEW_REQUEST_TASK, ACTOR_VALIDATOR)
        processBuilder.addUserTask(SIGN_CONTRACT_TASK, ACTOR_REQUESTER) // Imagine this task involve paper signing

        // For completion, this auto-task should have a connector on it,
        // to notify the rejection (through email connector, for example):
        processBuilder.addAutomaticTask(NOTIFY_REJECTION_TASK)

        // Define the events:
        processBuilder.addStartEvent(START_EVENT)
        processBuilder.addEndEvent(ACCEPTED_END_EVENT)
        processBuilder.addEndEvent(REJECTED_END_EVENT)
        // Define the Gateway:
        processBuilder.addGateway(DECISION_GATEWAY, GatewayType.EXCLUSIVE)
        // Define transitions:
        processBuilder.addTransition(START_EVENT, REVIEW_REQUEST_TASK)
        processBuilder.addTransition(REVIEW_REQUEST_TASK, DECISION_GATEWAY)
        processBuilder.addTransition(DECISION_GATEWAY, SIGN_CONTRACT_TASK,
                // let's simulate a human decision with a random accepted / rejected decision:
                ExpressionBuilder().createGroovyScriptExpression("random decision", "new java.util.Random(System.currentTimeMillis()).nextBoolean()", "java.lang.Boolean")
        )
        processBuilder.addDefaultTransition(DECISION_GATEWAY, NOTIFY_REJECTION_TASK) // Default transition, taken is expression above returns false
        processBuilder.addTransition(SIGN_CONTRACT_TASK, ACCEPTED_END_EVENT)
        processBuilder.addTransition(NOTIFY_REJECTION_TASK, REJECTED_END_EVENT)

        // Define a contract on the process initiation:
        processBuilder.addContract().addInput(CONTRACT_AMOUNT, Type.DECIMAL, "Amount of the loan requested")
        // Here we imagine a more complex contract with more inputs...

        return processBuilder.process
    }

}