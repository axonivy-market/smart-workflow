package com.axonivy.utils.smart.workflow.demo.model;

import dev.langchain4j.model.output.structured.Description;

@Description("Extracted profile of a job candidate from their CV")
public class CandidateProfile {

  @Description("Full name of the candidate")
  private String fullName;

  @Description("Contact email address")
  private String email;

  @Description("List of technical and soft skills, comma-separated")
  private String skills;

  @Description("Total years of relevant work experience")
  private int yearsOfExperience;

  @Description("Highest education level obtained")
  private String education;

  @Description("Brief summary of work experience highlights")
  private String experienceSummary;

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getSkills() {
    return skills;
  }

  public void setSkills(String skills) {
    this.skills = skills;
  }

  public int getYearsOfExperience() {
    return yearsOfExperience;
  }

  public void setYearsOfExperience(int yearsOfExperience) {
    this.yearsOfExperience = yearsOfExperience;
  }

  public String getEducation() {
    return education;
  }

  public void setEducation(String education) {
    this.education = education;
  }

  public String getExperienceSummary() {
    return experienceSummary;
  }

  public void setExperienceSummary(String experienceSummary) {
    this.experienceSummary = experienceSummary;
  }
}
