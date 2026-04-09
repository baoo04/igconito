package com.foodorder.menuservice.config;

import com.foodorder.menuservice.entity.Category;
import com.foodorder.menuservice.entity.Combo;
import com.foodorder.menuservice.entity.ComboItem;
import com.foodorder.menuservice.entity.FoodItem;
import com.foodorder.menuservice.entity.Review;
import com.foodorder.menuservice.repository.CategoryRepository;
import com.foodorder.menuservice.repository.ComboRepository;
import com.foodorder.menuservice.repository.FoodItemRepository;
import com.foodorder.menuservice.repository.ReviewRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class MenuDemoDataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final ComboRepository comboRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        Category mains =
                categoryRepository.save(Category.builder().name("Mains").description("Main dishes").build());
        Category drinks =
                categoryRepository.save(Category.builder().name("Drinks").description("Beverages").build());
        FoodItem burger = foodItemRepository.save(FoodItem.builder()
                .name("Burger")
                .description("Beef burger")
                .price(new BigDecimal("12.50"))
                .available(true)
                .category(mains)
                .build());
        FoodItem pizza = foodItemRepository.save(FoodItem.builder()
                .name("Pizza")
                .description("Margherita")
                .price(new BigDecimal("10.00"))
                .available(true)
                .category(mains)
                .build());
        FoodItem cola = foodItemRepository.save(FoodItem.builder()
                .name("Cola")
                .description("Cold drink")
                .price(new BigDecimal("2.00"))
                .available(true)
                .category(drinks)
                .build());

        Combo combo = Combo.builder()
                .name("Combo Burger + Cola")
                .description("Burger + 1 Cola (bundle)")
                .bundlePrice(new BigDecimal("13.50"))
                .available(true)
                .items(new ArrayList<>())
                .build();
        combo.getItems().add(ComboItem.builder().combo(combo).foodItem(burger).quantity(1).build());
        combo.getItems().add(ComboItem.builder().combo(combo).foodItem(cola).quantity(1).build());
        comboRepository.save(combo);

        reviewRepository.save(Review.builder()
                .foodItem(pizza)
                .rating(5)
                .comment("Ngon, nóng hổi.")
                .authorName("An")
                .createdAt(Instant.now())
                .build());
        reviewRepository.save(Review.builder()
                .foodItem(pizza)
                .rating(4)
                .comment("Ổn, sẽ mua lại.")
                .authorName("Bảo")
                .createdAt(Instant.now())
                .build());
    }
}
