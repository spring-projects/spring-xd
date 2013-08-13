/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.DeploymentMessageSender;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests REST compliance of jobs-related endpoints.
 * 
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class JobsControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private DeploymentMessageSender sender;

	@Autowired
	private ModuleRegistry moduleRegistry;

	@Before
	public void before() {
		Resource resource = mock(Resource.class);
		ModuleDefinition jobDefinition = new ModuleDefinition(ModuleType.JOB.getTypeName(),
				ModuleType.JOB.getTypeName(), resource);

		when(resource.exists()).thenReturn(true);
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		definitions.add(jobDefinition);
		when(moduleRegistry.findDefinitions(ModuleType.JOB.getTypeName())).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("job1")).thenReturn(definitions);
		when(moduleRegistry.findDefinitions("job2")).thenReturn(definitions);
		when(moduleRegistry.lookup("job1", "job")).thenReturn(jobDefinition);
		when(moduleRegistry.lookup("job2", "job")).thenReturn(jobDefinition);
		when(moduleRegistry.lookup("job", "job")).thenReturn(jobDefinition);

	}

	@Test
	public void testSuccessfulJobCreation() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
	}

	@Test
	public void testSuccessfulJobCreateAndDeploy() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job5").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		verify(sender, times(1)).sendDeploymentRequests(eq("job5"), anyListOf(ModuleDeploymentRequest.class));
	}

	@Test
	public void testSuccessfulJobDeletion() throws Exception {
		mockMvc.perform(delete("/jobs/{name}", "job1"));
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(delete("/jobs/{name}", "job1")).andExpect(status().isOk());
	}

	@Test
	public void testListAllJobs() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs").param("name", "job2").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());

		mockMvc.perform(get("/jobs").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(2))).andExpect(jsonPath("$.content[0].name").value("job1")).andExpect(
				jsonPath("$.content[1].name").value("job2"));
	}

	@Test
	public void testJobCreationNoDefinition() throws Exception {
		mockMvc.perform(post("/jobs").param("name", "myjob").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());
	}

	@Test
	public void testJobUnDeployNoDef() throws Exception {
		mockMvc.perform(put("/jobs/{name}", "myjob").param("deploy", "false").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testJobDeployNoDef() throws Exception {
		mockMvc.perform(put("/jobs/{name}", "myjob").param("deploy", "true").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isNotFound());
	}

	@Test
	public void testCreateOnAlreadyCreatedJob() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isCreated());
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job --cron='*/10 * * * * *'").accept(
						MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
	}

	@Test
	public void testFailedJobDeletion() throws Exception {
		mockMvc.perform(delete("/jobs/{name}", "job1")).andExpect(status().isNotFound());
	}

	@Test
	public void testInvalidDefinitionCreate() throws Exception {
		mockMvc.perform(
				post("/jobs").param("name", "job1").param("definition", "job adsfa").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isBadRequest());

		mockMvc.perform(get("/jobs").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(
				jsonPath("$.content", Matchers.hasSize(0)));
	}
}
