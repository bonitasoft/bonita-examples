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

    // Expose the finished process instances (=cases completed)
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