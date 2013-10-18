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

package org.springframework.xd.module;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.xd.module.options.ModuleOption;
import org.springframework.xd.module.options.ModuleOptions;

/**
 * Defines a module.
 * 
 * @author Gary Russell
 * 
 */
public class ModuleDefinition {

	private final String name;

	private final ModuleType type;

	private final Resource resource;

	private volatile Properties properties;

	private final URL[] classpath;

	private ModuleOptions moduleOptions;

	public ModuleDefinition(String name, ModuleType moduleType) {
		this(name, moduleType, new DescriptiveResource("Dummy resource"));
	}

	public ModuleDefinition(String name, ModuleType type, Resource resource) {
		this(name, type, resource, null);
	}

	public ModuleDefinition(String name, ModuleType type, Resource resource, URL[] classpath) {
		Assert.hasLength(name, "name cannot be blank");
		Assert.notNull(type, "type cannot be null");
		Assert.notNull(resource, "resource cannot be null");
		this.resource = resource;
		this.name = name;
		this.type = type;
		this.classpath = classpath != null && classpath.length > 0 ? classpath : null;
	}

	public String getName() {
		return name;
	}

	public ModuleType getType() {
		return type;
	}

	public Resource getResource() {
		return resource;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public URL[] getClasspath() {
		return classpath;
	}

	public synchronized ModuleOptions getModuleOptions() {
		if ("file".equals(name)) {
			ModuleOptions result = new ModuleOptions();
			result.add(ModuleOption.named("suffix").withDefaultExpression("foo + 'bar'"));
			result.add(ModuleOption.named("foo"));
			return result;
		}

		if (moduleOptions == null) {
			try {
				Resource optionsContext = resource.createRelative(name + "-options.xml");
				if (!optionsContext.exists()) {
					moduleOptions = ModuleOptions.ABSENT;
				}
				else {
					GenericApplicationContext context = new GenericApplicationContext();
					XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
					reader.loadBeanDefinitions(optionsContext);
					moduleOptions = context.getBean(ModuleOptions.class);
				}
			}
			catch (IOException e) {
				e.printStackTrace();
				moduleOptions = ModuleOptions.ABSENT;
			}
		}
		return moduleOptions;
	}
}
