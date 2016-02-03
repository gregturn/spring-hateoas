/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.hateoas.jsonapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.hateoas.Link;

/**
 * Split up a list of links into IANA-approved rels vs. domain specific relationships.
 */
public class JsonApiOrganizedLinks {

	public static Set<String> IANA_RELS = new HashSet<String>(Arrays.asList(Link.REL_SELF, Link.REL_FIRST,
			Link.REL_PREVIOUS, Link.REL_NEXT, Link.REL_LAST));

	private final List<Link> ianaLinks;
	private final List<Link> relationshipLinks;

	public JsonApiOrganizedLinks(List<Link> links) {
		this.ianaLinks = new ArrayList<Link>();
		this.relationshipLinks = new ArrayList<Link>();

		for (Link link : links) {
			if (IANA_RELS.contains(link.getRel())) {
				ianaLinks.add(link);
			} else {
				relationshipLinks.add(link);
			}
		}
	}

	public List<Link> getIanaLinks() {
		return ianaLinks;
	}

	public List<Link> getRelationshipLinks() {
		return relationshipLinks;
	}
}
