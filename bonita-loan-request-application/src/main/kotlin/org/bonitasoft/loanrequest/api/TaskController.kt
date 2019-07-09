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
package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class TaskController(val apiClient: APIClient) {

    @GetMapping("/tasks")
    fun list(): List<HumanTaskInstance>? {
        apiClient.login("install", "install")
        val result = apiClient.processAPI.searchMyAvailableHumanTasks(
                apiClient.session.userId,
                SearchOptionsBuilder(0, 100).done())
                .result
        apiClient.logout()
        return result
    }

    @GetMapping("/task/{taskId}/executeAsValidator")
    fun executeFirstHumanTask(@PathVariable taskId: Long) {
        apiClient.login("install", "install")
        val user = apiClient.identityAPI.getUserByUserName("validator")
        apiClient.logout()
        apiClient.login("validator", "bpm")
        apiClient.processAPI.assignAndExecuteUserTask(user.id, taskId, emptyMap())
        apiClient.logout()
    }

    @GetMapping("/task/{taskId}/executeAsRequester")
    fun executeSignContractTask(@PathVariable taskId: Long) {
        apiClient.login("install", "install")
        val user = apiClient.identityAPI.getUserByUserName("requester")
        apiClient.logout()
        apiClient.login("requester", "bpm")
        apiClient.processAPI.assignAndExecuteUserTask(user.id, taskId, emptyMap())
        apiClient.logout()
    }

}