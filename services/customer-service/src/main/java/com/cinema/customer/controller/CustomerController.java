package com.cinema.customer.controller;

import com.cinema.customer.dto.ApiResponse;
import com.cinema.customer.dto.CustomerRequest;
import com.cinema.customer.dto.CustomerResponse;
import com.cinema.customer.dto.CustomerValidateRequest;
import com.cinema.customer.dto.CustomerValidateResponse;
import com.cinema.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<ApiResponse<CustomerResponse>> upsert(@Valid @RequestBody CustomerRequest request) {
    var result = customerService.upsert(request);
    if (result.created()) {
      return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(result.customer()));
    }
    return ResponseEntity.ok(ApiResponse.ok(result.customer()));
  }

  @GetMapping("/lookup")
  public ResponseEntity<ApiResponse<CustomerResponse>> lookup(
      @RequestParam(required = false) String email, @RequestParam(required = false) String phone) {
    return ResponseEntity.ok(ApiResponse.ok(customerService.lookup(email, phone)));
  }

  @PostMapping("/validate")
  public ResponseEntity<ApiResponse<CustomerValidateResponse>> validate(
      @Valid @RequestBody CustomerValidateRequest request) {
    CustomerValidateResponse body = customerService.validate(request);
    if (!body.isValid()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(
              ApiResponse.<CustomerValidateResponse>builder()
                  .data(body)
                  .message("Validation failed")
                  .status(400)
                  .build());
    }
    return ResponseEntity.ok(ApiResponse.ok(body));
  }
}
