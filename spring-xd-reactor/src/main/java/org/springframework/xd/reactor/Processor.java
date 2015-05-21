/*
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
package org.springframework.xd.reactor;

import org.reactivestreams.Publisher;
import reactor.rx.Stream;

/**
 * Contract for performing stream processing using Reactor within an XD processor module
 *
 * @author Mark Pollack
 * @author Stephane Maldini
 */
public interface Processor<I, O> {

    /**
     * Process a stream of messages and return an output stream.  The input
     * and output stream will be mapped onto receive/send operations on the message bus.
     *
     * @param inputStream Input stream the receives messages from the message bus
     * @return Output Publisher of messages sent to the message bus
     */
    Publisher<O> process(Stream<I> inputStream);

}
