package com.axonivy.utils.smart.workflow.demo.supplier.onboarding.bean.interfaces;

import java.util.Arrays;
import java.util.List;

import com.axonivy.utils.smart.workflow.demo.supplier.onboarding.enums.Country;

public interface SupplierFormSupport {

  List<Country> COUNTRIES = Arrays.asList(Country.values());

  List<String> LEGAL_FORMS = List.of(
      "GmbH", "AG", "GmbH & Co. KG", "SE", "UG", "KG", "OHG", "e.K.", "Ltd.", "S.A.", "B.V.", "Other");

  default List<Country> getCountries() {
    return COUNTRIES;
  }

  default List<String> getLegalForms() {
    return LEGAL_FORMS;
  }
}
