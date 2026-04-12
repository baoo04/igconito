package com.cinema.customer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class CustomerValidationRules {

  private static final Pattern NAME_PATTERN =
      Pattern.compile("^[\\p{L}][\\p{L}\\s'-]*(?:\\s+[\\p{L}][\\p{L}\\s'-]*)+$");
  private static final Pattern PHONE_PATTERN = Pattern.compile("^0[0-9]{9,10}$");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

  private CustomerValidationRules() {}

  public static List<String> validateBookingFields(String fullName, String email, String phone) {
    List<String> errors = new ArrayList<>();
    if (fullName == null || fullName.isBlank()) {
      errors.add("fullName is required");
    } else {
      String t = fullName.trim();
      if (!NAME_PATTERN.matcher(t).matches()) {
        errors.add(
            "fullName must contain at least two words and only letters, spaces, apostrophes, hyphens");
      }
    }
    if (email == null || email.isBlank()) {
      errors.add("email is required");
    } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
      errors.add("email format is invalid");
    }
    if (phone == null || phone.isBlank()) {
      errors.add("phone is required");
    } else if (!PHONE_PATTERN.matcher(phone.trim()).matches()) {
      errors.add("phone must be 10–11 digits starting with 0 (Vietnam)");
    }
    return errors;
  }
}
