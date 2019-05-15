package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ProcessInstance
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CaseController(val apiClient: APIClient) {

    @GetMapping("/cases")
    fun list(): List<ProcessInstance> {
        return apiClient.processAPI
                .searchOpenProcessInstancesInvolvingUser(apiClient.session.userId, SearchOptionsBuilder(0, 100).done())
                .result
    }

}