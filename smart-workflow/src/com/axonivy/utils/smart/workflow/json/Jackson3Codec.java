package com.axonivy.utils.smart.workflow.json;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.PolymorphicTypes;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;

class Jackson3Codec implements Json.JsonCodec {

  private final ObjectMapper objectMapper;

  private static ObjectMapper createObjectMapper() {

    SimpleModule module = new SimpleModule("langchain4j-module");

    module.addSerializer(LocalDate.class, new StdSerializer<>(LocalDate.class){
      @Override
      public void serialize(LocalDate value, JsonGenerator gen, SerializationContext provider) {
        gen.writeString(value.format(ISO_LOCAL_DATE));
      }
    });

    module.addDeserializer(LocalDate.class, new ValueDeserializer<>(){
      @Override
      public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
          int year = node.get("year").asInt();
          int month = node.get("month").asInt();
          int day = node.get("day").asInt();
          return LocalDate.of(year, month, day);
        } else {
          return LocalDate.parse(node.asString(), ISO_LOCAL_DATE);
        }
      }
    });

    module.addSerializer(LocalTime.class, new StdSerializer<>(LocalTime.class){
      @Override
      public void serialize(LocalTime value, JsonGenerator gen, SerializationContext provider) {
        gen.writeString(value.format(ISO_LOCAL_TIME));
      }
    });

    module.addDeserializer(LocalTime.class, new ValueDeserializer<>(){
      @Override
      public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
          int hour = node.get("hour").asInt();
          int minute = node.get("minute").asInt();
          int second = Optional.ofNullable(node.get("second"))
              .map(JsonNode::asInt)
              .orElse(0);
          int nano = Optional.ofNullable(node.get("nano"))
              .map(JsonNode::asInt)
              .orElse(0);
          return LocalTime.of(hour, minute, second, nano);
        } else {
          return LocalTime.parse(node.asString(), ISO_LOCAL_TIME);
        }
      }
    });

    module.addSerializer(LocalDateTime.class, new StdSerializer<>(LocalDateTime.class){
      @Override
      public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext provider) {
        gen.writeString(value.format(ISO_LOCAL_DATE_TIME));
      }
    });

    module.addDeserializer(LocalDateTime.class, new ValueDeserializer<LocalDateTime>(){
      @Override
      public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonNode node = p.readValueAsTree();
        if (node.isObject()) {
          JsonNode date = node.get("date");
          int year = date.get("year").asInt();
          int month = date.get("month").asInt();
          int day = date.get("day").asInt();
          JsonNode time = node.get("time");
          int hour = time.get("hour").asInt();
          int minute = time.get("minute").asInt();
          int second = Optional.ofNullable(time.get("second"))
              .map(JsonNode::asInt)
              .orElse(0);
          int nano = Optional.ofNullable(time.get("nano"))
              .map(JsonNode::asInt)
              .orElse(0);
          return LocalDateTime.of(year, month, day, hour, minute, second, nano);
        } else {
          return LocalDateTime.parse(node.asString(), ISO_LOCAL_DATE_TIME);
        }
      }
    });

    var mapper = JsonMapper.builderWithJackson2Defaults();
    mapper.changeDefaultVisibility(visibility -> visibility.withFieldVisibility(Visibility.ANY))
        .disable(SerializationFeature.INDENT_OUTPUT) // disabled on purpose to save tokens when sending tool results to LLM
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // enabled on purpose to prevent issues caused by LLM hallucinations
        .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
        .findAndAddModules()
        .addModule(module);

    var original = JsonMapper.shared()._deserializationContext().getAnnotationIntrospector();
    // Make sealed interfaces/classes deserializable as polymorphic types without the user
    // having to add @JsonTypeInfo+@JsonSubTypes. We synthesize equivalent metadata via a
    // custom AnnotationIntrospector consulted ahead of Jackson's default one.
    mapper.annotationIntrospector(AnnotationIntrospectorPair.pair(
        new SealedTypePolymorphicIntrospector(), original));
    return mapper.build();
  }

  /**
   * Constructs a JacksonJsonCodec instance with the provided ObjectMapper.
   *
   * @param objectMapper the ObjectMapper to use for JSON serialization and deserialization.
   */
  public Jackson3Codec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Constructs a JacksonJsonCodec instance with a default ObjectMapper.
   * The default ObjectMapper is configured with custom serializers and deserializers
   * for Java 8 date/time types such as LocalDate, LocalTime, and LocalDateTime.
   * It also registers other modules found on the classpath
   * and throws exceptions for unknown properties to improve handling of unexpected input.
   */
  public Jackson3Codec() {
    this(createObjectMapper());
  }

  @Override
  public String toJson(Object o) {
    return objectMapper.writeValueAsString(o);
  }

  @Override
  public <T> T fromJson(String json, Class<T> type) {
    return objectMapper.readValue(json, type);
  }

  @Override
  public <T> T fromJson(String json, Type type) {
    return objectMapper.readValue(json, objectMapper.constructType(type));
  }

  /**
   * Returns the ObjectMapper instance used for JSON processing.
   *
   * @return the ObjectMapper instance.
   */
  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Synthesizes {@code @JsonTypeInfo} + {@code @JsonSubTypes} metadata for sealed types that
   * carry no Jackson polymorphism annotations of their own. With this introspector consulted
   * ahead of Jackson's default one, sealed bases dispatch natively via the same discriminator
   * langchain4j puts in the schema — no custom deserializer needed.
   */
  private static final class SealedTypePolymorphicIntrospector extends tools.jackson.databind.introspect.NopAnnotationIntrospector {

    @Override
    public Object findTypeResolverBuilder(MapperConfig<?> config, Annotated ann) {
      Class<?> raw = ann.getRawType();
      if (!shouldHandle(raw)) {
        return null;
      }
      String discriminatorPropertyName = PolymorphicTypes.discriminatorPropertyName(raw);
      StdTypeResolverBuilder builder = new StdTypeResolverBuilder()
          .init(JsonTypeInfo.Value.construct(
              JsonTypeInfo.Id.NAME,
              JsonTypeInfo.As.PROPERTY,
              discriminatorPropertyName,
              null,  // defaultImpl
              false, // idVisible
              null,  // requireTypeIdForSubtypes
              null   // writeTypeIdForDefaultImpl
          ), null);
      return builder;
    }

    @Override
    public List<NamedType> findSubtypes(MapperConfig<?> config, Annotated a) {
      Class<?> raw = a.getRawType();
      if (!shouldHandle(raw)) {
        return null;
      }
      return PolymorphicTypes.findConcreteSubtypes(raw).stream()
          .map(sub -> new NamedType(sub, PolymorphicTypes.discriminatorValue(raw, sub)))
          .toList();
    }

    private static boolean shouldHandle(Class<?> raw) {
      // Step in for any polymorphic base that doesn't already declare its own type-info
      // strategy via @JsonTypeInfo. This covers both sealed types (no annotations) and
      // types that only use @JsonSubTypes for subtype enumeration.
      return raw.getAnnotation(JsonTypeInfo.class) == null && PolymorphicTypes.isPolymorphic(raw);
    }
  }
}
