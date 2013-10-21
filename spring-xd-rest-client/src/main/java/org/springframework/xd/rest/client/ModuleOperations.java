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

package org.springframework.xd.rest.client;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.client.domain.DetailedModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.ModuleDefinitionResource;
import org.springframework.xd.rest.client.domain.RESTModuleType;

/**
 * Interface defining operations available against Module.
 * 
 * @author Glenn Renfro
 */
public interface ModuleOperations {

	/**
	 * Compose a new virtual Module.
	 */
	public ModuleDefinitionResource composeModule(String name, String definition);


	/**
	 * List modules known to the system.
	 */
	public PagedResources<ModuleDefinitionResource> list(RESTModuleType type);


	/**
	 * Retrieve information about a particular module.
	 */
	public DetailedModuleDefinitionResource info(String name, RESTModuleType type);
}
