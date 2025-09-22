package com.axonivy.utils.smart.workflow.demo.shopping.common;

public class Address {
  private String street1;
  private String street2;
  private String city;
  private String state;
  private String zipCode;
  private String country;

  public Address() {
  }

  public Address(String street1, String street2, String city, String state, String zipCode, String country) {
    this.street1 = street1;
    this.street2 = street2;
    this.city = city;
    this.state = state;
    this.zipCode = zipCode;
    this.country = country;
  }
  
  public String getStreet1() {
    return street1;
  }

  public void setStreet1(String street1) {
    this.street1 = street1;
  }

  public String getStreet2() {
    return street2;
  }

  public void setStreet2(String street2) {
    this.street2 = street2;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (street1 != null)
      sb.append(street1);
    if (street2 != null && !street2.isEmpty()) {
      if (sb.length() > 0)
        sb.append(", ");
      sb.append(street2);
    }
    if (city != null) {
      if (sb.length() > 0)
        sb.append(", ");
      sb.append(city);
    }
    if (state != null) {
      if (sb.length() > 0)
        sb.append(", ");
      sb.append(state);
    }
    if (zipCode != null) {
      if (sb.length() > 0)
        sb.append(" ");
      sb.append(zipCode);
    }
    if (country != null) {
      if (sb.length() > 0)
        sb.append(", ");
      sb.append(country);
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    Address address = (Address) o;

    if (street1 != null ? !street1.equals(address.street1) : address.street1 != null)
      return false;
    if (street2 != null ? !street2.equals(address.street2) : address.street2 != null)
      return false;
    if (city != null ? !city.equals(address.city) : address.city != null)
      return false;
    if (state != null ? !state.equals(address.state) : address.state != null)
      return false;
    if (zipCode != null ? !zipCode.equals(address.zipCode) : address.zipCode != null)
      return false;
    return country != null ? country.equals(address.country) : address.country == null;
  }

  @Override
  public int hashCode() {
    int result = street1 != null ? street1.hashCode() : 0;
    result = 31 * result + (street2 != null ? street2.hashCode() : 0);
    result = 31 * result + (city != null ? city.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
    result = 31 * result + (country != null ? country.hashCode() : 0);
    return result;
  }
}