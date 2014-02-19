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

package org.springframework.xd.dirt.server.options;

import javax.validation.constraints.NotNull;

import org.kohsuke.args4j.Option;


/**
 * Holds options that are common to both admin and container servers, when used in distributed mode. Note that single
 * node has its own options class, because valid values are different.
 * 
 * @author Eric Bottard
 */
public class CommonDistributedOptions extends CommonOptions {

	public static enum Analytics {
		// note: memory is NOT an option here
		redis;
	}

	public static enum ControlTransport {
		rabbit, redis
	}

	// Should be pushed down to AdminOptions but currently
	// can't b/c of the way container runtime info is persisted
	public static enum Store {
		memory, redis;
	}

	@Option(name = "--analytics", usage = "How to persist analytics such as counters and gauges")
	private Analytics analytics;

	@Option(name = "--store", usage = "How to persist admin data")
	private Store store;

	@NotNull
	public Analytics getXD_ANALYTICS() {
		return analytics;
	}

	@NotNull
	public Store getXD_STORE() {
		return store;
	}

	public void setXD_ANALYTICS(Analytics analytics) {
		this.analytics = analytics;
	}

	public void setXD_STORE(Store store) {
		this.store = store;
	}

}
