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

import java.util.Map;

import lombok.Builder;
import lombok.Value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Greg Turnquist
 */
@lombok.Data
@Value
@Builder(builderMethodName = "jsonApiData")
public class JsonApiData<T> {

	private String type;
	private String id;
	private T attributes;
	private Map<String, String> links;
	private Map<String, Map<String, Map<String, String>>> relationships;

	@JsonCreator
	public JsonApiData(@JsonProperty("type") String type, @JsonProperty("id") String id,
					   @JsonProperty("attributes") T attributes, @JsonProperty("links") Map<String, String> links,
					   @JsonProperty("relationships") Map<String, Map<String, Map<String, String>>> relationships) {

		this.type = type;
		this.id = id;
		this.attributes = attributes;
		this.links = links;
		this.relationships = relationships;
	}


}
