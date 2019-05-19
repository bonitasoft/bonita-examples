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

    @GetMapping("/task/{taskId}/execute")
    fun executeFirstHumanTask(@PathVariable taskId: Long) {
        apiClient.login("install", "install")
        val user = apiClient.identityAPI.getUserByUserName("scott")
        apiClient.logout()
        apiClient.login("scott", "bpm")
        apiClient.processAPI.assignAndExecuteUserTask(user.id, taskId, emptyMap())
        apiClient.logout()
    }

}