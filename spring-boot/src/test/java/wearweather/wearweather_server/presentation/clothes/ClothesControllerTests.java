package wearweather.wearweather_server.presentation.clothes;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import wearweather.wearweather_server.application.auth.AuthenticatedUser;
import wearweather.wearweather_server.application.auth.AuthenticationPort;
import wearweather.wearweather_server.application.clothes.ClothesQueryService;
import wearweather.wearweather_server.application.clothes.dto.ClothesResponse;
import wearweather.wearweather_server.domain.clothes.ClothesCategory;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClothesControllerTests {

    @Test
    void getsAuthenticatedUsersClothes() throws Exception {
        AuthenticationPort authenticationPort = mock(AuthenticationPort.class);
        ClothesQueryService clothesQueryService = mock(ClothesQueryService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ClothesController(authenticationPort, clothesQueryService)
        ).build();
        AuthenticatedUser user = new AuthenticatedUser(UUID.randomUUID(), "user@example.com");
        when(authenticationPort.authenticate("Bearer token")).thenReturn(user);
        when(clothesQueryService.getMine(user)).thenReturn(List.of(new ClothesResponse(
                1L, "내 상의", ClothesCategory.TOP, "https://storage.example/1.webp",
                "https://www.musinsa.com/products/1", -5f, 30f
        )));

        mockMvc.perform(get("/clothes").header(HttpHeaders.AUTHORIZATION, "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clothesId").value(1))
                .andExpect(jsonPath("$[0].name").value("내 상의"))
                .andExpect(jsonPath("$[0].category").value("TOP"));

        verify(authenticationPort).authenticate("Bearer token");
        verify(clothesQueryService).getMine(user);
    }
}
