package com.global.augold.product.service;

import com.global.augold.product.dto.ProductDTO;
import com.global.augold.product.entity.Product;
import com.global.augold.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 상품 관련 비즈니스 로직을 처리하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 모든 상품 조회
     */
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 특정 상품 조회
     */
    public ProductDTO getProductById(String id) {
        return productRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    /**
     * 상품 저장/수정
     */
    public ProductDTO saveProduct(ProductDTO dto) {
        Product saved = productRepository.save(toEntity(dto));
        return toDTO(saved);
    }

    /**
     * 상품 삭제
     */
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

    /**
     * Entity → DTO 변환
     */
    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .productId(product.getProductId())
                .karatCode(product.getKaratCode())
                .ctgrId(product.getCtgrId())
                .productName(product.getProductName())
                .basePrice(product.getBasePrice())
                .goldWeight(product.getGoldWeight())
                .finalPrice(product.getFinalPrice())
                .imageUrl(product.getImageUrl())
                .description(product.getDescription())
                .subCtgr(product.getSubCtgr())
                .build();
    }

    /**
     * DTO → Entity 변환
     */
    private Product toEntity(ProductDTO dto) {
        return Product.builder()
                .productId(dto.getProductId())
                .karatCode(dto.getKaratCode())
                .ctgrId(dto.getCtgrId())
                .productName(dto.getProductName())
                .basePrice(dto.getBasePrice())
                .goldWeight(dto.getGoldWeight())
                .finalPrice(dto.getFinalPrice())
                .imageUrl(dto.getImageUrl())
                .description(dto.getDescription())
                .subCtgr(dto.getSubCtgr())
                .build();
    }

    /**
     * subCtgr 값에 따라 productId 자동 생성
     */
    public String generateNextProductId(String subCtgr) {
        String prefix;
        switch (subCtgr) {
            case "귀걸이": prefix = "E"; break;
            case "반지":   prefix = "R"; break;
            case "목걸이": prefix = "N"; break;
            case "골드바": prefix = "G"; break;
            case "감사패":
            case "돌반지":
            case "카네이션기념품": prefix = "S"; break;
            default: prefix = "X"; break;
        }

        String fullPrefix = "PROD_" + prefix;
        List<Product> products = productRepository.findByProductIdStartingWith(fullPrefix);

        int maxNumber = products.stream()
                .map(p -> p.getProductId().replace(fullPrefix, ""))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);

        int nextNumber = maxNumber + 1;

        // 여기서 숫자 부분을 5자리로 포맷팅
        return String.format("%s%05d", fullPrefix, nextNumber);
    }


}