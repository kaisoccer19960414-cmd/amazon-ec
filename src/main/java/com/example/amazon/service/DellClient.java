package com.example.amazon.service;

import com.example.amazon.dto.external.DellReservationActionRequest;
import com.example.amazon.dto.external.DellReservationResponse;
import com.example.amazon.dto.external.DellReserveRequest;
import com.example.amazon.dto.external.ExternalErrorResponse;
import com.example.amazon.exception.DellReservationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class DellClient {

    private final WebClient dellWebClient;

    /**
     * 在庫仮確保を依頼する。失敗時(在庫不足・商品なし・通信エラー等)は
     * すべてDellReservationExceptionにまとめて変換する。
     */
    public Long reserve(String orderId, String productId, int quantity) {
        try {
            DellReservationResponse response = dellWebClient.post()
                    .uri("/inventory/reserve")
                    .bodyValue(new DellReserveRequest(orderId, productId, quantity))
                    .retrieve()
                    .bodyToMono(DellReservationResponse.class)
                    .block();

            return response != null ? response.reservationId() : null;

        } catch (WebClientResponseException e) {
            ExternalErrorResponse error = e.getResponseBodyAs(ExternalErrorResponse.class);
            String errorCode = error != null ? error.errorCode() : "UNKNOWN_ERROR";
            String message = error != null ? error.message() : "Dellとの通信でエラーが発生しました";
            throw new DellReservationException(errorCode, message);

        } catch (Exception e) {
            // タイムアウトや接続失敗など、Dellからの応答自体が得られなかった場合
            throw new DellReservationException("COMMUNICATION_ERROR", "Dellとの通信に失敗しました");
        }
    }

    /**
     * 在庫確定を依頼する。決済成功後に呼ばれる。
     */
    public void confirm(String orderId, Long reservationId) {
        try {
            dellWebClient.post()
                    .uri("/inventory/confirm")
                    .bodyValue(new DellReservationActionRequest(orderId, reservationId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            // confirm失敗は今回のスコープでは致命的エラーとして扱う(要手動対応)
            throw new DellReservationException("CONFIRM_FAILED", "在庫確定に失敗しました");
        }
    }

    /**
     * 在庫解放を依頼する(補償処理)。成功/失敗をbooleanで返す。
     * 呼び出し側(OrderService)でリトライ制御を行うため、例外は投げずbooleanで結果を返す。
     */
    public boolean release(String orderId, Long reservationId) {
        try {
            dellWebClient.post()
                    .uri("/inventory/release")
                    .bodyValue(new DellReservationActionRequest(orderId, reservationId))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}