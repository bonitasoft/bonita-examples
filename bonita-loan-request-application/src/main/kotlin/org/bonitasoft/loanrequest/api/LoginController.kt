package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class LoginController(val apiClient: APIClient) {

    @PostMapping("/login")
    fun login(@RequestBody credentials: Credentials) {
        apiClient.logout()
        apiClient.login(credentials.username, credentials.password)
    }

    @GetMapping("/session")
    fun session(): Map<String, Any?> {
        return mapOf(
                "username" to apiClient.session?.userName,
                "loginDate" to apiClient.session?.creationDate
        )
    }
}

data class Credentials(val username: String, val password: String)

