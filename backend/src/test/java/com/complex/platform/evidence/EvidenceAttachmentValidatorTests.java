package com.complex.platform.evidence;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.common.api.ApiException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class EvidenceAttachmentValidatorTests {
    @TempDir Path directory;
    private EvidenceEventRepository repository;
    private EvidenceFileStorage storage;
    private EvidenceAttachmentValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(EvidenceEventRepository.class);
        storage = new EvidenceFileStorage(directory.toString(), 1024);
        validator = new EvidenceAttachmentValidator(repository, storage);
    }

    @Test
    void pendingMetadataCannotBePhotoEvidence() {
        attachment("pending", "evidence-files/missing.png", 4L, "image/png");
        assertThatThrownBy(() -> validator.requirePhotos(List.of("attachment"), "photos"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void completedMetadataWithoutFileCannotBeEvidence() {
        attachment("uploaded", "evidence-files/missing.png", 4L, "image/png");
        assertThatThrownBy(() -> validator.requireExisting(List.of("attachment"), "evidence"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void actualPhotoAndVideoFilesPassButDeletedObjectCannotBeReused() throws Exception {
        var photo = storage.store(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3, 4}));
        attachment("uploaded", photo.objectKey(), photo.fileSize(), photo.contentType());
        validator.requirePhotos(List.of("attachment"), "photos");
        Files.delete(storage.resolveForRead(photo.objectKey()));
        assertThatThrownBy(() -> validator.requirePhotos(List.of("attachment"), "photos"))
                .isInstanceOf(ApiException.class);

        var video = storage.store(new MockMultipartFile("file", "video.mp4", "video/mp4", new byte[]{5, 6, 7}));
        attachment("uploaded", video.objectKey(), video.fileSize(), video.contentType());
        validator.requireVideos(List.of("attachment"), "videos");
    }

    @Test
    void changedObjectSizeCannotPassReview() throws Exception {
        var photo = storage.store(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3, 4}));
        attachment("uploaded", photo.objectKey(), photo.fileSize(), photo.contentType());
        Files.write(storage.resolveForRead(photo.objectKey()), new byte[]{1});
        assertThatThrownBy(() -> validator.requirePhotos(List.of("attachment"), "photos"))
                .isInstanceOf(ApiException.class);
    }

    private void attachment(String status, String objectKey, long size, String type) {
        when(repository.findAttachmentById("attachment")).thenReturn(Optional.of(
                new EvidenceAttachmentRecord("attachment", "proof", type, size, "saved_bottle_void",
                        "bottle", "proof", objectKey, null, status, null, Instant.now(), Instant.now(), null)));
    }
}
