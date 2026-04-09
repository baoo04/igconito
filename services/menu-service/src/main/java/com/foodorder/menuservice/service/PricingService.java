package com.foodorder.menuservice.service;

import com.foodorder.menuservice.config.MenuPricingProperties;
import com.foodorder.menuservice.dto.PriceQuoteResponse;
import com.foodorder.menuservice.entity.Combo;
import com.foodorder.menuservice.entity.FoodItem;
import com.foodorder.menuservice.model.SizeOption;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PricingService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final MenuPricingProperties props;
    private final Clock menuClock;

    public PriceQuoteResponse quoteFood(FoodItem food, SizeOption size) {
        BigDecimal mult = multiplierFor(size);
        BigDecimal afterSize =
                food.getPrice().multiply(mult).setScale(2, RoundingMode.HALF_UP);
        List<String> rules = new ArrayList<>();
        rules.add("SIZE_" + size.name());
        BigDecimal unit = afterSize;
        if (isHappyHour()) {
            unit = applyHappyHourDiscount(afterSize, rules);
        }
        return new PriceQuoteResponse(
                "FOOD",
                food.getId(),
                food.getName(),
                food.isAvailable(),
                food.getPrice(),
                unit,
                size.name(),
                rules);
    }

    public PriceQuoteResponse quoteCombo(Combo combo) {
        boolean available = combo.isAvailable()
                && combo.getItems().stream().allMatch(ci -> ci.getFoodItem().isAvailable());
        BigDecimal base = combo.getBundlePrice();
        List<String> rules = new ArrayList<>();
        rules.add("BUNDLE");
        BigDecimal unit = base.setScale(2, RoundingMode.HALF_UP);
        if (isHappyHour()) {
            unit = applyHappyHourDiscount(unit, rules);
        }
        return new PriceQuoteResponse(
                "COMBO",
                combo.getId(),
                combo.getName(),
                available,
                base,
                unit,
                null,
                rules);
    }

    private BigDecimal multiplierFor(SizeOption size) {
        Map<String, BigDecimal> map = props.getSizeMultipliers();
        if (map == null || map.isEmpty()) {
            return BigDecimal.ONE;
        }
        return map.getOrDefault(size.name(), BigDecimal.ONE);
    }

    private boolean isHappyHour() {
        LocalTime now = LocalTime.now(menuClock);
        LocalTime start = parseHm(props.getHappyHourStart());
        LocalTime end = parseHm(props.getHappyHourEnd());
        return !now.isBefore(start) && now.isBefore(end);
    }

    private static LocalTime parseHm(String raw) {
        try {
            return LocalTime.parse(raw.trim(), HM);
        } catch (DateTimeParseException e) {
            return LocalTime.of(18, 0);
        }
    }

    private BigDecimal applyHappyHourDiscount(BigDecimal amount, List<String> rules) {
        int pct = Math.max(0, Math.min(100, props.getHappyHourDiscountPercent()));
        if (pct == 0) {
            return amount;
        }
        BigDecimal factor = BigDecimal.ONE.subtract(BigDecimal.valueOf(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
        rules.add("HAPPY_HOUR_" + pct + "PCT");
        return amount.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }
}
