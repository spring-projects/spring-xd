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

package org.springframework.xd.module.core;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.initializer.ContextIdApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.validation.BindException;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.options.DefaultModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;

/**
 * A {@link Module} implementation backed by a Spring {@link ApplicationContext}.
 * 
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 */
public class SimpleModule extends AbstractModule {

	private final Log logger = LogFactory.getLog(this.getClass());

	private ConfigurableApplicationContext context;

	private final SpringApplicationBuilder application;

	private final AtomicInteger propertiesCounter = new AtomicInteger();

	private final Properties properties = new Properties();

	private final MutablePropertySources propertySources = new MutablePropertySources();

	private ConfigurableApplicationContext parent;

	private static final String XD_CONFIG_HOME_PROPERTY_SOURCE_NAME = "xdConfigOptions";

	private static final String MODULES_CONFIG_HOME_PROPERTY_SOURCE_NAME = "moduleConfigOptions";

	public SimpleModule(ModuleDefinition definition, DeploymentMetadata metadata) {
		this(definition, metadata, null, defaultModuleOptions());
	}

	private static ModuleOptions defaultModuleOptions() {
		try {
			return new DefaultModuleOptionsMetadata().interpolate(Collections.<String, String> emptyMap());
		}
		catch (BindException e) {
			throw new IllegalStateException(e);
		}
	}

	public SimpleModule(ModuleDefinition definition, DeploymentMetadata metadata, ClassLoader classLoader,
			ModuleOptions moduleOptions) {
		super(definition, metadata);
		application = new SpringApplicationBuilder().sources(PropertyPlaceholderAutoConfiguration.class).web(false);
		if (classLoader != null) {
			application.resourceLoader(new PathMatchingResourcePatternResolver(classLoader));
		}

		propertySources.addFirst(moduleOptions.asPropertySource());
		// Also add as properties for now, b/c other parts of the system
		// (eg type conversion plugin) expects it
		this.properties.putAll(moduleOptionsToProperties(moduleOptions));

		application.profiles(moduleOptions.profilesToActivate());
		if (definition != null) {
			if (definition.getResource().isReadable()) {
				this.addComponents(definition.getResource());
			}
		}
	}

	private Map<Object, Object> moduleOptionsToProperties(ModuleOptions moduleOptions) {
		Map<Object, Object> result = new HashMap<Object, Object>();
		EnumerablePropertySource<?> ps = moduleOptions.asPropertySource();
		for (String propname : ps.getPropertyNames()) {
			Object value = ps.getProperty(propname);
			if (value != null) {
				result.put(propname, value);
			}
		}
		return result;
	}

	@Override
	public void setParentContext(ApplicationContext parent) {
		this.parent = (ConfigurableApplicationContext) parent;
	}

	@Override
	public void addComponents(Resource resource) {
		addSource(resource);
	}

	protected void addSource(Object source) {
		application.sources(source);
	}

	@Override
	public void addProperties(Properties properties) {
		this.registerPropertySource(properties);
		this.properties.putAll(properties);
	}

	@Override
	public Properties getProperties() {
		return this.properties;
	}

	public ApplicationContext getApplicationContext() {
		return this.context;
	}

	@Override
	public <T> T getComponent(Class<T> requiredType) {
		return this.context.getBean(requiredType);
	}

	@Override
	public <T> T getComponent(String componentName, Class<T> requiredType) {
		if (this.context.containsBean(componentName)) {
			return context.getBean(componentName, requiredType);
		}
		return null;
	}

	private void registerPropertySource(Properties properties) {
		int propertiesIndex = this.propertiesCounter.getAndIncrement();
		String propertySourceName = "properties-" + propertiesIndex;
		PropertySource<?> propertySource = new PropertiesPropertySource(propertySourceName, properties);
		this.propertySources.addLast(propertySource);
	}

