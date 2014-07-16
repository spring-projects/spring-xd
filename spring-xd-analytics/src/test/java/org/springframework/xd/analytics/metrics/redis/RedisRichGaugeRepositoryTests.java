/**
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.metrics.redis;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.AbstractRichGaugeRepositoryTests;
import org.springframework.xd.analytics.metrics.common.RedisRepositoriesConfig;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Luke Taylor
 * @author Gary Russell
 */
@ContextConfiguration(classes = RedisRepositoriesConfig.class, loader = AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisRichGaugeRepositoryTests extends AbstractRichGaugeRepositoryTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Autowired
	private RedisRichGaugeRepository repo;

	@After
	@Before
	public void beforeAndAfter() {
		repo.deleteAll();
	}

	@Override
	protected RichGaugeRepository createService() {
		return repo;
	}
}
