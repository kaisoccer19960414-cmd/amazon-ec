package com.example.amazon.service;

import com.example.amazon.dto.external.ExternalErrorResponse;
import com.example.amazon.dto.external.SmbcPayRequest;
import com.example.amazon.dto.external.SmbcPaymentResponse;
import com.example.amazon.dto.external.SmbcTokenRequest;
import com.example.amazon.dto.external.SmbcTokenResponse;
import com.example.amazon.exception.SmbcPaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
public class SmbcClient {

    private final WebClient smbcWebClient;

    /**
     * カード情報をトークンに変換する。生のカード情報はAmazon側では保持せず、
     * ここで得たトークンだけをUser.smbcTokenとして保存する。
     */
    public String issueToken(String cardNumber, String expiryMonth, String expiryYear, String cvv) {
        try {
            SmbcTokenResponse response = smbcWebClient.post()
                    .uri("/payment/tokens")
                    .bodyValue(new SmbcTokenRequest(cardNumber, expiryMonth, expiryYear, cvv))
                    .retrieve()
                    .bodyToMono(SmbcTokenResponse.class)
                    .block();

            return response != null ? response.token() : null;

        } catch (WebClientResponseException e) {
            ExternalErrorResponse error = e.getResponseBodyAs(ExternalErrorResponse.class);
            String errorCode = error != null ? error.errorCode() : "UNKNOWN_ERROR";
            String message = error != null ? error.message() : "SMBCとの通信でエラーが発生しました";
            throw new SmbcPaymentException(errorCode, message);

        } catch (Exception e) {
            throw new SmbcPaymentException("COMMUNICATION_ERROR", "SMBCとの通信に失敗しました");
        }
    }

    /**
     * 決済を依頼する。失敗時(残高不足・トークン不正・通信エラー等)は
     * すべてSmbcPaymentExceptionにまとめて変換する。
     */
    public Long pay(String orderId, String token, int amount) {
        try {
            SmbcPaymentResponse response = smbcWebClient.post()
                    .uri("/payment/pay")
                    .bodyValue(new SmbcPayRequest(orderId, token, amount))
                    .retrieve()
                    .bodyToMono(SmbcPaymentResponse.class)
                    .block();

            return response != null ? response.transactionId() : null;

        } catch (WebClientResponseException e) {
            ExternalErrorResponse error = e.getResponseBodyAs(ExternalErrorResponse.class);
            String errorCode = error != null ? error.errorCode() : "UNKNOWN_ERROR";
            String message = error != null ? error.message() : "SMBCとの通信でエラーが発生しました";
            throw new SmbcPaymentException(errorCode, message);

        } catch (Exception e) {
            throw new SmbcPaymentException("COMMUNICATION_ERROR", "SMBCとの通信に失敗しました");
        }
    }
}