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

import java.util.List;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import org.springframework.hateoas.Link;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representation of a JSON Collection.
 *
 * @author Greg Turnquist
 */
@Data
@Value
@Builder(builderMethodName = "jsonApiCollection")
public class JsonApiCollection<T> {

	private List<JsonApiData<?>> data;
	private List<Link> links;
	private List<Link> relationships;
	private Object meta;

	@JsonCreator
	public JsonApiCollection(@JsonProperty("data") List<JsonApiData<?>> data, @JsonProperty("links") List<Link> links,
							 @JsonProperty("relationships") List<Link> relationships, @JsonProperty("meta") Object meta) {

		this.data = data;
		this.links = links;
		this.relationships = relationships;
		this.meta = meta;
	}

}
