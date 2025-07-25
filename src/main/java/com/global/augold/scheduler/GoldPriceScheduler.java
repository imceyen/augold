package com.global.augold.scheduler;

import com.global.augold.goldPrice.Service.GoldPriceService;
import com.global.augold.goldPrice.dto.GoldPriceDTO;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoldPriceScheduler {

    private final GoldPriceService goldPriceService;
    private final ProductRepository productRepository;

    /**
     * 서버 시작 시 골드바 가격 초기화
     */
    @PostConstruct
    public void initGoldBarPrices() {
        log.info("🚀 서버 시작 - 골드바 가격 초기화 시작...");

        try {
            // 1. 엑셀에서 최신 금시세 가져오기
            GoldPriceDTO latestPrice = goldPriceService.getTodayGoldPrice();
            double pricePerGram = latestPrice.getPricePerGram();

            log.info("📈 최신 금시세: {}원/g (날짜: {})", pricePerGram, latestPrice.getEffectiveDate());

            // 2. 골드바 카테고리 상품들 조회
            List<Product> goldBars = productRepository.findByCtgrId("CTGR-00002");

            if (goldBars.isEmpty()) {
                log.warn("⚠️ 골드바 상품이 없습니다.");
                return;
            }

            log.info("🔍 업데이트할 골드바 상품 수: {}개", goldBars.size());

            // 3. 각 골드바 상품 가격 업데이트
            int updatedCount = 0;
            for (Product goldBar : goldBars) {
                // 기존 가격 저장 (로그용)
                double oldPrice = goldBar.getFinalPrice() != null ? goldBar.getFinalPrice() : 0;

                // 새로운 가격 계산
                goldBar.setBasePrice(pricePerGram);  // 시세 저장
                double newFinalPrice = pricePerGram * goldBar.getGoldWeight() * 1.1; // 10% 수수료
                goldBar.setFinalPrice(newFinalPrice);

                // DB 저장
                productRepository.save(goldBar);
                updatedCount++;

                log.info("✅ {} ({:.3f}g): ₩{:,.0f} → ₩{:,.0f}",
                        goldBar.getProductName(),
                        goldBar.getGoldWeight(),
                        oldPrice,
                        newFinalPrice);
            }

            log.info("🎉 골드바 가격 초기화 완료! ({}개 상품 업데이트)", updatedCount);

        } catch (Exception e) {
            log.error("❌ 골드바 가격 초기화 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 수동 실행용 메서드 (관리자용)
     */
    public void manualUpdate() {
        log.info("🔧 수동 골드바 가격 업데이트 실행");
        initGoldBarPrices();
    }
}