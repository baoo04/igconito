package com.foodorder.orderservice.controller;

import com.foodorder.orderservice.dto.CreateOrderRequest;
import com.foodorder.orderservice.dto.OrderLineRequest;
import com.foodorder.orderservice.dto.OrderResponse;
import com.foodorder.orderservice.dto.OrderStatusUpdateRequest;
import com.foodorder.orderservice.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> list() {
        return orderService.listAll();
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return orderService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @PostMapping("/{id}/items")
    public OrderResponse addItems(@PathVariable Long id, @Valid @RequestBody List<OrderLineRequest> lines) {
        return orderService.addLines(id, lines);
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    public OrderResponse deleteItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        return orderService.removeItem(orderId, itemId);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse status(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request);
    }
}
