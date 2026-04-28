package com.axonivy.utils.smart.workflow.memory.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.primefaces.model.map.Circle;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.ValueInstantiators;
import com.fasterxml.jackson.databind.deser.impl.PropertyValueBuffer;
import com.fasterxml.jackson.databind.deser.std.StdValueInstantiator;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

public class BusinessDataMemory implements ChatMemoryStore {

  @Override
  public void deleteMessages(Object id) {
    Ivy.repo().deleteById((String)id);
  }

  @Override
  public List<ChatMessage> getMessages(Object id) {
    return findMemory(id).map(m -> m.getMessages()).orElse(List.of());
  }

  private Optional<ChatMemory> findMemory(Object id) {
    return Optional.ofNullable(Ivy.repo().find((String)id, ChatMemory.class));
  }

  @Override
  public void updateMessages(Object id, List<ChatMessage> messages) {
    var existing = findMemory(id);
    if (existing.isEmpty()) {
      var memory = new ChatMemory((String)id, messages);
      Ivy.repo().save(memory);
    } else {
      var memory = existing.get();
      memory = new ChatMemory(memory.id, messages);
      Ivy.repo().save(memory);
    }
  }

  public static class ChatMessageInstantiator extends StdValueInstantiator {

    public ChatMessageInstantiator(DeserializationConfig config, JavaType raw) {
      super(config, raw);
    }

    @Override
    public Object createUsingDefault(DeserializationContext ctxt) throws IOException {
      // TODO Auto-generated method stub
      return super.createUsingDefault(ctxt);
    }

    @Override
    public ValueInstantiator createContextual(DeserializationContext ctxt, BeanDescription beanDesc)
        throws JsonMappingException {
      // TODO Auto-generated method stub

      return super.createContextual(ctxt, beanDesc);
    }

    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, Object[] args) throws IOException {
      // TODO Auto-generated method stub
      return super.createFromObjectWith(ctxt, args);
    }

    @Override
    public Object createFromObjectWith(DeserializationContext ctxt, SettableBeanProperty[] props,
        PropertyValueBuffer buffer) throws IOException {
      // TODO Auto-generated method stub
      return super.createFromObjectWith(ctxt, props, buffer);
    }

  }

  public static record ChatMemory(String id, String messages) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
      MAPPER.activateDefaultTyping(new LaissezFaireSubTypeValidator(), ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);
      MAPPER.registerSubtypes(
        new NamedType(UserMessage.class, ChatMessageType.USER.name().toLowerCase()),
        new NamedType(SystemMessage.class, ChatMessageType.SYSTEM.name().toLowerCase()),
        new NamedType(AiMessage.class, ChatMessageType.AI.name().toLowerCase()),
        new NamedType(CustomMessage.class, ChatMessageType.CUSTOM.name().toLowerCase()),
        new NamedType(ToolExecutionResultMessage.class, ChatMessageType.TOOL_EXECUTION_RESULT.name().toLowerCase())
      );

      MAPPER.registerModules(new SimpleModule(){

@Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);

    context.addValueInstantiators(new ValueInstantiators.Base(){
      @Override
      public ValueInstantiator findValueInstantiator(DeserializationConfig config,
          BeanDescription beanDesc, ValueInstantiator defaultInstantiator) {
        JavaType raw = beanDesc.getType();
        if (ChatMessage.class.isAssignableFrom(raw.getRawClass())) {
          var us = config.getTypeFactory().constructType(UserMessage.class);
          return new ChatMessageInstantiator(config, us);
        }


        // if (List.class.isAssignableFrom(raw)) {
        //   if (defaultInstantiator instanceof StdValueInstantiator &&
        //       beanDesc.getType() instanceof CollectionType) {
        //     return new IvyListInstantiator(
        //         (StdValueInstantiator) defaultInstantiator,
        //         (CollectionType) beanDesc.getType());
        //   }
        // }
        return defaultInstantiator;
      }
    });
  }

        // @Override
        // public <T> SimpleModule addDeserializer(Class<T> type, JsonDeserializer<? extends T> deser) {
        //   // TODO Auto-generated method stub
        //   return super.addDeserializer(UserMessage.class, );
        // }
      });
    }

    public ChatMemory(String id, List<ChatMessage> messages) {
      this(id, read(messages));
    }

    private static String read(List<ChatMessage> messages) {
      try {
        Messages msgs = new Messages(new ArrayList<>(messages));
        var raw = Json.toJson(msgs);
        return raw;
      } catch (Exception e) {
        throw new RuntimeException("Failed to serialize messages", e);
      }
    }

    public List<ChatMessage> getMessages() {
      System.out.println("Deserializing messages: " + messages);
      try {
        return MAPPER.readValue(messages, Messages.class).messages;
      } catch (Exception e) {
        throw new RuntimeException("Failed to deserialize messages", e);
      } 
    }

    public static class Messages {
      @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, 
         include = As.PROPERTY, property = "type")
      public ArrayList<ChatMessage> messages;

      @JsonCreator
      public Messages(@JsonProperty("messages") ArrayList<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
      }
    }

  }
  
}