	@Override
	public void initialize() {
		this.application.initializers(new ContextIdApplicationContextInitializer(this.toString()));
		ConfigurableEnvironment environment = new StandardEnvironment();
		if (parent != null) {
			copyEnvironment(environment, parent.getEnvironment());
			loadPropertiesResources(environment);
		}
		for (PropertySource<?> source : propertySources) {
			MutablePropertySources propertySources = environment.getPropertySources();
			// Set the moduleOptions (interpolated from commandLine options) with the highest precedence
			if (source.getName().equalsIgnoreCase(ModuleOptionsMetadata.MODULE_OPTIONS_PROPERTY_SOURCE_NAME)) {
				propertySources.addFirst(source);
			}
			// Set MODULES_CONFIG_HOME with the precedence after systemEnvironment (which is after systemProperties)
			else if (source.getName().equalsIgnoreCase(MODULES_CONFIG_HOME_PROPERTY_SOURCE_NAME)) {
				// systemEnvironment PropertySource is always present. Hence, no need to check if it exists.
				propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
						source);
			}
			// Set XD_CONFIG_HOME with the precedence after MODULES_CONFIG_HOME
			else if (source.getName().equalsIgnoreCase(XD_CONFIG_HOME_PROPERTY_SOURCE_NAME)) {
				if (propertySources.contains(MODULES_CONFIG_HOME_PROPERTY_SOURCE_NAME)) {
					propertySources.addAfter(MODULES_CONFIG_HOME_PROPERTY_SOURCE_NAME, source);
				}
				else {
					propertySources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, source);
				}
			}
			else {
				// TODO: Should the module properties be added next to moduleOptions PropertySource?
				environment.getPropertySources().addLast(source);
			}
		}
		this.application.parent(parent);
		this.application.environment(environment);
		this.context = this.application.run();
		if (logger.isInfoEnabled()) {
			logger.info("initialized module: " + this.toString());
		}
	}

	private void copyEnvironment(ConfigurableEnvironment environment, ConfigurableEnvironment parent) {
		MutablePropertySources sources = environment.getPropertySources();
		String lastName = null;
		for (PropertySource<?> source : parent.getPropertySources()) {
			String name = source.getName();
			if (sources.contains(name)) {
				sources.replace(source.getName(), source);
			}
			else {
				if (lastName == null) {
					sources.addFirst(source);
				}
				else {
					sources.addAfter(lastName, source);
				}
			}
			lastName = name;
		}
		for (String profile : parent.getActiveProfiles()) {
			environment.addActiveProfile(profile);
		}
	}

	private void loadPropertiesResources(ConfigurableEnvironment environment) {
		String[] propertiesHomes = { XD_CONFIG_HOME, MODULES_CONFIG_HOME + "/" + this.getType() };
		for (String propertiesHome : propertiesHomes) {
			String propertiesLocation = this.parent.getEnvironment().resolvePlaceholders(
					propertiesHome + "/" + this.getName() + ".properties");
			Resource propertiesResource = this.parent.getResource(propertiesLocation);
			if (propertiesResource.exists()) {
				try {
					if (propertiesHome.equalsIgnoreCase(XD_CONFIG_HOME)) {
						ResourcePropertySource rps = new ResourcePropertySource(XD_CONFIG_HOME_PROPERTY_SOURCE_NAME,
								propertiesResource);
						propertySources.addLast(rps);
					}
					else if (propertiesHome.equalsIgnoreCase(MODULES_CONFIG_HOME)) {
						ResourcePropertySource rps = new ResourcePropertySource(
								MODULES_CONFIG_HOME_PROPERTY_SOURCE_NAME, propertiesResource);
						propertySources.addLast(rps);
					}
				}
				catch (IOException e) {
					// do nothing
				}
			}
		}
	}

	@Override
	public void start() {
		context.start();
	}

	@Override
	public void stop() {
		context.stop(); // Shouldn't need to close() as well?
	}

	@Override
	public boolean isRunning() {
		return context.isRunning();
	}

	@Override
	public void destroy() {
		if (context instanceof DisposableBean) {
			try {
				((DisposableBean) context).destroy();
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

}
