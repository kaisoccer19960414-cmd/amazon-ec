package com.example.amazon.service;

import com.example.amazon.dto.request.OrderRequest;
import com.example.amazon.entity.Order;
import com.example.amazon.entity.Product;
import com.example.amazon.exception.DellReservationException;
import com.example.amazon.exception.SmbcPaymentException;
import com.example.amazon.repository.OrderRepository;
import com.example.amazon.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int MAX_RELEASE_RETRY = 3;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final DellClient dellClient;
    private final SmbcClient smbcClient;

    /**
     * 注文を開始し、Sagaを最後まで遂行する。
     * このメソッド自体は@Transactionalにしない(外部API呼び出しを跨ぐ処理を
     * 1つのDBトランザクションに収めることはできないため)。各状態変更は
     * 都度saveすることで、途中経過も含めてorderテーブルに残す。
     */
    public Order placeOrder(OrderRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

        Order order = new Order(generateOrderId(), request.getUserId(), request.getProductId(), request.getQuantity());
        orderRepository.save(order);

        // ---- 1. 在庫確保 ----
        Long reservationId;
        try {
            reservationId = dellClient.reserve(order.getId(), order.getProductId(), order.getQuantity());
        } catch (DellReservationException e) {
            order.markStockFailed();
            orderRepository.save(order);
            return order;
        }

        order.markStockReserved(reservationId);
        orderRepository.save(order);

        // ---- 2. 決済 ----
        Long transactionId;
        try {
            transactionId = smbcClient.pay(order.getId(), request.getToken(), product.getPrice() * order.getQuantity());
        } catch (SmbcPaymentException e) {
            return compensate(order);
        }

        // ---- 3. 在庫確定 ----
        // 決済成功後のconfirm失敗は、状態遷移図では明示的にカバーしていない
        // レアケース(決済は成功済みなので在庫を取り消すのは不適切)。
        // 学習用のこの実装では、COMPLETEDのまま確定させ、ログに残すに留める。
        try {
            dellClient.confirm(order.getId(), reservationId);
        } catch (DellReservationException e) {
            // 実務であればアラート通知や手動対応キューへの登録が必要な箇所
            System.err.println("WARN: confirm failed for orderId=" + order.getId());
        }

        order.markCompleted(transactionId);
        orderRepository.save(order);
        return order;
    }

    /**
     * 決済失敗時の補償処理。在庫を解放し、成功すればCANCELLED、
     * 一定回数リトライしても失敗すればCOMPENSATION_FAILEDとする。
     */
    private Order compensate(Order order) {
        order.markCompensating();
        orderRepository.save(order);

        for (int attempt = 1; attempt <= MAX_RELEASE_RETRY; attempt++) {
            boolean released = dellClient.release(order.getId(), order.getReservationId());
            if (released) {
                order.markCancelled();
                orderRepository.save(order);
                return order;
            }
        }

        order.markCompensationFailed();
        orderRepository.save(order);
        return order;
    }

    /**
     * ORD-yyyyMMdd-連番4桁 の形式で相関IDを採番する。
     */
    private String generateOrderId() {
        String datePart = LocalDate.now().format(DATE_FORMAT);
        String prefix = "ORD-" + datePart + "-";
        long countToday = orderRepository.countByIdStartingWith(prefix);
        long nextSeq = countToday + 1;
        return prefix + String.format("%04d", nextSeq);
    }
}
