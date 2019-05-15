package org.bonitasoft.loanrequest

import org.bonitasoft.engine.api.APIClient
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LoanRequestApplication

fun main(args: Array<String>) {

    runApplication<LoanRequestApplication>(*args)

    val apiClient = APIClient()
    apiClient.login("install", "install")
    try {
        apiClient.identityAPI.createUser("manu", "bpm")

        ProcessDesign

    } finally {
        apiClient.logout()
    }

}
