package com.flashsale.config;

import com.flashsale.domain.Product;
import com.flashsale.domain.Stock;
import com.flashsale.repository.ProductRepository;
import com.flashsale.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 기동 시 테스트 데이터 시딩.
 * ddl-auto: create 라 매 기동마다 스키마가 새로 만들어지므로, 항상 여기서 시작 상태를 만든다.
 *
 * 시작 상태: Product(id=1) + Stock(quantity=100)
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    public static final int INITIAL_STOCK = 100;

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            return;
        }
        Product product = productRepository.save(new Product("한정판 스니커즈", 100_000L));
        stockRepository.save(new Stock(product.getId(), INITIAL_STOCK));
    }
}
