/*
 * Copyright 2013 the xoriginal author or authors.
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

package org.springframework.xd.dirt.stream.dsl;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andy Clement
 */
public class ChannelNode extends AstNode {

	private ChannelType channelType;

	private List<String> nameComponents;

	private List<String> indexingElements;

	public ChannelNode(ChannelType channelType, int startpos, int endpos, List<String> nameElements,
			List<String> indexingElements) {
		super(startpos, endpos);
		this.channelType = channelType;
		this.nameComponents = nameElements;
		this.indexingElements = indexingElements;
	}

	@Override
	public String stringify(boolean includePositionalInfo) {
		StringBuilder s = new StringBuilder();
		s.append("(");
		s.append(channelType.getStringRepresentation());
		int t = 0;
		if (nameComponents.size() > 0 && channelType.isTap() &&
				nameComponents.get(0).equalsIgnoreCase(channelType.tapSource().name())) {
			t = 1;
		}
		for (int max = nameComponents.size(); t < max; t++) {
			s.append(nameComponents.get(t));
			if (t < nameComponents.size() - 1) {
				s.append(":");
			}
		}
		if (indexingElements.size() != 0) {
			for (int t2 = 0, max = indexingElements.size(); t2 < max; t2++) {
				s.append(".");
				s.append(indexingElements.get(t2));
			}
		}
		if (includePositionalInfo) {
			s.append(":");
			s.append(getStartPos()).append(">").append(getEndPos());
		}
		s.append(")");
		return s.toString();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(channelType.getStringRepresentation());
		s.append(getNameComponents());
		s.append(getIndexingComponents());
		return s.toString();
	}

	public String getChannelName() {
		StringBuilder s = new StringBuilder();
		if (channelType.isTap()) {
			s.append("tap:");
		}
		s.append(getNameComponents());
		s.append(getIndexingComponents());
		return s.toString();
	}

	public String getNameComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = nameComponents.size(); t < max; t++) {
			if (t > 0) {
				s.append(":");
			}
			s.append(nameComponents.get(t));
		}
		return s.toString();
	}

	public String getIndexingComponents() {
		StringBuilder s = new StringBuilder();
		for (int t = 0, max = indexingElements.size(); t < max; t++) {
			s.append(".");
			s.append(indexingElements.get(t));
		}
		return s.toString();
	}

	public String getStreamName() {
		if (channelType == ChannelType.TAP_STREAM) {
			return getNameComponents();
		}
		return null;
	}

	public ChannelType getChannelType() {
		return this.channelType;
	}

	public ChannelNode copyOf() {
		// TODO not a deep copy, is that ok?
		return new ChannelNode(this.channelType, startpos, endpos, nameComponents, indexingElements);
	}

	public void resolve(StreamLookupEnvironment env) {
		if (channelType == ChannelType.TAP_STREAM) {
			if (indexingElements.isEmpty()) {
				StreamNode sn = env.lookupStream(getStreamName());
				if (sn == null) {
					throw new StreamDefinitionException("", -1, XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE,
							getStreamName());
				}
				// Point to the first element of the stream
				indexingElements = new ArrayList<String>();
				indexingElements.add(sn.getModuleNodes().get(0).getName());
			}
			else {
				// Easter Egg: can use index of module in a stream when tapping.
				// Key benefit:
				// You can tap into something that perhaps wasn't labelled
				// in some stream but needed to be because it was a dup:
				// Note: need to be aware of how indexes will move when
				// stream composition has occurred!
				try {
					int index = Integer.parseInt(indexingElements.get(0));
					StreamNode sn = env.lookupStream(getStreamName());
					if (sn == null) {
						throw new StreamDefinitionException("", -1, XDDSLMessages.UNRECOGNIZED_STREAM_REFERENCE,
								getStreamName());
					}
					indexingElements.remove(0);
					indexingElements.add(0, sn.getModuleNodes().get(index).getName());
				}
				catch (NumberFormatException nfe) {
					// this is ok, probably wasn't a number
				}
			}
		}
	}
}
