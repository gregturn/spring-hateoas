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

import static org.springframework.hateoas.jsonapi.JsonApiCollection.*;
import static org.springframework.hateoas.jsonapi.JsonApiData.*;
import static org.springframework.hateoas.jsonapi.JsonApiSingle.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.ContainerDeserializerBase;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

/**
 * Jackson 2 module implementation to render {@link Resources}, {@link Resource}, and {@link ResourceSupport}
 * instances in HAL compatible JSON.
 *
 * @author Greg Turnquist
 */
public class Jackson2JsonApiModule extends SimpleModule {

	private static Set<String> IANA_RELS = new HashSet<String>(Arrays.asList(Link.REL_SELF, Link.REL_FIRST,
			Link.REL_PREVIOUS, Link.REL_NEXT, Link.REL_LAST));

	public Jackson2JsonApiModule() {

		super("json-api-module", new Version(1, 0, 0, null, "org.springframework.hateoas", "spring-hateoas"));

		setMixInAnnotation(ResourceSupport.class, ResourceSupportMixin.class);
		setMixInAnnotation(Resource.class, ResourceMixin.class);
		setMixInAnnotation(Resources.class, ResourcesMixin.class);
		setMixInAnnotation(PagedResources.class, PagedResourcesMixin.class);
	}

	/**
	 * Returns whether the module was already registered in the given {@link ObjectMapper}.
	 *
	 * @param mapper must not be {@literal null}.
	 * @return
	 */
	public static boolean isAlreadyRegisteredIn(ObjectMapper mapper) {

		Assert.notNull(mapper, "ObjectMapper must not be null!");
		return ResourceSupportMixin.class.equals(mapper.findMixInClassFor(ResourceSupport.class));
	}

	/**
	 * Split up a list of links into IANA-approved rels vs. domain specific relationships.
	 */
	private static class OrganizedLinks {

		private final List<Link> ianaLinks;
		private final List<Link> relationshipLinks;

