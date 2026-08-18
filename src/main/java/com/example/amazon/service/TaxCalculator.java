package com.example.amazon.service;

import org.springframework.stereotype.Component;

/**
 * 消費税の計算をこの1箇所に集約する。
 *
 * 税率8%・端数は切り捨て。Dellの価格(products.price)は税抜きのまま扱い、
 * 税込み価格への変換はAmazon側(このクラス)だけで行う。SMBCは実際に
 * 請求する金額(税込み)を受け取るだけで、税率そのものは一切関知しない。
 *
 * 商品一覧・検索結果・商品詳細・カートの「表示価格」と、OrderServiceが
 * 実際にSMBCへ請求する「決済金額」は、必ずこのクラス経由で計算すること。
 * 表示と実際の請求額がズレることを防ぐため。
 */
@Component
public class TaxCalculator {

    /** 消費税率。将来変わる可能性があるのでここに集約しておく。 */
    private static final double TAX_RATE = 0.08;

    /** 単価1点分の税込み価格(一覧・詳細・カートの単価表示用)。端数は切り捨て。 */
    public int includedUnitPrice(int unitPrice) {
        return (int) Math.floor(unitPrice * (1 + TAX_RATE));
    }

    /** 単価×数量の税込み合計(カート小計・カート合計・実際の請求額で共通して使う)。端数は切り捨て。 */
    public int includedTotal(int unitPrice, int quantity) {
        return (int) Math.floor((double) unitPrice * quantity * (1 + TAX_RATE));
    }
}
