package com.cinema.customer.service;

import com.cinema.customer.dto.CustomerRequest;
import com.cinema.customer.dto.CustomerResponse;
import com.cinema.customer.dto.CustomerValidateRequest;
import com.cinema.customer.dto.CustomerValidateResponse;
import com.cinema.customer.entity.Customer;
import com.cinema.customer.exception.ResourceNotFoundException;
import com.cinema.customer.repository.CustomerRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

  private final CustomerRepository customerRepository;

  @Transactional
  public UpsertResult upsert(CustomerRequest request) {
    var existing = customerRepository.findByEmailIgnoreCase(request.getEmail().trim());
    if (existing.isPresent()) {
      Customer c = existing.get();
      c.setFullName(request.getFullName().trim());
      c.setPhone(request.getPhone().trim());
      return new UpsertResult(false, toResponse(c));
    }
    Customer c =
        Customer.builder()
            .fullName(request.getFullName().trim())
            .email(request.getEmail().trim().toLowerCase())
            .phone(request.getPhone().trim())
            .build();
    Customer saved = customerRepository.save(c);
    return new UpsertResult(true, toResponse(saved));
  }

  @Transactional(readOnly = true)
  public CustomerResponse lookup(String email, String phone) {
    if (email != null && !email.isBlank()) {
      return customerRepository
          .findByEmailIgnoreCase(email.trim())
          .map(this::toResponse)
          .orElseThrow(() -> new ResourceNotFoundException("Customer not found for email"));
    }
    if (phone != null && !phone.isBlank()) {
      return customerRepository
          .findByPhone(phone.trim())
          .map(this::toResponse)
          .orElseThrow(() -> new ResourceNotFoundException("Customer not found for phone"));
    }
    throw new IllegalArgumentException("Provide email or phone query parameter");
  }

  /** Validates format; if valid and customer exists by email, returns customerId. */
  public CustomerValidateResponse validate(CustomerValidateRequest req) {
    List<String> errors =
        CustomerValidationRules.validateBookingFields(
            req.getFullName(), req.getEmail(), req.getPhone());
    if (!errors.isEmpty()) {
      return CustomerValidateResponse.builder().valid(false).errors(errors).build();
    }
    Long id =
        customerRepository
            .findByEmailIgnoreCase(req.getEmail().trim().toLowerCase())
            .map(Customer::getId)
            .orElse(null);
    return CustomerValidateResponse.builder().valid(true).customerId(id).errors(List.of()).build();
  }

  private CustomerResponse toResponse(Customer c) {
    return CustomerResponse.builder()
        .id(c.getId())
        .fullName(c.getFullName())
        .email(c.getEmail())
        .phone(c.getPhone())
        .createdAt(c.getCreatedAt())
        .build();
  }

  public record UpsertResult(boolean created, CustomerResponse customer) {}
}
