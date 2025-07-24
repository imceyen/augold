// ===============================
// OrderItemService.java - 주문 상품 서비스
// ===============================

package com.global.augold.order.service;

import com.global.augold.order.dto.OrderItemDTO;
import com.global.augold.order.entity.OrderItem;
import com.global.augold.order.repository.OrderItemRepository;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    /**
     * 주문번호로 주문 상품 목록 조회 (DTO 변환)
     */
    public List<OrderItemDTO> getOrderItemsByOrderNumber(String orderNumber) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByOrderNumber(orderNumber);
            return convertToOrderItemDTOs(orderItems);

        } catch (Exception e) {
            log.error("주문 상품 목록 조회 실패: orderNumber={}, error={}", orderNumber, e.getMessage(), e);
            throw new RuntimeException("주문 상품 목록 조회에 실패했습니다.", e);
        }
    }

    /**
     * 상품별 주문 내역 조회
     */
    public List<OrderItemDTO> getOrderItemsByProductId(String productId) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findByProductId(productId);
            return convertToOrderItemDTOs(orderItems);

        } catch (Exception e) {
            log.error("상품별 주문 내역 조회 실패: productId={}, error={}", productId, e.getMessage(), e);
            throw new RuntimeException("상품별 주문 내역 조회에 실패했습니다.", e);
        }
    }

    /**
     * 주문 상품 상세 조회
     */
    public OrderItemDTO getOrderItemDetail(String orderNumber, String productId) {
        try {
            OrderItem orderItem = orderItemRepository.findByOrderNumberAndProductId(orderNumber, productId)
                    .orElseThrow(() -> new IllegalArgumentException("주문 상품을 찾을 수 없습니다."));

            return convertToOrderItemDTO(orderItem);

        } catch (Exception e) {
            log.error("주문 상품 상세 조회 실패: orderNumber={}, productId={}, error={}",
                    orderNumber, productId, e.getMessage(), e);
            throw new RuntimeException("주문 상품 상세 조회에 실패했습니다.", e);
        }
    }

    /**
     * 주문별 총 상품 개수 조회
     */
    public Integer getTotalQuantityByOrder(String orderNumber) {
        try {
            Integer totalQuantity = orderItemRepository.getTotalQuantityByOrderNumber(orderNumber);
            return totalQuantity != null ? totalQuantity : 0;

        } catch (Exception e) {
            log.error("주문별 총 상품 개수 조회 실패: orderNumber={}, error={}", orderNumber, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 주문별 총 상품 금액 조회
     */
    public BigDecimal getTotalAmountByOrder(String orderNumber) {
        try {
            BigDecimal totalAmount = orderItemRepository.getTotalAmountByOrderNumber(orderNumber);
            return totalAmount != null ? totalAmount : BigDecimal.ZERO;

        } catch (Exception e) {
            log.error("주문별 총 상품 금액 조회 실패: orderNumber={}, error={}", orderNumber, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 상품별 총 판매량 조회
     */
    public Integer getTotalSoldQuantityByProduct(String productId) {
        try {
            Integer totalSold = orderItemRepository.getTotalSoldQuantityByProductId(productId);
            return totalSold != null ? totalSold : 0;

        } catch (Exception e) {
            log.error("상품별 총 판매량 조회 실패: productId={}, error={}", productId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 인기 상품 조회 (판매량 기준)
     */
    public List<OrderItemDTO> getTopSellingProducts(int limit) {
        try {
            List<Object[]> results = orderItemRepository.findTopSellingProducts(limit);
            List<OrderItemDTO> topProducts = new ArrayList<>();

            for (Object[] result : results) {
                String productId = (String) result[0];
                Long totalQuantity = (Long) result[1];

                // 상품 정보 조회
                Product product = productRepository.findByProductId(productId).orElse(null);
                if (product != null) {
                    OrderItemDTO dto = OrderItemDTO.builder()
                            .productId(productId)
                            .productName(product.getProductName())
                            .karatCode(product.getKaratCode())
                            .imageUrl(product.getImageUrl())
                            .quantity(totalQuantity.intValue())
                            .unitPrice(BigDecimal.valueOf(product.getFinalPrice()))
                            .goldWeight(product.getGoldWeight())
                            .productGroup(product.getProductGroup())
                            .build();

                    topProducts.add(dto);
                }
            }

            return topProducts;

        } catch (Exception e) {
            log.error("인기 상품 조회 실패: limit={}, error={}", limit, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // ===============================
    // Private 메서드들
    // ===============================

    /**
     * OrderItem Entity를 OrderItemDTO로 변환
     */
    private OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        // 상품 정보 조회
        Product product = productRepository.findByProductId(orderItem.getProductId()).orElse(null);

        OrderItemDTO.OrderItemDTOBuilder builder = OrderItemDTO.builder()
                .orderItemId(orderItem.getOrderItemId())
                .orderNumber(orderItem.getOrderNumber())
                .productId(orderItem.getProductId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .finalAmount(orderItem.getFinalAmount());

        // 상품 정보가 있으면 추가
        if (product != null) {
            builder.productName(product.getProductName())
                    .karatCode(product.getKaratCode())
                    .imageUrl(product.getImageUrl())
                    .goldWeight(product.getGoldWeight())
                    .productGroup(product.getProductGroup());
        } else {
            // 상품 정보가 없는 경우 기본값
            builder.productName("상품명 없음")
                    .karatCode("N/A")
                    .imageUrl("/images/default-product.jpg")
                    .goldWeight(0.0)
                    .productGroup("기타");
        }

        return builder.build();
    }

    /**
     * OrderItem Entity 리스트를 OrderItemDTO 리스트로 변환
     */
    private List<OrderItemDTO> convertToOrderItemDTOs(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
    }
}