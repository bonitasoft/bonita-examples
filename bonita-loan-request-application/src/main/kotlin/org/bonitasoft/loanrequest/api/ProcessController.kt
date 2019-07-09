/**
 * Copyright (C) 2019 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.loanrequest.api

import org.bonitasoft.engine.api.APIClient
import org.bonitasoft.engine.bpm.process.ProcessDeploymentInfo
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.loanrequest.process.CONTRACT_AMOUNT
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

// to work around the javascript / json limitation of Long values:
val ProcessDeploymentInfo.processIdAsString
    get() = processId.toString()

@RestController
class ProcessController(val apiClient: APIClient) {

    // Expose the deployed processes through Rest Apis:
    @GetMapping("/processes")
    fun list(): List<ProcessDeploymentInfo> {
        apiClient.login("install", "install")
        val result = apiClient.processAPI.searchProcessDeploymentInfos(SearchOptionsBuilder(0, 100).done()).result
        result.forEach { println(it.processIdAsString) } // so that we see the full Process ID in the console
        apiClient.logout()
        return result
    }

    @GetMapping("/process/{id}/undeploy")
    fun uninstall(@PathVariable id: Long) {
        apiClient.login("install", "install")
        apiClient.processAPI.disableAndDeleteProcessDefinition(id)
        apiClient.logout()
    }

    @GetMapping("/process/{id}/start")
    fun startProcess(@PathVariable id: Long) {
        apiClient.login("install", "install")
        apiClient.processAPI.startProcessWithInputs(id, mapOf(Pair(CONTRACT_AMOUNT, 12000.0)))
        apiClient.logout()
    }

}