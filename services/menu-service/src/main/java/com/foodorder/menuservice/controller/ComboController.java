package com.foodorder.menuservice.controller;

import com.foodorder.menuservice.dto.ComboCreateRequest;
import com.foodorder.menuservice.dto.ComboResponse;
import com.foodorder.menuservice.dto.PriceQuoteResponse;
import com.foodorder.menuservice.service.ComboService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
public class ComboController {

    private final ComboService comboService;

    @GetMapping
    public List<ComboResponse> list() {
        return comboService.listAll();
    }

    @GetMapping("/{id}")
    public ComboResponse get(@PathVariable Long id) {
        return comboService.getById(id);
    }

    @GetMapping("/{id}/price")
    public PriceQuoteResponse price(@PathVariable Long id) {
        return comboService.quotePrice(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComboResponse create(@Valid @RequestBody ComboCreateRequest request) {
        return comboService.create(request);
    }
}

