/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.rest;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.stream.EnhancedStreamParser;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.memory.InMemoryJobDefinitionRepository;

/**
 * Override some dependencies to use actual in-memory implementations.
 * 
 * @author Glenn Renfro
 */
@Configuration
public class JobsControllerIntegrationTestsConfig extends Dependencies {

	@Bean
	public JobDefinitionRepository jobDefinitionRepository() {
		return new InMemoryJobDefinitionRepository();
	}

	@Bean
	public JobDeployer jobDeployer() {
		XDParser parser = new EnhancedStreamParser(
				jobDefinitionRepository(), moduleRegistry());

		return new JobDeployer(jobDefinitionRepository(), triggerDefinitionRepository(),
				deploymentMessageSender(), parser);
	}

	@Bean
	public ModuleRegistry moduleRegistry() {
		ModuleRegistry registry = mock(ModuleRegistry.class);

		return registry;
	}
}
