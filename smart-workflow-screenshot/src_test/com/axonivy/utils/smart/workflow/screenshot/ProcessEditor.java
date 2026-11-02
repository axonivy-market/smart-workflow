package com.axonivy.utils.smart.workflow.screenshot;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.partialText;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.actions;

import java.net.URI;
import java.time.Duration;

import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.SelenideElement;

public class ProcessEditor {
  public static String getEditorUrl(String project, String file) {
    var context = EngineUrl.securityContextName();
    var server = URI.create(EngineUrl.base()).getRawAuthority();
    if (!context.isBlank()) {
      server += "/" + context.replaceFirst("^/+", "");
    }

    var editorUrl = EngineUrl.create()
        .context("")
        .app("")
        .path("process-editor/")
        .queryParam("server", server)
        .queryParam("app", EngineUrl.applicationName())
        .queryParam("project", project)
        .queryParam("file", file)
        .toUrl();
    System.out.println("Opening editor URL: " + editorUrl);
    return editorUrl;
  }

  public static Inscription openInscription(String elementId) {
    var element = $("#sprotty_" + elementId).shouldBe(visible);
    actions().moveToElement(element).click().perform();
    element.shouldHave(cssClass("selected"), Duration.ofSeconds(10));
    actions().moveToElement(element).doubleClick().perform();
    var inscription = $("aside#inscription-ui").shouldNotHave(cssClass("hidden"), Duration.ofSeconds(10));
    return new Inscription(inscription);
  }

  public static class Inscription {
    private final SelenideElement element;

    public Inscription(SelenideElement element) {
      this.element = element;
    }

    public void openTab(String tabName) {
      var configurationTab = $$("aside#inscription-ui [role='tab']")
          .findBy(attribute("aria-label", tabName))
          .shouldBe(visible, Duration.ofSeconds(10));
      if (!"true".equals(configurationTab.getAttribute("aria-selected"))) {
        configurationTab.click();
      }
      configurationTab.shouldHave(attribute("aria-selected", "true"));
      $("#" + configurationTab.getAttribute("aria-controls"))
          .shouldBe(visible, Duration.ofSeconds(10));
    }

    public void expandGroup(String name, boolean doExpand) {
      expand(findGroup(name), Boolean.toString(doExpand));
    }

    private SelenideElement findGroup(String expectedText) {
      return element.$$("button").findBy(partialText(expectedText));
    }

    private static void expand(SelenideElement group, String doExpand) {
      if (!doExpand.equals(group.getAttribute("aria-expanded"))) {
        group.click();
      }
      group.shouldHave(attribute("aria-expanded", doExpand));
    }

  }
}
