package com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.builder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.axonivy.utils.smart.workflow.demo.erp.supplier.model.Supplier;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.OnboardingRequest;
import com.axonivy.utils.smart.workflow.demo.erp.supplier.onboarding.RequestSummaryLine;

public class OnboardingRequestSummaryBuilder {

  interface SummaryLabels {
    String LABEL_REQUESTED_BY      = "Requested by";
    String LABEL_DEPARTMENT        = "Department";
    String LABEL_SUPPLIER          = "Supplier";
    String LABEL_LOCATION          = "Location";
    String LABEL_PRODUCTS_SERVICES = "Products / services";
    String LABEL_ANNUAL_VOLUME     = "Annual volume";
    String LABEL_URGENCY           = "Urgency";
    String LABEL_NEEDED_BY         = "Needed by";
    String LABEL_NOTES             = "Notes";

    String FORMAT_ANNUAL_VOLUME   = "EUR %.0f";
    String VAT_SEPARATOR          = " · VAT ";
    String ADDRESS_PART_SEPARATOR = ", ";
  }

  private OnboardingRequestSummaryBuilder() {
  }

  public static List<RequestSummaryLine> build(OnboardingRequest request) {
    return Stream.of(
        simpleFields(request),
        supplierLine(request.getSupplier()),
        locationLine(request.getSupplier()))
        .flatMap(Function.identity())
        .toList();
  }

  private static Stream<RequestSummaryLine> simpleFields(OnboardingRequest request) {
    return Stream.of(
        line(SummaryLabels.LABEL_REQUESTED_BY, request.getRequestedBy()),
        line(SummaryLabels.LABEL_DEPARTMENT, request.getDepartment()),
        line(SummaryLabels.LABEL_PRODUCTS_SERVICES, request.getProductsServicesNeeded()),
        request.getExpectedAnnualVolume() == null
            ? Stream.<RequestSummaryLine>empty()
            : Stream.of(new RequestSummaryLine(
                SummaryLabels.LABEL_ANNUAL_VOLUME,
                String.format(SummaryLabels.FORMAT_ANNUAL_VOLUME, request.getExpectedAnnualVolume()))),
        line(SummaryLabels.LABEL_URGENCY, request.getUrgency()),
        line(SummaryLabels.LABEL_NEEDED_BY, request.getNeededByDate()),
        line(SummaryLabels.LABEL_NOTES, request.getAdditionalNotes()))
        .flatMap(Function.identity());
  }

  private static Stream<RequestSummaryLine> supplierLine(Supplier supplier) {
    if (supplier == null || !hasValue(supplier.getBusinessName())) {
      return Stream.empty();
    }
    String value = Stream.of(supplier.getBusinessName(), supplier.getVatId())
        .filter(OnboardingRequestSummaryBuilder::hasValue)
        .collect(Collectors.joining(SummaryLabels.VAT_SEPARATOR));
    return Stream.of(new RequestSummaryLine(SummaryLabels.LABEL_SUPPLIER, value));
  }

  private static Stream<RequestSummaryLine> locationLine(Supplier supplier) {
    return line(SummaryLabels.LABEL_LOCATION, formatLocation(supplier));
  }

  private static String formatLocation(Supplier supplier) {
    if (supplier == null || supplier.getBusinessAddress() == null) {
      return null;
    }
    var address = supplier.getBusinessAddress();
    return Stream.of(address.getCity(), address.getCountry())
        .filter(OnboardingRequestSummaryBuilder::hasValue)
        .collect(Collectors.joining(SummaryLabels.ADDRESS_PART_SEPARATOR));
  }

  private static Stream<RequestSummaryLine> line(String label, String value) {
    return hasValue(value)
        ? Stream.of(new RequestSummaryLine(label, value))
        : Stream.empty();
  }

  private static Stream<RequestSummaryLine> line(String label, Object value) {
    return value != null
        ? Stream.of(new RequestSummaryLine(label, value.toString()))
        : Stream.empty();
  }

  private static boolean hasValue(String value) {
    return value != null && !value.isBlank();
  }
}
