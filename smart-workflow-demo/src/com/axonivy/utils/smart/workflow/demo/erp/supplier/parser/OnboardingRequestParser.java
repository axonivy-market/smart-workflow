package com.axonivy.utils.smart.workflow.demo.erp.supplier.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.axonivy.utils.smart.workflow.demo.erp.shared.Address;
import com.axonivy.utils.smart.workflow.demo.erp.document.LegalDocumentType;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierBanking;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierCertification;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.SupplierContact;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.Country;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.Urgency;

public class OnboardingRequestParser {

  private OnboardingRequestParser() {
  }

  public static void applyDraft(OnboardingRequest request, OnboardingRequest draft) {
    if (request == null || draft == null) {
      return;
    }

    if (request.getSupplier() == null) {
      request.setSupplier(new Supplier());
    }
    if (request.getSupplier().getBusinessAddress() == null) {
      request.getSupplier().setBusinessAddress(new Address());
    }

    java.util.Optional.ofNullable(draft.getDepartment()).filter(StringUtils::isNotBlank)
        .ifPresent(request::setDepartment);
    java.util.Optional.ofNullable(draft.getProductsServicesNeeded()).filter(StringUtils::isNotBlank)
        .ifPresent(request::setProductsServicesNeeded);
    java.util.Optional.ofNullable(draft.getExpectedAnnualVolume())
        .ifPresent(request::setExpectedAnnualVolume);
    java.util.Optional.ofNullable(draft.getAdditionalNotes()).filter(StringUtils::isNotBlank)
        .ifPresent(request::setAdditionalNotes);
    java.util.Optional.ofNullable(Urgency.fromString(draft.getUrgency()))
        .map(Urgency::name).ifPresent(request::setUrgency);
    java.util.Optional.ofNullable(draft.getNeededByDate())
        .ifPresent(request::setNeededByDate);

    java.util.Optional.ofNullable(draft.getSupplier()).ifPresent(draftSupplier -> {
      Supplier supplier = request.getSupplier();
      java.util.Optional.ofNullable(draftSupplier.getBusinessName()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setBusinessName);
      java.util.Optional.ofNullable(draftSupplier.getLegalForm()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setLegalForm);
      java.util.Optional.ofNullable(draftSupplier.getVatId()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setVatId);
      java.util.Optional.ofNullable(draftSupplier.getCommercialRegisterNo()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setCommercialRegisterNo);
      java.util.Optional.ofNullable(draftSupplier.getBusinessPurpose()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setBusinessPurpose);
      java.util.Optional.ofNullable(draftSupplier.getPhone()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setPhone);
      java.util.Optional.ofNullable(draftSupplier.getEmail()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setEmail);
      java.util.Optional.ofNullable(draftSupplier.getWebsite()).filter(StringUtils::isNotBlank)
          .ifPresent(supplier::setWebsite);
      java.util.Optional.ofNullable(draftSupplier.getBusinessAddress()).ifPresent(draftAddr -> {
        Address addr = supplier.getBusinessAddress();
        applyCountry(request, draftAddr.getCountry());
        java.util.Optional.ofNullable(draftAddr.getStreet1()).filter(StringUtils::isNotBlank)
            .ifPresent(addr::setStreet1);
        java.util.Optional.ofNullable(draftAddr.getStreet2()).filter(StringUtils::isNotBlank)
            .ifPresent(addr::setStreet2);
        java.util.Optional.ofNullable(draftAddr.getCity()).filter(StringUtils::isNotBlank)
            .ifPresent(addr::setCity);
        java.util.Optional.ofNullable(draftAddr.getState()).filter(StringUtils::isNotBlank)
            .ifPresent(addr::setState);
        java.util.Optional.ofNullable(draftAddr.getZipCode()).filter(StringUtils::isNotBlank)
            .ifPresent(addr::setZipCode);
      });
      applyPrimaryContact(supplier, draftSupplier.getPrimaryContact());
      applyBanking(supplier, draftSupplier.getBanking());
      applyCertifications(supplier, draftSupplier.getCertifications());
    });
  }

