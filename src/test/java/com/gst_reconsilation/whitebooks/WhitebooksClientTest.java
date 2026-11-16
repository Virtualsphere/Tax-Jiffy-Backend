package com.gst_reconsilation.whitebooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WhitebooksClientTest {

    private static final String URL = "https://wb.test/gstr1/b2b";

    private MockRestServiceServer server;
    private WhitebooksClient client;

    @BeforeEach
    void setUp() {
        RestTemplate rt = new RestTemplate();
        server = MockRestServiceServer.bindTo(rt).build();
        client = new WhitebooksClient(rt, new ObjectMapper());
    }

    private void respond(String json) {
        server.expect(requestTo(URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void unwrapsDataOnSuccess() {
        respond("{\"status_cd\":\"1\",\"data\":{\"b2b\":[{\"ctin\":\"X\"}]}}");
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> data = client.getData(URL, new HttpHeaders(), Map.class);
        assertThat(data.get("b2b").get(0).get("ctin")).isEqualTo("X");
    }

    @Test
    void http200WithStatusZeroIsAnError_usesGstnErrorMessage() {
        // Real body captured from the sandbox (AUTH002)
        respond("{\"error\":{\"errorCode\":\"AUTH002\",\"errorMessage\":\"GSTIN has disabled API access\"},"
                + "\"status_cd\":\"0\",\"status_desc\":\"user name does not exists\"}");
        assertThatThrownBy(() -> client.get(URL, new HttpHeaders()))
                .isInstanceOf(WhitebooksApiException.class)
                .hasMessage("GSTIN has disabled API access")
                .extracting("errorCode").isEqualTo("AUTH002");
    }

    @Test
    void whitebooksLayerErrorUsesStatusDesc() {
        respond("{\"status_cd\":\"0\",\"status_desc\":\"WB_ERR_9001 : Invalid credentials provided\"}");
        assertThatThrownBy(() -> client.get(URL, new HttpHeaders()))
                .isInstanceOf(WhitebooksApiException.class)
                .hasMessageContaining("WB_ERR_9001");
    }

    @Test
    void ewayBillErrorArrayWithBareCodeFallsBackToCode() {
        // Real e-way bill error shape: message is just the code
        respond("{\"error\":[{\"errorCode\":\"601\",\"errorMessage\":\"601\"}],\"irp\":\"NIC1\",\"status_cd\":\"0\"}");
        assertThatThrownBy(() -> client.get(URL, new HttpHeaders()))
                .isInstanceOf(WhitebooksApiException.class)
                .hasMessage("Error 601");
    }

    @Test
    void ewayBillArrayDataIsUnwrapped() {
        respond("{\"irp\":\"NIC1\",\"data\":[{\"ewbNo\":181012980811}],\"status_cd\":\"1\"}");
        JsonNode root = client.get(URL, new HttpHeaders());
        JsonNode[] rows = client.dataAs(root, JsonNode[].class);
        assertThat(rows).hasSize(1);
        assertThat(rows[0].get("ewbNo").asLong()).isEqualTo(181012980811L);
    }

    @Test
    void noDataDetection() {
        assertThat(WhitebooksApiException.isNoData(new WhitebooksApiException("RET", "No Invoices found for the provided Inputs"))).isTrue();
        assertThat(WhitebooksApiException.isNoData(new WhitebooksApiException("AUTH002", "GSTIN has disabled API access"))).isFalse();
    }
}
