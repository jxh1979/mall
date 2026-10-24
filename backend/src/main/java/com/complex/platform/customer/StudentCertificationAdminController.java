package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import java.util.Map;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ROOT-03-R28 后台学生认证审核（票务运营岗，权限 ticket.student_cert.review，平台范围）：BSM 第 777 行「学生票要票务运营岗人工审核证件 + 学信网
 * 核验通过才可用」。权限与范围在服务层逐个校验。
 */
@RestController
@RequestMapping("/api/v1/student-certifications")
public class StudentCertificationAdminController {

    private final StudentCertificationService service;

    public StudentCertificationAdminController(StudentCertificationService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<StudentCertificationService.ReviewItem>> list(
            @RequestParam(name = "status", required = false) String status) {
        return ApiResponse.ok(service.list(status));
    }

    @PostMapping("/{certification_id}/approve")
    public ApiResponse<StudentCertificationService.ReviewItem> approve(@PathVariable("certification_id") String certificationId,
            @RequestBody StudentCertificationService.ApproveRequest request) {
        return ApiResponse.ok(service.approve(certificationId, request));
    }

    @PostMapping("/{certification_id}/reject")
    public ApiResponse<StudentCertificationService.ReviewItem> reject(@PathVariable("certification_id") String certificationId,
            @RequestBody StudentCertificationService.RejectRequest request) {
        return ApiResponse.ok(service.reject(certificationId, request));
    }

    /** 受控回读第 index 张学生证件照片（从 1 起），供审核人核对证件。 */
    @GetMapping("/{certification_id}/files/{index}")
    public ResponseEntity<Resource> readFile(@PathVariable("certification_id") String certificationId,
            @PathVariable("index") int index) {
        Map.Entry<byte[], String> file = service.readFile(certificationId, index);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.getValue())).body(new ByteArrayResource(file.getKey()));
    }
}
