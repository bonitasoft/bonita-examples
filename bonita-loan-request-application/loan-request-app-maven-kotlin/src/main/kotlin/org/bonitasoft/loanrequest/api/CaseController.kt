package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CaseController(val apiClient: APIClient) {

    // Expose the open process instances (=cases not completed)
    @GetMapping("/cases")
    fun list(): List<ProcessInstance> {
        apiClient.login("install", "install")
        try {
            return apiClient.processAPI
                    .searchOpenProcessInstances(SearchOptionsBuilder(0, 100).done())
                    .result
        } finally {
            apiClient.logout()
        }
    }

    // Expose the open process instances (=cases not completed)
    @GetMapping("/completedcases")
    fun listCompleted(): List<ArchivedProcessInstance> {
        apiClient.login("install", "install")
        try {
            return apiClient.processAPI
                    .searchArchivedProcessInstances(SearchOptionsBuilder(0, 100).done())
                    .result
        } finally {
            apiClient.logout()
        }
    }

}