package com.axonivy.utils.smart.workflow.model.spi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.internal.SpiLoader;
import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.IvyTest;
import ch.ivyteam.ivy.project.model.Project;

@IvyTest
class TestSpiLoader {

  @Test
  void load() {
    Project project = IProcessModelVersion.current().project();
    var impls = new SpiLoader(project).load(ChatModelProvider.class);
    assertThat(impls).isNotEmpty();
    var dummies = impls.stream()
        .filter(p -> (p instanceof DummyChatModelProvider))
        .toList();
    assertThat(dummies)
        .as("SPI loader finds imlementors")
        .isNotEmpty();
  }

}
