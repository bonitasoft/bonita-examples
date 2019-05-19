package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CaseController(val apiClient: APIClient) {

    // Expose the open process instances (=cases) for openness
    @GetMapping("/cases")
    fun list(): List<ProcessInstance> {
        apiClient.login("install", "install")
        val result = apiClient.processAPI
                .searchOpenProcessInstances(SearchOptionsBuilder(0, 100).done())
                .result
        apiClient.logout()
        return result
    }

}