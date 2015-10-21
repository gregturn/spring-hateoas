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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.hateoas.jsonapi.JsonApiSingle.*;
import static org.springframework.hateoas.jsonapi.JsonApiData.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.support.MappingUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Greg Turnquist
 */
public class JacksonSerializationTest {

	ObjectMapper mapper;

	@Before
	public void setUp() {

		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
	}

	@Test
	public void createSimpleCollection() throws IOException {

		JsonApiData data = jsonApiData()
				.type(String.class.getSimpleName())
				.attributes("Greetings")
				.links(Arrays.asList(new Link("localhost").withSelfRel()))
				.relationships(Arrays.asList(new Link("localhost/manager").withRel("manager")))
				.build();

		JsonApiSingle<String> jsonApi = jsonApi()
			.links(Arrays.asList(new Link("foo").withSelfRel()))
			.relationships(Arrays.asList(new Link("localhost/brother").withRel("brother")))
			.data(data)
			.build();

		String actual = mapper.writeValueAsString(jsonApi);
		assertThat(actual, is(MappingUtils.read(new ClassPathResource("reference.json", getClass()))));
	}
}
