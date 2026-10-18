package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.dto.EvidenceAttachmentCompleteRequest;
import com.complex.platform.evidence.dto.EvidenceAttachmentUploadIntentRequest;
import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FinanceRefundEvidenceFileTests {
    @TempDir Path directory;
    private final Map<String, EvidenceAttachmentRecord> attachments = new LinkedHashMap<>();
    private EvidenceEventService evidence;
    private EvidenceAttachmentValidator validator;
    private EvidenceFileStorage storage;

    @BeforeEach
    void setUp() {
        EvidenceEventRepository repository = mock(EvidenceEventRepository.class);
        when(repository.saveAttachment(any())).thenAnswer(i -> {
            EvidenceAttachmentRecord record = i.getArgument(0);
            attachments.put(record.id(), record);
            return record;
        });
        when(repository.findAttachmentById(anyString())).thenAnswer(i -> Optional.ofNullable(attachments.get(i.getArgument(0))));
        AuthorizationService authorization = mock(AuthorizationService.class);
        when(authorization.currentMerchantScope()).thenReturn(Optional.of("merchant"));
        when(authorization.currentActor()).thenReturn(new AccessActor("operator", "operator", "staff", "org", "merchant", "active"));
        storage = new EvidenceFileStorage(directory.toString(), 1024);
        evidence = new EvidenceEventService(repository, new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(new InMemoryAuditEventRepository()), authorization);
        evidence.fileStorage = storage;
        validator = new EvidenceAttachmentValidator(repository, storage);
    }

    @Test
    void pendingThenClientCompletionWithoutFileCannotBecomeRefundProof() {
        EvidenceAttachmentRecord intent = evidence.createAttachmentUploadIntent(
                new EvidenceAttachmentUploadIntentRequest("proof-intent", "凭证.png", "image/png", 4L, "finance_refund", "refund"));
        EvidenceAttachmentRecord completed = evidence.completeAttachment(intent.id(),
                new EvidenceAttachmentCompleteRequest("proof-complete", "ignored-client-object", "sha256:" + "a".repeat(64), 4L));
        assertThat(completed.status()).isEqualTo("uploaded");
        assertThatThrownBy(() -> validator.requireRefundEvidence(List.of(completed.id()), "refund", "merchant"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void actualMultipartBytesCanBeUsedAsRefundProof() throws Exception {
        byte[] bytes = {1, 2, 3, 4};
        EvidenceAttachmentRecord uploaded = evidence.uploadAttachment(
                new MockMultipartFile("file", "凭证.png", "image/png", bytes), "finance_refund", "refund", "proof");
        assertThat(Files.readAllBytes(storage.resolveForRead(uploaded.objectKey()))).containsExactly(bytes);
        validator.requireRefundEvidence(List.of(uploaded.id()), "refund", "merchant");
    }

    @Test
    void uploadedMetadataCannotHideMissingLocalObject() throws Exception {
        EvidenceAttachmentRecord uploaded = evidence.uploadAttachment(
                new MockMultipartFile("file", "凭证.png", "image/png", new byte[]{1, 2, 3, 4}), "finance_refund", "refund", "proof");
        Files.delete(storage.resolveForRead(uploaded.objectKey()));
        assertThatThrownBy(() -> validator.requireRefundEvidence(List.of(uploaded.id()), "refund", "merchant"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void uploadedMetadataCannotHideChangedFileLength() throws Exception {
        EvidenceAttachmentRecord uploaded = evidence.uploadAttachment(
                new MockMultipartFile("file", "凭证.png", "image/png", new byte[]{1, 2, 3, 4}), "finance_refund", "refund", "proof");
        Files.write(storage.resolveForRead(uploaded.objectKey()), new byte[]{1});
        assertThatThrownBy(() -> validator.requireRefundEvidence(List.of(uploaded.id()), "refund", "merchant"))
                .isInstanceOf(ApiException.class);
    }
}
