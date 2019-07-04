package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.identity.User
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class IdentityController(val apiClient: APIClient) {

    @GetMapping("/users")
    fun list(): List<User> {
        apiClient.login("install", "install")
        try {
            return apiClient.identityAPI.searchUsers(SearchOptionsBuilder(0, 50).done()).result
        } finally {
            apiClient.logout()
        }
    }

}