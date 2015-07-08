/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.fn.BiConsumer;
import reactor.rx.Stream;

/**
 * Contract for performing stream processing using Reactor within an XD processor module
 * <p/>
 * This {@link BiConsumer} will receive an input {@link Stream} left operand and an output
 * and a right operand {@link ReactiveOutput} on module initialization or on new partition
 * and eventually send messages to the module to the right with {@link ReactiveOutput#writeOutput(Publisher)}.
 * <p/>
 * The {@link Stream} will map {@link Subscriber#onNext(Object)} calls to receive operations on the message bus.
 * The {@link ReactiveOutput} will map {@link Subscriber#onNext(Object)} calls to send operations on the message bus.
 *
 * @author Stephane Maldini
 */
public interface ReactiveProcessor<I, O> extends BiConsumer<Stream<I>, ReactiveOutput<O>>, ReactiveModule {

}
