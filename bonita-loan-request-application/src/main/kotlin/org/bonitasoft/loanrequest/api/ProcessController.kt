package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class ProcessController(val apiClient: APIClient) {

    // Expose the deployed processes through Rest Apis:
    @GetMapping("/processes")
    fun list(): List<ProcessDeploymentInfo> {
        apiClient.login("install", "install")
        val result = apiClient.processAPI.searchProcessDeploymentInfos(SearchOptionsBuilder(0, 100).done()).result
        apiClient.logout()
        return result
    }

    @GetMapping("/process/{id}/undeploy")
    fun uninstall(@PathVariable id: Long) {
        apiClient.login("install", "install")
        apiClient.processAPI.disableAndDeleteProcessDefinition(id)
        apiClient.logout()
    }

}