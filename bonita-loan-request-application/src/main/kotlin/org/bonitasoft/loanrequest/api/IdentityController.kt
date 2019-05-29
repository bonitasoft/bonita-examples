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
        val users = apiClient.identityAPI
                .searchUsers(SearchOptionsBuilder(0, 50).done())
                .result
        apiClient.logout()
        return users
    }

    @GetMapping("/loadOrganization")
    fun import() {
        apiClient.login("install", "install")
        val orgaAsString = this.javaClass.classLoader.getResourceAsStream("organization.xml").toString()
        println("loading Organization content:\n$orgaAsString")
        apiClient.identityAPI.importOrganization(orgaAsString)
        apiClient.logout()
    }

}