package com.utem.utem_core.repository;

import com.utem.utem_core.entity.Attachment;
import com.utem.utem_core.entity.TestNode;
import com.utem.utem_core.entity.TestRun;
import com.utem.utem_core.entity.TestStep;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AttachmentRepositoryTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TestStepRepository testStepRepository;

    @Autowired
    private TestNodeRepository testNodeRepository;

    @Autowired
    private TestRunRepository testRunRepository;

    private TestRun testRun;
    private TestNode testNode;
    private TestStep testStep;
    private Attachment screenshot;
    private Attachment logFile;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        testStepRepository.deleteAll();
        testNodeRepository.deleteAll();
        testRunRepository.deleteAll();

        testRun = TestRun.builder()
                .name("Test Suite")
                .startTime(Instant.now())
                .status(TestRun.RunStatus.RUNNING)
                .build();
        testRun = testRunRepository.save(testRun);

        testNode = TestNode.builder()
                .testRun(testRun)
                .nodeType(TestNode.NodeType.SCENARIO)
                .name("Login Scenario")
                .status(TestNode.NodeStatus.FAILED)
                .startTime(Instant.now())
                .build();
        testNode = testNodeRepository.save(testNode);

        testStep = TestStep.builder()
                .testNode(testNode)
                .name("Click login button")
                .status(TestStep.StepStatus.FAILED)
                .timestamp(Instant.now())
                .stepOrder(1)
                .build();
        testStep = testStepRepository.save(testStep);

        screenshot = Attachment.builder()
                .testNode(testNode)
                .testStep(testStep)
                .name("failure_screenshot.png")
                .type(Attachment.AttachmentType.SCREENSHOT)
                .filePath("/attachments/run1/failure_screenshot.png")
                .mimeType("image/png")
                .fileSize(102400L)
                .timestamp(Instant.now())
                .isFailureScreenshot(true)
                .build();

        logFile = Attachment.builder()
                .testNode(testNode)
                .name("console.log")
                .type(Attachment.AttachmentType.LOG)
                .filePath("/attachments/run1/console.log")
                .mimeType("text/plain")
                .fileSize(2048L)
                .timestamp(Instant.now())
                .isFailureScreenshot(false)
                .build();
    }

    @Test
    @DisplayName("Should save and retrieve attachment by ID")
    void shouldSaveAndFindById() {
        Attachment saved = attachmentRepository.save(screenshot);

        Optional<Attachment> found = attachmentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("failure_screenshot.png");
        assertThat(found.get().getType()).isEqualTo(Attachment.AttachmentType.SCREENSHOT);
    }

    @Test
    @DisplayName("Should find attachments by test node ID")
    void shouldFindByTestNodeId() {
        attachmentRepository.save(screenshot);
        attachmentRepository.save(logFile);

        List<Attachment> attachments = attachmentRepository.findByTestNodeId(testNode.getId());

        assertThat(attachments).hasSize(2);
    }

    @Test
    @DisplayName("Should find attachments by test step ID")
    void shouldFindByTestStepId() {
        attachmentRepository.save(screenshot);
        attachmentRepository.save(logFile); // This one has no step

        List<Attachment> attachments = attachmentRepository.findByTestStepId(testStep.getId());

        assertThat(attachments).hasSize(1);
        assertThat(attachments.get(0).getName()).isEqualTo("failure_screenshot.png");
    }

    @Test
    @DisplayName("Should find attachments by type")
    void shouldFindByType() {
        attachmentRepository.save(screenshot);
        attachmentRepository.save(logFile);

        List<Attachment> screenshots = attachmentRepository.findByType(Attachment.AttachmentType.SCREENSHOT);
        List<Attachment> logs = attachmentRepository.findByType(Attachment.AttachmentType.LOG);

        assertThat(screenshots).hasSize(1);
        assertThat(logs).hasSize(1);
    }

    @Test
    @DisplayName("Should find failure screenshots")
    void shouldFindFailureScreenshots() {
        attachmentRepository.save(screenshot);
        attachmentRepository.save(logFile);

        Attachment regularScreenshot = Attachment.builder()
                .testNode(testNode)
                .name("regular_screenshot.png")
                .type(Attachment.AttachmentType.SCREENSHOT)
                .filePath("/attachments/run1/regular_screenshot.png")
                .timestamp(Instant.now())
                .isFailureScreenshot(false)
                .build();
        attachmentRepository.save(regularScreenshot);

        List<Attachment> failureScreenshots = attachmentRepository.findByIsFailureScreenshotTrue();

        assertThat(failureScreenshots).hasSize(1);
        assertThat(failureScreenshots.get(0).getName()).isEqualTo("failure_screenshot.png");
    }

    @Test
    @DisplayName("Should find attachments by node ID and type")
    void shouldFindByNodeIdAndType() {
        attachmentRepository.save(screenshot);
        attachmentRepository.save(logFile);

        List<Attachment> screenshots = attachmentRepository.findByTestNodeIdAndType(
                testNode.getId(),
                Attachment.AttachmentType.SCREENSHOT
        );

        assertThat(screenshots).hasSize(1);
        assertThat(screenshots.get(0).getName()).isEqualTo("failure_screenshot.png");
    }

    @Test
    @DisplayName("Should save attachment with all properties")
    void shouldSaveWithAllProperties() {
        Attachment saved = attachmentRepository.save(screenshot);

        Optional<Attachment> found = attachmentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMimeType()).isEqualTo("image/png");
        assertThat(found.get().getFileSize()).isEqualTo(102400L);
        assertThat(found.get().getFilePath()).contains("failure_screenshot.png");
        assertThat(found.get().getIsFailureScreenshot()).isTrue();
    }

    @Test
    @DisplayName("Should save video attachment")
    void shouldSaveVideoAttachment() {
        Attachment video = Attachment.builder()
                .testNode(testNode)
                .name("test_recording.mp4")
                .type(Attachment.AttachmentType.VIDEO)
                .filePath("/attachments/run1/test_recording.mp4")
                .mimeType("video/mp4")
                .fileSize(10485760L)
                .timestamp(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(video);

        Optional<Attachment> found = attachmentRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getType()).isEqualTo(Attachment.AttachmentType.VIDEO);
    }

    @Test
    @DisplayName("Should delete attachment")
    void shouldDeleteAttachment() {
        Attachment saved = attachmentRepository.save(screenshot);
        String id = saved.getId();

        attachmentRepository.deleteById(id);

        Optional<Attachment> deleted = attachmentRepository.findById(id);
        assertThat(deleted).isEmpty();
    }
}
