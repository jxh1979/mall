package com.complex.platform.access.staff;

// 文件版本：V0.03
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = { "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex", "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}" })
@AutoConfigureMockMvc
@Transactional
class M010RegistrationHttpTests {
    @Autowired MockMvc mvc;

    @Test void unregisteredEmployeeCanSubmitButReceivesOnlyPendingReceipt() throws Exception {
        // U01-001/U05-003/U11-007：私人手机通道以 staff_private_phone 声明，并携带安装时生成的设备公钥/指纹；无证明 → 400。
        String noProof = "{\"idempotency_key\":\""+UUID.randomUUID()+"\",\"company_id\":\"be_proj_changsha\",\"name\":\"自助申请\",\"phone\":\"13800138000\"}";
        mvc.perform(post("/api/v1/staff/self-registrations").header("X-Terminal-Type", "staff_private_phone").contentType(MediaType.APPLICATION_JSON).content(noProof))
                .andExpect(status().isBadRequest());
        String[] proof = StaffTestActors.deviceProof();
        String body = "{\"idempotency_key\":\""+UUID.randomUUID()+"\",\"company_id\":\"be_proj_changsha\",\"name\":\"自助申请\",\"phone\":\"13800138000\","
                + "\"device_public_key\":\""+proof[0]+"\",\"device_fingerprint_hash\":\""+proof[1]+"\"}";
        mvc.perform(post("/api/v1/staff/self-registrations").header("X-Terminal-Type", "staff_private_phone").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.registration_status").value("submitted"))
                .andExpect(jsonPath("$.data.id").isString()).andExpect(jsonPath("$.data.phone").doesNotExist())
                .andExpect(jsonPath("$.data.user_id").doesNotExist());
        mvc.perform(get("/api/v1/staff/registration-companies"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].name").isString());
    }

    @Test void openingSelfRegistrationDoesNotOpenManagementOrApproval() throws Exception {
        mvc.perform(get("/api/v1/staff/profiles")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/staff/registrations/unknown/review").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"test\",\"decision\":\"approve\"}"))
                .andExpect(status().isUnauthorized());
    }
}
