package com.foodorder.menuservice.config;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "menu.pricing")
public class MenuPricingProperties {

    /** IANA zone id, e.g. Asia/Ho_Chi_Minh */
    private String zoneId = "Asia/Ho_Chi_Minh";

    /** Local start time inclusive, HH:mm */
    private String happyHourStart = "18:00";

    /** Local end time exclusive, HH:mm */
    private String happyHourEnd = "20:00";

    /** Percent discount during happy hour (e.g. 10 = 10%) */
    private int happyHourDiscountPercent = 10;

    /** Multiplier applied to base price per size key (S, M, L) */
    private Map<String, BigDecimal> sizeMultipliers =
            Map.of("S", new BigDecimal("1.00"), "M", new BigDecimal("1.00"), "L", new BigDecimal("1.20"));
}
