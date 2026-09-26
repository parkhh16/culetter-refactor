package com.sim.backend.global.exception;

import com.sim.backend.domain.nft.controller.NftController;
import com.sim.backend.domain.nft.repository.NftRepository;
import com.sim.backend.domain.nft.service.NftService;
import com.sim.backend.domain.users.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * #4 GlobalExceptionHandler 회귀 테스트.
 * 컨트롤러가 더 이상 catch(Exception e) { throw new RuntimeException(e); }로 직접 감싸지 않아도,
 * 전역 핸들러가 동일하게 500 + 에러 메시지를 응답하는지 확인한다.
 */
@WebMvcTest(NftController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private NftService nftService;
    @MockBean
    private NftRepository nftRepository;
    @MockBean
    private FirebaseAuth firebaseAuth;
    @MockBean
    private UserRepository userRepository;

    @Test
    void 서비스에서_예외가_나면_500과_메시지를_응답한다() throws Exception {
        when(nftService.getSentTokens(any())).thenThrow(new RuntimeException("simulated failure"));

        mockMvc.perform(get("/api/v1/nft/sentTokens").header("Authorization", "Bearer test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("simulated failure"));
    }
}
