package com.example.amazon.controller;

import com.example.amazon.entity.Order;
import com.example.amazon.entity.User;
import com.example.amazon.repository.OrderRepository;
import com.example.amazon.repository.ProductRepository;
import com.example.amazon.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OrderHistoryControllerのテスト。
 * 「他人の注文が見えてしまう」ことを構造的に防げているかを、
 * 実際にHTTPリクエストを模して確認する(Service層のテストとは違い、
 * 認証・認可という入り口の防御そのものを検証する)。
 */
@WebMvcTest(OrderHistoryController.class)
@Import(com.example.amazon.security.SecurityConfig.class)
class OrderHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void 未ログインでアクセスするとログイン画面へリダイレクトされる() throws Exception {
        mockMvc.perform(get("/account/orders"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void ログイン中ユーザー本人の注文だけがモデルに渡される() throws Exception {
        UserPrincipal principal = principalWithId(1L, "alice");
        Order aliceOrder = new Order("ORD-20260813-0001", 1L, "PRD-000001", 1, 1080);

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(aliceOrder));

        mockMvc.perform(get("/account/orders").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(view().name("order-history"))
                .andExpect(model().attribute("orders", hasSize(1)));

        // 「本人の注文だけ」を取得するメソッドしか呼ばれておらず、
        // 全ユーザー分を取得するメソッド(管理者向け)は一切呼ばれていないことを確認する
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(1L);
        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc(any());
    }

    @Test
    void 別ユーザーでログインすればfindByUserIdに渡るIDも切り替わる() throws Exception {
        UserPrincipal bobPrincipal = principalWithId(2L, "bob");

        when(orderRepository.findByUserIdOrderByCreatedAtDesc(2L))
                .thenReturn(List.of());

        mockMvc.perform(get("/account/orders").with(user(bobPrincipal)))
                .andExpect(status().isOk());

        // bobとしてログインした場合、bobのuserId(2)だけが問い合わせに使われ、
        // 他のユーザーIDは一切参照されないことを確認する
        verify(orderRepository).findByUserIdOrderByCreatedAtDesc(2L);
        verify(orderRepository, never()).findByUserIdOrderByCreatedAtDesc(1L);
    }

    private UserPrincipal principalWithId(Long id, String username) {
        User user = new User(username, "hashed-password");
        ReflectionTestUtils.setField(user, "id", id);
        return new UserPrincipal(user);
    }
}