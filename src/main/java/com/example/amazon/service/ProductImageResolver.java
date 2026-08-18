package com.example.amazon.service;

import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 商品画像の割り当てを行う。
 *
 * DBに画像パスは持たせず、static/images/productsimages/ 配下のファイル名を
 * カテゴリ名(laptop/desktop/monitor/accessory)の接頭辞で自動的にグループ分けする。
 * 例: laptop.jpg, laptop2.jpg, laptop3.jpg は全部LAPTOPカテゴリの候補になる。
 *
 * こうしておくと、Dell側でgitbash等から新しい商品をどれだけ追加しても
 * (カテゴリさえ合っていれば)何もコード変更せずに画像が自動で付く。
 * 同じ商品IDには常に同じ画像を返すので、リロードのたびに画像が変わることはない。
 * 該当カテゴリの画像がまだ1枚も無い場合は、既存のカテゴリアイコン(images/{category}.png)にフォールバックする。
 */
@Slf4j
@Component
public class ProductImageResolver {

    private static final String IMAGES_CLASSPATH_LOCATION = "classpath:/static/images/productsimages/*";
    private static final String IMAGES_URL_PREFIX = "/images/productsimages/";

    private Map<ProductCategory, List<String>> imagesByCategory = new EnumMap<>(ProductCategory.class);

    @PostConstruct
    void scanImages() {
        Map<ProductCategory, List<String>> result = new EnumMap<>(ProductCategory.class);
        for (ProductCategory category : ProductCategory.values()) {
            result.put(category, new ArrayList<>());
        }

        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(IMAGES_CLASSPATH_LOCATION);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                String lowerFilename = filename.toLowerCase();

                for (ProductCategory category : ProductCategory.values()) {
                    if (lowerFilename.startsWith(category.name().toLowerCase())) {
                        result.get(category).add(IMAGES_URL_PREFIX + filename);
                        break;
                    }
                }
            }

            result.values().forEach(Collections::sort);
        } catch (IOException e) {
            log.warn("商品画像フォルダ(productsimages)の読み込みに失敗しました: {}", e.getMessage());
        }

        for (ProductCategory category : ProductCategory.values()) {
            log.info("商品画像 {}件をロードしました: category={}", result.get(category).size(), category);
        }

        this.imagesByCategory = result;
    }

    /** 商品カテゴリに応じた画像URLを1枚返す。同じ商品なら常に同じ画像を返す。 */
    public String resolve(Product product) {
        List<String> candidates = imagesByCategory.getOrDefault(product.getCategory(), List.of());

        if (candidates.isEmpty()) {
            // まだそのカテゴリの画像が用意されていない場合は、カテゴリ選択画面で使っている
            // アイコン画像を暫定的に使う
            return "/images/" + product.getCategory().name().toLowerCase() + ".png";
        }

        int index = Math.floorMod(product.getId().hashCode(), candidates.size());
        return candidates.get(index);
    }
}
