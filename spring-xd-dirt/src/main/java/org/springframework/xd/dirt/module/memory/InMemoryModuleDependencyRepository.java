/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.module.memory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.module.ModuleType;


/**
 * In memory implementation of {@link ModuleDependencyRepository}.
 * 
 * @author Eric Bottard
 */
public class InMemoryModuleDependencyRepository implements ModuleDependencyRepository {

	private Map<String, Set<String>> dependencies = new HashMap<String, Set<String>>() {

		@Override
		public Set<String> get(Object key) {
			Set<String> result = super.get(key);
			if (result == null) {
				result = new HashSet<String>();
				put((String) key, result);
			}
			return result;
		}
	};

	@Override
	public void store(String moduleName, ModuleType type, String target) {
		dependencies.get(keyFor(moduleName, type)).add(target);
	}

	@Override
	public void delete(String module, ModuleType type, String target) {
		dependencies.get(keyFor(module, type)).remove(target);
	}

	@Override
	public Set<String> findDependents(String name, ModuleType type) {
		return dependencies.get(keyFor(name, type));
	}

	private String keyFor(String moduleName, ModuleType type) {
		return type.name() + ":" + moduleName;
	}

}
