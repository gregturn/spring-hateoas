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
package org.springframework.hateoas.client;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.springframework.hateoas.Link;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/**
 * A {@link TraversalHandler} used to print out critical information while hopping. Useful to debug an action
 * sequence through {@link Traverson}.
 *
 * @author Greg Turnquist
 * @since 0.18.0
 */
public class PrintWriterTraversalHandler implements TraversalHandler {

	private PrintWriter writer;

	public PrintWriterTraversalHandler(PrintWriter writer) {
		this.writer = writer;
	}

	@Override
	public void beforeHop(String uri, List<Hop> rels) {

		writer.println("--------------------------------------------");
		writer.println(rels.size() > 0 ? "About to hop..." : "Last stop");
	}

	@Override
	public void afterHop(String uri, HttpEntity<?> request, ResponseEntity<String> response, Rels.Rel rel, Link link, Map<String, Object> mergedParameters) {

		writer.println("URI: " + uri);
		writer.println("Request headers:");
		for (Map.Entry<String, List<String>> header : request.getHeaders().entrySet()) {
			writer.println("\t" + header.getKey() + ": " + StringUtils.collectionToCommaDelimitedString(header.getValue()));
		}
		writer.println();
		writer.println("HTTP response status: " + response.getStatusCode() + " " + response.getStatusCode().getReasonPhrase());
		writer.println();
		writer.println("Response headers:");
		for (Map.Entry<String, List<String>> header : response.getHeaders().entrySet()) {
			writer.println("\t" + header.getKey() + ": " + StringUtils.collectionToCommaDelimitedString(header.getValue()));
		}
		writer.println();
		writer.println("Response body:");
		writer.println(response.getBody());
		writer.println(rel.toString() + " -> " + link);
	}
}
