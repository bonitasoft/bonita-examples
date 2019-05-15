package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ProcessController(val apiClient: APIClient) {

    @GetMapping("/processes")
    fun list(): List<ProcessDeploymentInfo> {
        apiClient.login("install", "install")
        val result = apiClient.processAPI.searchProcessDeploymentInfosCanBeStartedBy(apiClient.session.userId, SearchOptionsBuilder(0, 100).done()).result
        apiClient.logout()
        return result
    }

}