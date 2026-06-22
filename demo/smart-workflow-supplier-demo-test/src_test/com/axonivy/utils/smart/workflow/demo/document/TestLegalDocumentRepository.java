package com.axonivy.utils.smart.workflow.demo.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.axonivy.utils.ai.SupplierDemoTestProcessData;
import com.axonivy.utils.smart.workflow.demo.document.enums.LegalDocumentType;

import ch.ivyteam.ivy.bpm.engine.client.BpmClient;
import ch.ivyteam.ivy.bpm.engine.client.element.BpmProcess;
import ch.ivyteam.ivy.bpm.exec.client.IvyProcessTest;

@IvyProcessTest
class TestLegalDocumentRepository {

  private static final BpmProcess TEST_PROCESS = BpmProcess.name("SupplierDemoTestProcess");

  @Test
  void saveAndFindById_returnsCorrectDocument(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testLegalDocumentRepo_saveAndFindById")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundLegalDocument()).isNotNull();
    assertThat(data.getFoundLegalDocument().getDocumentType()).isEqualTo(LegalDocumentType.ISO_9001);
    assertThat(data.getFoundLegalDocument().getFileName()).isEqualTo("ISO9001.pdf");
  }

  @Test
  void findByObjectId_savedDocument_returnsOne(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testLegalDocumentRepo_findByObjectId")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getLegalDocumentCount()).isEqualTo(1);
  }

  @Test
  void delete_removesDocument_findByIdReturnsNull(BpmClient client) {
    var res = client.start().process(TEST_PROCESS.elementName("testLegalDocumentRepo_delete")).execute();
    SupplierDemoTestProcessData data = res.data().last();
    assertThat(data.getFoundLegalDocument()).isNull();
  }
}