  public static void applyCertifications(Supplier supplier, List<SupplierCertification> draftCerts) {
    if (supplier == null || draftCerts == null || draftCerts.isEmpty()) {
      return;
    }
    if (supplier.getCertifications() == null) {
      supplier.setCertifications(new ArrayList<>());
    }
    for (SupplierCertification draftCert : draftCerts) {
      if (draftCert == null || draftCert.getType() == null) {
        continue;
      }
      LegalDocumentType type = draftCert.getType();
      SupplierCertification existing = supplier.getCertifications().stream()
          .filter(c -> type.equals(c.getType()))
          .findFirst()
          .orElse(null);
      if (existing == null) {
        existing = new SupplierCertification(type, null, null, null, false);
        supplier.getCertifications().add(existing);
      }
      java.util.Optional.ofNullable(draftCert.getCertNumber()).filter(StringUtils::isNotBlank)
          .ifPresent(existing::setCertNumber);
      java.util.Optional.ofNullable(draftCert.getExpiryDate()).filter(StringUtils::isNotBlank)
          .ifPresent(existing::setExpiryDate);
      java.util.Optional.ofNullable(draftCert.getDocumentReference()).filter(StringUtils::isNotBlank)
          .ifPresent(existing::setDocumentReference);
    }
  }

  public static void applyBanking(Supplier supplier, SupplierBanking draftBanking) {
    if (supplier == null || draftBanking == null) {
      return;
    }
    if (supplier.getBanking() == null) {
      supplier.setBanking(new SupplierBanking());
    }
    SupplierBanking banking = supplier.getBanking();
    java.util.Optional.ofNullable(draftBanking.getIban()).filter(StringUtils::isNotBlank)
        .ifPresent(banking::setIban);
    java.util.Optional.ofNullable(draftBanking.getBic()).filter(StringUtils::isNotBlank)
        .ifPresent(banking::setBic);
    java.util.Optional.ofNullable(draftBanking.getBankName()).filter(StringUtils::isNotBlank)
        .ifPresent(banking::setBankName);
  }

  public static void applyPrimaryContact(Supplier supplier, SupplierContact draftContact) {
    if (supplier == null || draftContact == null) {
      return;
    }
    if (supplier.getPrimaryContact() == null) {
      supplier.setPrimaryContact(new SupplierContact());
    }
    SupplierContact contact = supplier.getPrimaryContact();
    java.util.Optional.ofNullable(draftContact.getFirstName()).filter(StringUtils::isNotBlank)
        .ifPresent(contact::setFirstName);
    java.util.Optional.ofNullable(draftContact.getLastName()).filter(StringUtils::isNotBlank)
        .ifPresent(contact::setLastName);
    java.util.Optional.ofNullable(draftContact.getJobTitle()).filter(StringUtils::isNotBlank)
        .ifPresent(contact::setJobTitle);
    java.util.Optional.ofNullable(draftContact.getEmail()).filter(StringUtils::isNotBlank)
        .ifPresent(contact::setEmail);
    java.util.Optional.ofNullable(draftContact.getPhone()).filter(StringUtils::isNotBlank)
        .ifPresent(contact::setPhone);
  }

  public static void applyCountry(OnboardingRequest request, String rawCountry) {
    if (request == null || request.getSupplier() == null || request.getSupplier().getBusinessAddress() == null
        || StringUtils.isBlank(rawCountry)) {
      return;
    }

    String normalizedCountry = rawCountry.trim();
    for (Country country : Country.values()) {
      if (country.getCode().equalsIgnoreCase(normalizedCountry)
          || country.getDisplayName().equalsIgnoreCase(normalizedCountry)
          || country.name().equalsIgnoreCase(normalizedCountry)) {
        request.getSupplier().getBusinessAddress().setCountry(country.getCode());
        return;
      }
    }

    request.getSupplier().getBusinessAddress().setCountry(normalizedCountry.toUpperCase(Locale.ROOT));
  }
}
