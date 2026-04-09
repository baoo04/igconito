package com.foodorder.orderservice.service;

import com.foodorder.orderservice.dto.CreateOrderRequest;
import com.foodorder.orderservice.dto.MenuPriceQuotePayload;
import com.foodorder.orderservice.dto.OrderItemResponse;
import com.foodorder.orderservice.dto.OrderLineRequest;
import com.foodorder.orderservice.dto.OrderResponse;
import com.foodorder.orderservice.dto.OrderStatusUpdateRequest;
import com.foodorder.orderservice.entity.CustomerOrder;
import com.foodorder.orderservice.entity.OrderItem;
import com.foodorder.orderservice.entity.OrderStatus;
import com.foodorder.orderservice.repository.CustomerOrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final MenuCatalogClient menuCatalogClient;

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        CustomerOrder order = CustomerOrder.builder()
                .customerName(request.customerName())
                .status(OrderStatus.PLACED)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .build();
        for (OrderLineRequest line : request.lines()) {
            order.addItem(buildLine(line));
        }
        order.recalculateTotal();
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return orderRepository.findById(id).map(this::toResponse).orElseThrow(OrderService::orderNotFound);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse addLines(Long orderId, List<OrderLineRequest> lines) {
        CustomerOrder order = orderRepository.findById(orderId).orElseThrow(OrderService::orderNotFound);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify order in this status");
        }
        for (OrderLineRequest line : lines) {
            order.addItem(buildLine(line));
        }
        order.recalculateTotal();
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse removeItem(Long orderId, Long itemId) {
        CustomerOrder order = orderRepository.findById(orderId).orElseThrow(OrderService::orderNotFound);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify order in this status");
        }
        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId() != null && i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));
        order.removeItem(item);
        order.recalculateTotal();
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatusUpdateRequest request) {
        CustomerOrder order = orderRepository.findById(id).orElseThrow(OrderService::orderNotFound);
        order.setStatus(request.status());
        return toResponse(orderRepository.save(order));
    }

    private OrderItem buildLine(OrderLineRequest line) {
        boolean hasFood = line.menuItemId() != null;
        boolean hasCombo = line.comboId() != null;
        if (hasFood == hasCombo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each line must provide exactly one of menuItemId or comboId");
        }

        if (hasFood) {
            MenuPriceQuotePayload quote = menuCatalogClient.quoteFoodPrice(line.menuItemId(), line.size());
            if (!quote.available()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item not available: " + quote.name());
            }
            BigDecimal unit = quote.unitPrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.quantity()));
            return OrderItem.builder()
                    .itemKind(Objects.requireNonNullElse(quote.kind(), "FOOD"))
                    .menuItemId(quote.id())
                    .size(quote.size())
                    .menuItemName(quote.name())
                    .quantity(line.quantity())
                    .unitPrice(unit)
                    .lineTotal(lineTotal)
                    .build();
        }

        MenuPriceQuotePayload quote = menuCatalogClient.quoteComboPrice(line.comboId());
        if (!quote.available()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Combo not available: " + quote.name());
        }
        BigDecimal unit = quote.unitPrice();
        BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(line.quantity()));
        return OrderItem.builder()
                .itemKind(Objects.requireNonNullElse(quote.kind(), "COMBO"))
                .menuItemId(quote.id())
                .size(null)
                .menuItemName(quote.name())
                .quantity(line.quantity())
                .unitPrice(unit)
                .lineTotal(lineTotal)
                .build();
    }

    private OrderResponse toResponse(CustomerOrder o) {
        List<OrderItemResponse> items = o.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getId(),
                        i.getItemKind(),
                        i.getMenuItemId(),
                        i.getSize(),
                        i.getMenuItemName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getLineTotal()))
                .toList();
        return new OrderResponse(o.getId(), o.getCustomerName(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt(), items);
    }

    private static ResponseStatusException orderNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
    }
}
