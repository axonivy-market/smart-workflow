package com.axonivy.utils.smart.workflow.model.spi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;

import ch.ivyteam.ivy.application.IProcessModelVersion;
import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class TestSpiLoader {

  @Test
  void load() {
    var pmv = IProcessModelVersion.current();
    var impls = new SpiLoader(pmv).load(ChatModelProvider.class);
    assertThat(impls).isNotEmpty();
    var dummies = impls.stream()
        .filter(p -> (p instanceof DummyChatModelProvider))
        .toList();
    assertThat(dummies)
        .as("SPI loader finds imlementors")
        .isNotEmpty();
  }

}
