package com.example.amazon.controller;

import com.example.amazon.entity.CpuMaker;
import com.example.amazon.entity.PcSpec;
import com.example.amazon.entity.Product;
import com.example.amazon.entity.ProductCategory;
import com.example.amazon.repository.PcSpecRepository;
import com.example.amazon.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductSearchController {

    private static final int PAGE_SIZE = 20;
    private static final List<Integer> RAM_OPTIONS = List.of(8, 16, 32, 64);
    private static final List<Integer> SSD_OPTIONS = List.of(256, 512, 1024, 2048);

    private final ProductRepository productRepository;
    private final PcSpecRepository pcSpecRepository;

    @GetMapping("/products/search")
    public String categorySelect() {
        return "product-search-category-select";
    }

    @GetMapping("/products/search/{category}")
    public String search(@PathVariable ProductCategory category,
                         @RequestParam(required = false) Integer priceMin,
                         @RequestParam(required = false) Integer priceMax,
                         @RequestParam(required = false) List<Integer> ram,
                         @RequestParam(required = false) List<Integer> ssd,
                         @RequestParam(required = false) List<CpuMaker> cpu,
                         @RequestParam(required = false) Boolean hasGpu,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {

        List<Product> candidates = productRepository.findByCategoryAndIsActiveTrue(category);

        List<Product> filtered = candidates.stream()
                .filter(p -> priceMin == null || p.getPrice() >= priceMin)
                .filter(p -> priceMax == null || p.getPrice() <= priceMax)
                .collect(Collectors.toList());

        boolean isPc = category == ProductCategory.LAPTOP || category == ProductCategory.DESKTOP;
        Map<String, PcSpec> specMap = Map.of();

        if (isPc) {
            List<String> ids = filtered.stream().map(Product::getId).toList();
            specMap = pcSpecRepository.findAllById(ids).stream()
                    .collect(Collectors.toMap(PcSpec::getProductId, s -> s));

            Map<String, PcSpec> finalSpecMap = specMap;
            filtered = filtered.stream()
                    .filter(p -> {
                        PcSpec spec = finalSpecMap.get(p.getId());
                        if (spec == null) return false;
                        if (ram != null && !ram.isEmpty() && !ram.contains(spec.getRamGb())) return false;
                        if (ssd != null && !ssd.isEmpty() && !ssd.contains(spec.getSsdGb())) return false;
                        if (cpu != null && !cpu.isEmpty() && !cpu.contains(spec.getCpuMaker())) return false;
                        if (hasGpu != null && !hasGpu.equals(spec.getHasGpu())) return false;
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        int totalCount = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(safePage * PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalCount);
        List<Product> pageItems = filtered.subList(fromIndex, toIndex);

        // テンプレート側で算数計算をさせず、表示用の値はここで確定させておく
        int displayFrom = totalCount == 0 ? 0 : fromIndex + 1;
        int displayTo = toIndex;

        model.addAttribute("category", category);
        model.addAttribute("products", pageItems);
        model.addAttribute("specMap", specMap);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("displayFrom", displayFrom);
        model.addAttribute("displayTo", displayTo);
        model.addAttribute("priceMin", priceMin);
        model.addAttribute("priceMax", priceMax);
        model.addAttribute("ramOptions", RAM_OPTIONS);
        model.addAttribute("ssdOptions", SSD_OPTIONS);
        model.addAttribute("selectedRam", ram != null ? ram : List.of());
        model.addAttribute("selectedSsd", ssd != null ? ssd : List.of());
        model.addAttribute("selectedCpu", cpu != null ? cpu : List.of());
        model.addAttribute("selectedHasGpu", hasGpu);

        return "product-search-results";
    }
}