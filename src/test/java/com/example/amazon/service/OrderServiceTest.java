package com.example.amazon.service;

import com.example.amazon.dto.request.OrderRequest;
import com.example.amazon.entity.Order;
import com.example.amazon.entity.OrderStatus;
import com.example.amazon.entity.Product;
import com.example.amazon.exception.DellReservationException;
import com.example.amazon.exception.SmbcPaymentException;
import com.example.amazon.repository.OrderRepository;
import com.example.amazon.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService(Sagaオーケストレーション)の単体テスト。
 * DellClient/SmbcClientをモックに差し替えることで、実際に3サーバーを
 * 起動しなくても、4つの状態遷移パターンを繰り返し検証できるようにする。
 *
 * テスト名(5つ)
 * 正常系_〜COMPLETEDになる	3つのサーバーを立ち上げて商品購入していたテスト
 * 在庫確保に失敗〜STOCK_FAILED	Dellの在庫を0にして試していたテスト
 * 決済に失敗し在庫解放が成功〜CANCELLED	価格を残高より高くして試していたテスト
 * 決済に失敗し在庫解放も3回とも失敗〜COMPENSATION_FAILED	 FORCE_RELEASE_FAILUREフラグを一時追加して試していたテスト
 * 存在しない商品ID〜例外	未検証だった異常系
 *
 *
 * 5つのテストがそれぞれ何を保証したか
 *
 * COMPLETEDのテスト: 在庫確保も決済も両方うまくいったとき、ちゃんとCOMPLETEDになり、決済IDが正しく記録されることを保証
 * STOCK_FAILEDのテスト: 在庫が確保できなかったとき、決済処理(SMBC)には一切進まないことを保証。「在庫がないのに、うっかりお金だけ引き落としてしまう」というバグを防いでいる証明
 * CANCELLEDのテスト: 決済が失敗したとき、在庫の解放(補償処理)がちゃんと1回だけ行われて、正しく取り消し完了になることを保証
 * COMPENSATION_FAILEDのテスト: 在庫解放が3回とも失敗したとき、「無限にリトライし続けず、ちゃんと3回で諦めて要対応の状態にする」という設計通りに動くことを保証
 * 商品が存在しないテスト: そもそも存在しない商品を注文しようとしたら、注文自体が作られずにきちんとエラーになることを保証
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DellClient dellClient;

    @Mock
    private SmbcClient smbcClient;

    @InjectMocks
    private OrderService orderService;

    private static final String PRODUCT_ID = "PRD-000001";
    private static final long RESERVATION_ID = 100L;
    private static final long TRANSACTION_ID = 200L;

    private OrderRequest request;
    private Product product;

    @BeforeEach
    void setUp() {
        request = new OrderRequest();
        request.setUserId(1L);
        request.setProductId(PRODUCT_ID);
        request.setQuantity(1);
        request.setToken("token_dummy");

        product = new Product(PRODUCT_ID, "テスト商品", 10000, 5);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
        lenient().when(orderRepository.countByIdStartingWith(anyString())).thenReturn(0L);
    }

    @Test
    void 正常系_在庫確保と決済がどちらも成功したらCOMPLETEDになる() {
        when(dellClient.reserve(anyString(), eq(PRODUCT_ID), eq(1))).thenReturn(RESERVATION_ID);
        when(smbcClient.pay(anyString(), eq("token_dummy"), eq(10000))).thenReturn(TRANSACTION_ID);

        Order order = orderService.placeOrder(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getTransactionId()).isEqualTo(TRANSACTION_ID);
        verify(dellClient).confirm(order.getId(), RESERVATION_ID);
        verify(dellClient, never()).release(anyString(), anyLong());
    }

    @Test
    void 在庫確保に失敗したらSTOCK_FAILEDになり決済は呼ばれない() {
        when(dellClient.reserve(anyString(), eq(PRODUCT_ID), eq(1)))
                .thenThrow(new DellReservationException("OUT_OF_STOCK", "在庫が不足しています"));

        Order order = orderService.placeOrder(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.STOCK_FAILED);
        verify(smbcClient, never()).pay(anyString(), anyString(), anyInt());
    }

    @Test
    void 決済に失敗し在庫解放が成功したらCANCELLEDになる() {
        when(dellClient.reserve(anyString(), eq(PRODUCT_ID), eq(1))).thenReturn(RESERVATION_ID);
        when(smbcClient.pay(anyString(), eq("token_dummy"), eq(10000)))
                .thenThrow(new SmbcPaymentException("INSUFFICIENT_BALANCE", "残高が不足しています"));
        when(dellClient.release(anyString(), eq(RESERVATION_ID))).thenReturn(true);

        Order order = orderService.placeOrder(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(dellClient, times(1)).release(anyString(), eq(RESERVATION_ID));
        verify(dellClient, never()).confirm(anyString(), anyLong());
    }

    @Test
    void 決済に失敗し在庫解放も3回とも失敗したらCOMPENSATION_FAILEDになる() {
        when(dellClient.reserve(anyString(), eq(PRODUCT_ID), eq(1))).thenReturn(RESERVATION_ID);
        when(smbcClient.pay(anyString(), eq("token_dummy"), eq(10000)))
                .thenThrow(new SmbcPaymentException("INSUFFICIENT_BALANCE", "残高が不足しています"));
        when(dellClient.release(anyString(), eq(RESERVATION_ID))).thenReturn(false);

        Order order = orderService.placeOrder(request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPENSATION_FAILED);
        verify(dellClient, times(3)).release(anyString(), eq(RESERVATION_ID));
    }

    @Test
    void 存在しない商品IDを指定したら例外を投げてSagaを開始しない() {
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(dellClient, never()).reserve(anyString(), anyString(), anyInt());
        verify(orderRepository, never()).save(any());
    }
}