		public OrganizedLinks(List<Link> links) {
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

	/**
	 * Extract the id of a URI.
	 * @param link
	 * @return
	 */
	private static String getId(Link link) {

		String[] uriParts = link.expand().getHref().split("/");
		return uriParts[uriParts.length - 1];
	}

	/**
	 * Custom {@link JsonSerializer} to render Link instances in JSON API compatible JSON.
	 *
	 * @author Alexander Baetz
	 * @author Oliver Gierke
	 */
	public static class JsonApiLinkListSerializer extends ContainerSerializer<List<Link>> implements ContextualSerializer {

		private final BeanProperty property;
		private final MessageSourceAccessor messageSource;

		public JsonApiLinkListSerializer(MessageSourceAccessor messageSource) {
			this(null, messageSource);
		}

		public JsonApiLinkListSerializer(BeanProperty property, MessageSourceAccessor messageSource) {

			super(List.class, false);
			this.property = property;
			this.messageSource = messageSource;
		}

		@Override
		public void serialize(List<Link> value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException, JsonGenerationException {

			ResourceSupport resource = new ResourceSupport();
			resource.add(value);

			OrganizedLinks organizedLinks = new OrganizedLinks(resource.getLinks());

			JsonApiSingleBuilder jsonApi = jsonApi()
				.data(jsonApiData()
						.type(resource.getClass().getSimpleName())
						.id(getId(resource.getId()))
						.links(organizedLinks.ianaLinks)
						.relationships(organizedLinks.getRelationshipLinks())
						.build());

			provider.findValueSerializer(JsonApiSingle.class, property).serialize(jsonApi.build(), jgen, provider);
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new JsonApiLinkListSerializer(property, messageSource);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean isEmpty(List<Link> value) {
			return value.isEmpty();
		}

		@Override
		public boolean hasSingleElement(List<Link> value) {
			return value.size() == 1;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
			return null;
		}

	}

	public static class JsonApiResourceSerializer extends ContainerSerializer<Resource<?>> implements ContextualSerializer {

		private final BeanProperty property;

		public JsonApiResourceSerializer() {
			this(null);
		}

		public JsonApiResourceSerializer(BeanProperty property) {

			super(Resource.class, false);
			this.property = property;
		}

		@Override
		public void serialize(Resource<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {

			OrganizedLinks organizedLinks = new OrganizedLinks(value.getLinks());

			JsonApiSingleBuilder jsonApi = jsonApi()
				.data(jsonApiData()
						.type(value.getClass().getSimpleName())
						.id(getId(value.getId()))
						.attributes(value.getContent())
						.links(organizedLinks.ianaLinks)
						.relationships(organizedLinks.getRelationshipLinks())
						.build());

			provider.findValueSerializer(JsonApiSingle.class, property).serialize(jsonApi.build(), jgen, provider);
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new JsonApiResourceSerializer(property);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean hasSingleElement(Resource<?> value) {
			return true;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
			return null;
		}
	}

	public static class JsonApiResourcesSerializer extends ContainerSerializer<Resources<?>> implements ContextualSerializer {

		private final BeanProperty property;

		public JsonApiResourcesSerializer() {
			this(null);
		}

		public JsonApiResourcesSerializer(BeanProperty property) {

			super(Resources.class, false);
			this.property = property;
		}

		@Override
		public void serialize(Resources<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {

			OrganizedLinks organizedLinks = new OrganizedLinks(value.getLinks());

			JsonApiCollectionBuilder jsonApi = jsonApiCollection();

			List<JsonApiData<?>> data = new ArrayList<JsonApiData<?>>();

			for (Object content : value.getContent()) {

				if (ClassUtils.isAssignableValue(Resource.class, content)) {

					Resource resource = (Resource) content;

					OrganizedLinks resourceLinks = new OrganizedLinks(resource.getLinks());

					data.add(jsonApiData()
						.type(resource.getContent().getClass().getSimpleName())
						.id(getId(resource.getId()))
						.attributes(resource.getContent())
						.links(resourceLinks.getIanaLinks())
						.relationships(resourceLinks.getRelationshipLinks())
						.build());

				} else {

					data.add(jsonApiData()
						.type(content.getClass().getSimpleName())
						.attributes(content)
						.build());

				}
			}

			// Top-level links
			jsonApi
				.data(data)
				.links(organizedLinks.getIanaLinks())
				.relationships(organizedLinks.getRelationshipLinks());

			provider.findValueSerializer(JsonApiCollection.class, property).serialize(jsonApi.build(), jgen, provider);
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new JsonApiResourcesSerializer(property);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean isEmpty(Resources<?> value) {
			return value.getContent().size() == 0;
		}

		@Override
		public boolean hasSingleElement(Resources<?> value) {
			return value.getContent().size() == 1;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
			return null;
		}
	}

	public static class JsonApiPagedResourcesSerializer extends ContainerSerializer<PagedResources<?>> implements ContextualSerializer {

		private final BeanProperty property;

		public JsonApiPagedResourcesSerializer() {
			this(null);
		}

		public JsonApiPagedResourcesSerializer(BeanProperty property) {

			super(Resources.class, false);
			this.property = property;
		}

		@Override
		public void serialize(PagedResources<?> value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException {

			OrganizedLinks organizedLinks = new OrganizedLinks(value.getLinks());

			JsonApiCollectionBuilder jsonApi = jsonApiCollection();

			List<JsonApiData<?>> data = new ArrayList<JsonApiData<?>>();

			for (Object content : value.getContent()) {

				if (ClassUtils.isAssignableValue(Resource.class, content)) {

					Resource resource = (Resource) content;

					OrganizedLinks resourceLinks = new OrganizedLinks(resource.getLinks());

					data.add(jsonApiData()
						.type(resource.getContent().getClass().getSimpleName())
						.id(getId(resource.getId()))
						.attributes(resource.getContent())
						.links(resourceLinks.getIanaLinks())
						.relationships(resourceLinks.getRelationshipLinks())
						.build());

				} else {

					data.add(jsonApiData()
						.type(content.getClass().getSimpleName())
						.attributes(content)
						.build());

				}
			}

			// Top-level links and page metadata
			jsonApi
				.data(data)
				.links(organizedLinks.getIanaLinks())
				.relationships(organizedLinks.getRelationshipLinks())
				.meta(value.getMetadata());

			provider.findValueSerializer(JsonApiCollection.class, property).serialize(jsonApi.build(), jgen, provider);
		}

		@Override
		public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
			return new JsonApiPagedResourcesSerializer(property);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonSerializer<?> getContentSerializer() {
			return null;
		}

		@Override
		public boolean isEmpty(PagedResources<?> value) {
			return value.getContent().size() == 0;
		}

		@Override
		public boolean hasSingleElement(PagedResources<?> value) {
			return value.getContent().size() == 1;
		}

		@Override
		protected ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
			return null;
		}
	}

	public static class JsonApiLinkListDeserializer extends ContainerDeserializerBase<List<Link>> {

		@SuppressWarnings("deprecation")
		public JsonApiLinkListDeserializer() {
			super(List.class);
		}

		@Override
		public JavaType getContentType() {
			return null;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public List<Link> deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {

			JsonApiSingle<?> jsonApi = jp.getCodec().readValues(jp, JsonApiSingle.class).next();

			List<Link> links = new ArrayList<Link>();
			links.addAll(jsonApi.getLinks());
			links.addAll(jsonApi.getRelationships());

			return links;
		}
	}

	public static class JsonApiResourceDeserializer extends ContainerDeserializerBase<Resource<?>>
			implements ContextualDeserializer {

		private final JavaType contentType;

		@SuppressWarnings("deprecation")
		public JsonApiResourceDeserializer() {
			this(JsonApiSingle.class, null);
		}

		public JsonApiResourceDeserializer(JavaType selfType) {
			this(null, selfType);
		}

		private JsonApiResourceDeserializer(Class<?> type, JavaType contentType) {

			super(type);
			this.contentType = contentType;
		}

		@Override
		public JavaType getContentType() {
			return this.contentType;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public Resource<?> deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {

			JavaType rootType = JacksonHelper.findRootType(this.contentType);
			JavaType wrappedType = ctxt.getTypeFactory().constructParametricType(JsonApiSingle.class, rootType);

			JsonApiSingle<?> jsonApi = (JsonApiSingle<?>) jp.getCodec().readValues(jp, wrappedType).next();

			List<Link> links = new ArrayList<Link>();
			links.addAll(jsonApi.getLinks());
			links.addAll(jsonApi.getRelationships());

			return new Resource<Object>(jsonApi.getData().getAttributes(), links);
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
													BeanProperty property) throws JsonMappingException {

			if (property != null) {
				JavaType vc = property.getType().getContentType();
				JsonApiResourceDeserializer des = new JsonApiResourceDeserializer(vc);
				return des;
			} else {
				return new JsonApiResourceDeserializer(ctxt.getContextualType());
			}
		}

	}

	public static class JsonApiResourcesDeserializer extends ContainerDeserializerBase<Resources>
			implements ContextualDeserializer {

		private final JavaType contentType;

		@SuppressWarnings("deprecation")
		public JsonApiResourcesDeserializer() {
			this(JsonApiCollection.class, null);
		}

		public JsonApiResourcesDeserializer(JavaType selfType) {
			this(null, selfType);
		}

		private JsonApiResourcesDeserializer(Class<?> type, JavaType contentType) {

			super(type);
			this.contentType = contentType;
		}

		@Override
		public JavaType getContentType() {
			return this.contentType;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public Resources deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			JavaType rootType = JacksonHelper.findRootType(this.contentType);
			JavaType wrappedType = ctxt.getTypeFactory().constructParametricType(JsonApiCollection.class, rootType);

			JsonApiCollection<?> jsonApi = (JsonApiCollection<?>) jp.getCodec().readValues(jp, wrappedType).next();

			List<Object> items = new ArrayList<Object>();

			for (JsonApiData<?> item : jsonApi.getData()) {

				Object itemData = item.getAttributes();

				if (this.contentType.hasGenericTypes()) {

					List<Link> links = new ArrayList<Link>();
					links.addAll(item.getLinks());
					links.addAll(item.getRelationships());

					if (this.contentType.containedType(0).hasRawClass(Resource.class)) {
						items.add(new Resource<Object>(itemData, links));
					} else {
						items.add(itemData);
					}
				}

			}

			List<Link> links = new ArrayList<Link>();
			links.addAll(jsonApi.getLinks());
			links.addAll(jsonApi.getRelationships());

			return new Resources(items, links);
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
													BeanProperty property) throws JsonMappingException {

			if (property != null) {
				JavaType vc = property.getType().getContentType();
				JsonApiResourcesDeserializer des = new JsonApiResourcesDeserializer(vc);
				return des;
			} else {
				return new JsonApiResourcesDeserializer(ctxt.getContextualType());
			}
		}
	}

	public static class JsonApiPagedResourcesDeserializer extends ContainerDeserializerBase<PagedResources>
			implements ContextualDeserializer {

		private final JavaType contentType;

		@SuppressWarnings("deprecation")
		public JsonApiPagedResourcesDeserializer() {
			this(JsonApiCollection.class, null);
		}

		public JsonApiPagedResourcesDeserializer(JavaType selfType) {
			this(null, selfType);
		}

		private JsonApiPagedResourcesDeserializer(Class<?> type, JavaType contentType) {

			super(type);
			this.contentType = contentType;
		}

		@Override
		public JavaType getContentType() {
			return this.contentType;
		}

		@Override
		public JsonDeserializer<Object> getContentDeserializer() {
			return null;
		}

		@Override
		public PagedResources deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {

			JavaType rootType = JacksonHelper.findRootType(this.contentType);
			JavaType wrappedType = ctxt.getTypeFactory().constructParametricType(JsonApiCollection.class, rootType);

			JsonApiCollection<?> jsonApi = (JsonApiCollection<?>) jp.getCodec().readValues(jp, wrappedType).next();

			List<Object> items = new ArrayList<Object>();

			for (JsonApiData<?> item : jsonApi.getData()) {

				Object itemData = item.getAttributes();

				if (this.contentType.hasGenericTypes()) {

					List<Link> links = new ArrayList<Link>();
					links.addAll(item.getLinks());
					links.addAll(item.getRelationships());

					if (this.contentType.containedType(0).hasRawClass(Resource.class)) {
						items.add(new Resource<Object>(itemData, links));
					} else {
						items.add(itemData);
					}
				}

			}

			PagedResources.PageMetadata pageMetadata = (PagedResources.PageMetadata) jsonApi.getMeta();

			List<Link> links = new ArrayList<Link>();
			links.addAll(jsonApi.getLinks());
			links.addAll(jsonApi.getRelationships());

			return new PagedResources(items, pageMetadata, links);
		}

		@Override
		public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {

			if (property != null) {
				JavaType vc = property.getType().getContentType();
				JsonApiPagedResourcesDeserializer des = new JsonApiPagedResourcesDeserializer(vc);
				return des;
			} else {
				return new JsonApiPagedResourcesDeserializer(ctxt.getContextualType());
			}
		}

	}

	public static class JsonApiHandlerInstantiator extends HandlerInstantiator {

		private final Map<Class<?>, Object> instanceMap = new HashMap<Class<?>, Object>();

		public JsonApiHandlerInstantiator(MessageSourceAccessor messageSource) {

			this.instanceMap.put(JsonApiPagedResourcesSerializer.class, new JsonApiPagedResourcesSerializer());
			this.instanceMap.put(JsonApiResourcesSerializer.class, new JsonApiResourcesSerializer());
			this.instanceMap.put(JsonApiResourceSerializer.class, new JsonApiResourceSerializer());
			this.instanceMap.put(JsonApiLinkListSerializer.class, new JsonApiLinkListSerializer(messageSource));
		}

		private Object findInstance(Class<?> type) {

			Object result = instanceMap.get(type);
			return result != null ? result : BeanUtils.instantiateClass(type);
		}

		@Override
		public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> deserClass) {
			return (JsonDeserializer<?>) findInstance(deserClass);
		}

		@Override
		public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> keyDeserClass) {
			return (KeyDeserializer) findInstance(keyDeserClass);
		}

		@Override
		public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> serClass) {
			return (JsonSerializer<?>) findInstance(serClass);
		}

		@Override
		public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> builderClass) {
			return (TypeResolverBuilder<?>) findInstance(builderClass);
		}

		@Override
		public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> resolverClass) {
			return (TypeIdResolver) findInstance(resolverClass);
		}
	}

	/**
	 * Jackson utility methods.
	 */
	public abstract static class JacksonHelper {

		/**
		 * Navigate a chain of parametric types (e.g. Resources&lt;Resource&lt;String&gt;&gt;) until you find the innermost type (String).
		 *
		 * @param contentType
		 * @return
		 */
		public static JavaType findRootType(JavaType contentType) {

			if (contentType.hasGenericTypes()) {
				return findRootType(contentType.containedType(0));
			} else {
				return contentType;
			}
		}
	}

}
