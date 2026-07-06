package com.axonivy.utils.smart.workflow.model.spi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.smart.workflow.model.dummy.DummyChatModelProvider;
import com.axonivy.utils.smart.workflow.model.spi.ChatModelProvider;
import com.axonivy.utils.smart.workflow.spi.internal.SpiLoader;

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

  @Test
  void load_noDuplicates_samePmvTwice() {
    var pmv = IProcessModelVersion.current();
    var loader = new SpiLoader(pmv) {
      @Override
      protected Stream<IProcessModelVersion> pmvsInScope() {
        return Stream.of(pmv, pmv); // same PMV twice simulates duplicate dependency declarations
      }
    };
    var impls = loader.load(ChatModelProvider.class);
    var classNames = impls.stream()
        .map(impl -> impl.getClass().getName())
        .toList();
    assertThat(classNames)
        .as("each provider class must appear only once even if the same PMV is in scope multiple times")
        .doesNotHaveDuplicates();
  }

}
