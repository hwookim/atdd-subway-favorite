package wooteco.subway.web.member;

import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import wooteco.subway.docs.LoginMemberDocumentation;
import wooteco.subway.domain.member.Member;
import wooteco.subway.service.member.MemberService;
import wooteco.subway.service.member.dto.LoginRequest;
import wooteco.subway.service.member.dto.MemberRequest;
import wooteco.subway.service.member.dto.MemberResponse;
import wooteco.subway.service.member.dto.UpdateMemberRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
class LoginMemberControllerTest {
    private static final Gson GSON = new Gson();
    private static final String TEST_USER_EMAIL = "test@email.com";
    private static final String TEST_USER_NAME = "타미";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String TOKEN = "This.is.token";

    private Member member;

    @MockBean
    private MemberService memberService;

    @MockBean
    private BearerAuthInterceptor bearerAuthInterceptor;

    @MockBean
    private LoginMemberMethodArgumentResolver resolver;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        this.member = new Member(1L, TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(new ShallowEtagHeaderFilter())
                .apply(documentationConfiguration(restDocumentation))
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .alwaysDo(print())
                .build();
    }

    @Test
    void login() throws Exception {
        LoginRequest request = new LoginRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD);

        given(memberService.createToken(any())).willReturn(TOKEN);

        mockMvc.perform(post("/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(GSON.toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is(TOKEN)))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andDo(LoginMemberDocumentation.login());
    }

    @Test
    void createMember() throws Exception {
        MemberRequest request = new MemberRequest(TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);

        given(memberService.createMember(any())).willReturn(member);
        mockMvc.perform(post("/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GSON.toJson(request)))
                .andExpect(status().isNoContent())
                .andDo(LoginMemberDocumentation.createMember());
    }

    @Test
    void getMember() throws Exception {
        given(bearerAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(member);

        MvcResult result = mockMvc.perform(get("/me")
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isOk())
                .andDo(LoginMemberDocumentation.getMember())
                .andReturn();

        MemberResponse expected = MemberResponse.of(member);

        assertThat(result.getResponse().getContentAsString())
                .isEqualTo(GSON.toJson(expected));
    }

    @Test
    void updateMember() throws Exception {
        UpdateMemberRequest request = new UpdateMemberRequest("NEW_" + TEST_USER_NAME, "NEW_" + TEST_USER_PASSWORD);

        given(bearerAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(member);

        mockMvc.perform(put("/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(GSON.toJson(request))
                .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent())
                .andDo(LoginMemberDocumentation.updateMember());
    }

    @Test
    void deleteMember() throws Exception {
        given(bearerAuthInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(resolver.supportsParameter(any())).willReturn(true);
        given(resolver.resolveArgument(any(), any(), any(), any())).willReturn(member);

        mockMvc.perform(delete("/me")
                .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isNoContent())
                .andDo(LoginMemberDocumentation.deleteMember());
    }
}