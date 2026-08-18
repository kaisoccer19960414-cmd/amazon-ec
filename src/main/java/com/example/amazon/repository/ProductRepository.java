package com.example.amazon.repository;

import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    /** 顧客向け一覧・検索用。販売停止中の商品は除外する */
    List<Product> findByIsActiveTrue();

    /** カテゴリ別検索の入り口。ここで取得した候補をさらにスペック・価格で絞り込む */
    List<Product> findByCategoryAndIsActiveTrue(ProductCategory category);
}