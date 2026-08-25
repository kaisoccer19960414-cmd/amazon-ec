package com.example.amazon.repository;

import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {

    /** 顧客向け一覧用(ページネーション対応)。販売停止中の商品は除外し、商品IDの昇順で固定表示する。
     *  ORDER BYを指定しないと、購入時のcachedStock更新などで行の物理的な並びが変わり、
     *  一覧の表示順が意図せず入れ替わってしまうため、明示的にソートしている。 */
    Page<Product> findByIsActiveTrueOrderByIdAsc(Pageable pageable);

    /** カテゴリ別検索の入り口。ここで取得した候補をさらにスペック・価格で絞り込む */
    List<Product> findByCategoryAndIsActiveTrue(ProductCategory category);
}