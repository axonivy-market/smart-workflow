package com.axonivy.utils.smart.orchestrator.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.orchestrator.demo.dto.SupportTicket;
import com.axonivy.utils.smart.orchestrator.utils.IdGenerationUtils;
import com.axonivy.utils.smart.orchestrator.utils.JsonUtils;

import ch.ivyteam.ivy.environment.Ivy;
import dev.langchain4j.internal.Json;

public class SupportTicketService {

  private static final String VARIABLE_KEY = "AiDemo.SupportTicket";

  private static SupportTicketService instance;

  public static SupportTicketService getInstance() {
    if (instance == null) {
      instance = new SupportTicketService();
    }

    return instance;
  }

  public List<SupportTicket> findAll() {
    try {
      return JsonUtils.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), SupportTicket.class);
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  public void save(SupportTicket ticket) {
    if (ticket == null) {
      return;
    }

    List<SupportTicket> existingTickets = Optional
        .ofNullable(JsonUtils.jsonValueToEntities(Ivy.var().get(VARIABLE_KEY), SupportTicket.class))
        .orElseGet(ArrayList::new);

    if (StringUtils.isBlank(ticket.getId())) {
      // Create new ticket
      ticket.setId(IdGenerationUtils.generateRandomId());
      existingTickets.add(ticket);
    } else {
      // Update existing ticket
      boolean isSaved = false;
      for (int i = 0; i < existingTickets.size(); i++) {
        if (existingTickets.get(i).getId().equals(ticket.getId())) {
          existingTickets.set(i, ticket);
          isSaved = true;
          break;
        }
      }

      if (!isSaved) {
        existingTickets.add(ticket);
      }
    }

    Ivy.var().set(VARIABLE_KEY, Json.toJson(existingTickets));
  }

  public SupportTicket findById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    SupportTicket found = findAll().stream().filter(ticket -> ticket.getId().equals(id)).findFirst().orElse(null);

    if (StringUtils.isNotBlank(found.getEmployeeUsername())) {
      found.setRequestor(EmployeeService.getInstance().findByUsername(found.getEmployeeUsername()));
    }

    return found;
  }
}
