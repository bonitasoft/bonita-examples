/**
 * Copyright (C) 2015 Bonitasoft S.A.
 * Bonitasoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.myproject.tests;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.bonitasoft.engine.bpm.bar.BusinessArchive;
import org.bonitasoft.engine.bpm.bar.BusinessArchiveFactory;
import org.bonitasoft.engine.bpm.process.ProcessDefinition;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.test.Engine;
import org.bonitasoft.engine.test.EngineInitializer;
import org.bonitasoft.engine.test.ProcessDeployerAPITest;
import org.bonitasoft.engine.test.UserTaskAPI;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Engine.class)
public class DummyProcessTest {

    //EngineInitializer should be a junit rule ?
    protected EngineInitializer engineInitializer = new EngineInitializer();
    private UserTaskAPI userTaskAPI;
    private ProcessDeployerAPITest processDeployer;
    private User user;

    @Before
    public void prepateTest() throws Exception {
        engineInitializer.defaultLogin();
        userTaskAPI = engineInitializer.getUserTaskAPI();
        processDeployer = engineInitializer.getProcessDeployer();
        user = engineInitializer.getIdentityAPI().createUser("william.jobs", "bpm");
    }

    @Test
    public void should_start_an_instance_of_dummy_process() throws Exception {
        //It could be great to be able to easily retrieve a BusinessArchive (eg: @BusinessArchive("/path/in/classpath"))
        final BusinessArchive businessArchive = BusinessArchiveFactory
                .readBusinessArchive(new File(DummyProcessTest.class.getResource("/DummyProcess--1.0.bar").getFile()));
        final ProcessDefinition processDef = processDeployer.deployAndEnableProcessWithActor(businessArchive, "Employee", user);
        assertThat(processDef).isNotNull();
    }

}

