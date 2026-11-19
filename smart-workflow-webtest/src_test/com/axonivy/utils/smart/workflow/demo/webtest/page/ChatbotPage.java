package com.axonivy.utils.smart.workflow.demo.webtest.page;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ChatbotPage {

  interface Css {
    String QUESTION = ".chatbot textarea[id$=':question']";
    String SEND = ".chatbot [id$=':send']";
    String USER_MESSAGES = ".chatbot-message-user .chatbot-bubble";
    String AI_ANSWERS = ".chatbot-message-ai .chatbot-answer";
    String ERROR = ".chatbot-error";
    String RETRY = ".chatbot-error [id$=':retry']";
  }

  public SelenideElement question() {
    return $(Css.QUESTION);
  }

  public SelenideElement send() {
    return $(Css.SEND);
  }

  public ElementsCollection userMessages() {
    return $$(Css.USER_MESSAGES);
  }

  public ElementsCollection answers() {
    return $$(Css.AI_ANSWERS);
  }

  public SelenideElement error() {
    return $(Css.ERROR);
  }

  public SelenideElement retry() {
    return $(Css.RETRY);
  }

  public void ask(String text) {
    question().setValue(text);
    send().click();
  }
}
