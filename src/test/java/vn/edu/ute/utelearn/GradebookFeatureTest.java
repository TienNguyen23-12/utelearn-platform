package vn.edu.ute.utelearn;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.ute.utelearn.dao.*;
import vn.edu.ute.utelearn.dto.GradebookDTO;
import vn.edu.ute.utelearn.entity.*;
import vn.edu.ute.utelearn.service.impl.CohortServiceImpl;
import vn.edu.ute.utelearn.service.impl.GradebookServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.ute.utelearn.controller.instructor.InstructorGradebookController;
import vn.edu.ute.utelearn.dto.GradebookImportResultDTO;
import vn.edu.ute.utelearn.service.AuthService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class GradebookFeatureTest {

    @Mock
    private CohortRepository cohortRepository;

    @Mock
    private CohortMemberRepository cohortMemberRepository;

    @Mock
    private CohortScheduleRepository cohortScheduleRepository;

    @Mock
    private LessonCompletionRepository lessonCompletionRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuizSubmissionRepository quizSubmissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GradebookServiceImpl gradebookService;

    @InjectMocks
    private CohortServiceImpl cohortService;

    private MockMvc mockMvc;
    private User sampleInstructor;
    private User sampleStudent;
    private Cohort sampleCohort;
    private Lesson finalLesson;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InstructorGradebookController(gradebookService, authService)).build();

        sampleInstructor = User.builder()
                .id(1L)
                .email("instructor@utelearn.edu.vn")
                .fullName("Thầy Giảng Viên")
                .build();

        sampleStudent = User.builder()
                .id(2L)
                .email("student@utelearn.edu.vn")
                .fullName("Nguyễn Văn Học Viên")
                .build();

        finalLesson = Lesson.builder()
                .id(100L)
                .title("Bài thi Cuối Kỳ")
                .lessonType("QUIZ")
                .build();

        sampleCohort = Cohort.builder()
                .id(10L)
                .code("MOD-TEST-01")
                .name("Lập trình Web nâng cao")
                .createdBy(sampleInstructor)
                .gradingPolicy("WEIGHTED_GRADING")
                .processWeight(40)
                .finalWeight(60)
                .finalLesson(finalLesson)
                .minPassingGrade(new BigDecimal("5.00"))
                .minFinalExamScore(new BigDecimal("3.00"))
                .isGradesPublished(false)
                .build();
    }

    @Test
    void testCalculateGrades_WeightedGrading_StudentPassed() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .isPassed(false)
                .build();

        Lesson processLesson1 = Lesson.builder().id(101L).title("Quiz 1").lessonType("QUIZ").build();
        Quiz quizProcess = Quiz.builder().id(201L).lesson(processLesson1).build();
        Quiz finalQuiz = Quiz.builder().id(200L).lesson(finalLesson).build();

        QuizSubmission processSub = QuizSubmission.builder()
                .id(301L)
                .quiz(quizProcess)
                .score(new BigDecimal("7.00"))
                .build();

        QuizSubmission finalSub = QuizSubmission.builder()
                .id(300L)
                .quiz(finalQuiz)
                .score(new BigDecimal("8.00"))
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 100L));

        when(quizRepository.findByLessonId(101L)).thenReturn(Optional.of(quizProcess));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(201L, 2L, 10L))
                .thenReturn(Optional.of(processSub));

        when(quizRepository.findByLessonId(100L)).thenReturn(Optional.of(finalQuiz));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(200L, 2L, 10L))
                .thenReturn(Optional.of(finalSub));

        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(inv -> inv.getArgument(0));

        gradebookService.calculateAndSaveGrades(10L, 1L);

        assertEquals(new BigDecimal("7.00"), member.getProcessGrade());
        assertEquals(new BigDecimal("8.00"), member.getFinalExamGrade());
        assertEquals(new BigDecimal("7.60"), member.getFinalGrade());
        assertTrue(member.getIsPassed());
        assertNotNull(member.getCertificateUrl());
        assertTrue(member.getCertificateUrl().contains("UTE-CERT-MOD-TEST-01-2"));
    }

    @Test
    void testCalculateGrades_WeightedGrading_BelowMinFinalExam_ShouldFail() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .isPassed(false)
                .build();

        Lesson processLesson1 = Lesson.builder().id(101L).title("Quiz 1").lessonType("QUIZ").build();
        Quiz quizProcess = Quiz.builder().id(201L).lesson(processLesson1).build();
        Quiz finalQuiz = Quiz.builder().id(200L).lesson(finalLesson).build();

        QuizSubmission processSub = QuizSubmission.builder()
                .id(301L)
                .quiz(quizProcess)
                .score(new BigDecimal("10.00"))
                .build();

        QuizSubmission finalSub = QuizSubmission.builder()
                .id(300L)
                .quiz(finalQuiz)
                .score(new BigDecimal("2.00"))
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 100L));

        when(quizRepository.findByLessonId(101L)).thenReturn(Optional.of(quizProcess));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(201L, 2L, 10L))
                .thenReturn(Optional.of(processSub));

        when(quizRepository.findByLessonId(100L)).thenReturn(Optional.of(finalQuiz));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(200L, 2L, 10L))
                .thenReturn(Optional.of(finalSub));

        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(inv -> inv.getArgument(0));

        gradebookService.calculateAndSaveGrades(10L, 1L);

        assertEquals(new BigDecimal("5.20"), member.getFinalGrade());
        assertFalse(member.getIsPassed());
    }

    @Test
    void testCalculateGrades_ProgressBased_AllCompleted_ShouldPass() {
        sampleCohort.setGradingPolicy("PROGRESS_BASED");
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .isPassed(false)
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(1L, 2L, 3L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(3L);
        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(inv -> inv.getArgument(0));

        gradebookService.calculateAndSaveGrades(10L, 1L);

        assertTrue(member.getIsPassed());
        assertEquals(new BigDecimal("10.00"), member.getFinalGrade());
    }

    @Test
    void testPublishGrades_Success() {
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));

        gradebookService.publishGrades(10L, 1L);

        assertTrue(sampleCohort.getIsGradesPublished());
        verify(cohortRepository).save(sampleCohort);
    }

    @Test
    void testGetGradebook_ReturnsSummaryAndList() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .finalGrade(new BigDecimal("8.50"))
                .isPassed(true)
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(1L, 2L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(2L);

        GradebookDTO dto = gradebookService.getGradebook(10L, 1L);

        assertNotNull(dto);
        assertEquals(1, dto.getTotalStudents());
        assertEquals(1, dto.getPassedCount());
        assertEquals(0, dto.getFailedCount());
        assertEquals(100.0, dto.getPassRate());
        assertEquals(new BigDecimal("8.50"), dto.getAverageGrade());
    }

    @Test
    void testController_ShowGradebook_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of());
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/instructor/cohorts/10/gradebook"))
                .andExpect(status().isOk())
                .andExpect(view().name("instructor/cohorts/gradebook"))
                .andExpect(model().attributeExists("currentUser", "gradebook", "cohort"));
    }

    @Test
    void testController_ShowGradebook_Unauthenticated_RedirectsLogin() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.empty());

        mockMvc.perform(get("/instructor/cohorts/10/gradebook"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void testController_ShowGradebook_Error_RedirectsToCohorts() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/instructor/cohorts/999/gradebook"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void testController_CalculateGrades_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of());
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of());

        mockMvc.perform(post("/instructor/cohorts/10/gradebook/calculate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts/10/gradebook"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testController_PublishGrades_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));

        mockMvc.perform(post("/instructor/cohorts/10/gradebook/publish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts/10/gradebook"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testExportGradebookToExcel_GeneratesValidExcelBytes() throws Exception {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .processGrade(new BigDecimal("8.00"))
                .finalExamGrade(new BigDecimal("9.00"))
                .finalGrade(new BigDecimal("8.60"))
                .isPassed(true)
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 100L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(2L);

        byte[] bytes = gradebookService.exportGradebookToExcel(10L, 1L);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet bangDiemSheet = workbook.getSheet("Bang_Diem");
            Sheet huongDanSheet = workbook.getSheet("Huong_Dan_Nhap_Diem");
            assertNotNull(bangDiemSheet);
            assertNotNull(huongDanSheet);

            Row headerRow = bangDiemSheet.getRow(5);
            assertNotNull(headerRow);
            assertTrue(headerRow.getCell(1).getStringCellValue().contains("Mã thành viên"));
            assertTrue(headerRow.getCell(2).getStringCellValue().contains("Email"));

            Row dataRow = bangDiemSheet.getRow(6);
            assertNotNull(dataRow);
            assertEquals("50", dataRow.getCell(1).getStringCellValue());
            assertEquals("student@utelearn.edu.vn", dataRow.getCell(2).getStringCellValue());
            assertEquals("Nguyễn Văn Học Viên", dataRow.getCell(3).getStringCellValue());
            assertEquals("8", dataRow.getCell(5).getStringCellValue());
            assertEquals("9", dataRow.getCell(6).getStringCellValue());
            assertEquals("ĐẠT", dataRow.getCell(8).getStringCellValue());
        }
    }

    @Test
    void testImportGradebookFromExcel_ValidExcel_UpdatesGrades() throws Exception {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .isPassed(false)
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 100L));
        when(cohortMemberRepository.save(any(CohortMember.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] excelBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bang_Diem");
            sheet.createRow(0);
            sheet.createRow(1);
            sheet.createRow(2);

            Row headerRow = sheet.createRow(5);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Mã thành viên (*)");
            headerRow.createCell(2).setCellValue("Email học viên (*)");
            headerRow.createCell(3).setCellValue("Họ và tên");
            headerRow.createCell(4).setCellValue("Tiến độ (%)");
            headerRow.createCell(5).setCellValue("Điểm Quá trình (*)");
            headerRow.createCell(6).setCellValue("Điểm Cuối kỳ (*)");

            Row dataRow = sheet.createRow(6);
            dataRow.createCell(0).setCellValue(1);
            dataRow.createCell(1).setCellValue(50);
            dataRow.createCell(2).setCellValue("student@utelearn.edu.vn");
            dataRow.createCell(3).setCellValue("Nguyễn Văn Học Viên");
            dataRow.createCell(4).setCellValue("100%");
            dataRow.createCell(5).setCellValue(8.5);
            dataRow.createCell(6).setCellValue(7.5);

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "grades.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        GradebookImportResultDTO result = gradebookService.importGradebookFromExcel(10L, file, 1L);

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getUpdatedCount());
        assertEquals(0, result.getFailedCount());
        assertTrue(result.getErrors().isEmpty());

        assertEquals(new BigDecimal("8.50"), member.getProcessGrade());
        assertEquals(new BigDecimal("7.50"), member.getFinalExamGrade());
        assertEquals(new BigDecimal("7.90"), member.getFinalGrade());
        assertTrue(member.getIsPassed());
    }

    @Test
    void testImportGradebookFromExcel_InvalidScore_LogsError() throws Exception {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .isPassed(false)
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));

        byte[] excelBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bang_Diem");
            sheet.createRow(0);
            sheet.createRow(1);
            sheet.createRow(2);

            Row headerRow = sheet.createRow(5);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Mã thành viên (*)");
            headerRow.createCell(2).setCellValue("Email học viên (*)");
            headerRow.createCell(3).setCellValue("Họ và tên");
            headerRow.createCell(4).setCellValue("Tiến độ (%)");
            headerRow.createCell(5).setCellValue("Điểm Quá trình (*)");
            headerRow.createCell(6).setCellValue("Điểm Cuối kỳ (*)");

            Row dataRow = sheet.createRow(6);
            dataRow.createCell(0).setCellValue(1);
            dataRow.createCell(1).setCellValue(50);
            dataRow.createCell(2).setCellValue("student@utelearn.edu.vn");
            dataRow.createCell(3).setCellValue("Nguyễn Văn Học Viên");
            dataRow.createCell(4).setCellValue("100%");
            dataRow.createCell(5).setCellValue(15.0);
            dataRow.createCell(6).setCellValue(7.5);

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile("file", "grades.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excelBytes);

        GradebookImportResultDTO result = gradebookService.importGradebookFromExcel(10L, file, 1L);

        assertEquals(1, result.getTotalRows());
        assertEquals(0, result.getUpdatedCount());
        assertEquals(1, result.getFailedCount());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("Điểm quá trình"));
    }

    @Test
    void testController_ExportGradebook_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of());
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/instructor/cohorts/10/gradebook/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"Bang_Diem_Cohort_10.xlsx\"")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void testController_ImportGradebook_Success() throws Exception {
        when(authService.getCurrentAuthenticatedUser()).thenReturn(Optional.of(sampleInstructor));
        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of());

        byte[] emptyExcel;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Bang_Diem");
            workbook.write(out);
            emptyExcel = out.toByteArray();
        }

        MockMultipartFile mockFile = new MockMultipartFile("file", "grades.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", emptyExcel);

        mockMvc.perform(multipart("/instructor/cohorts/10/gradebook/import").file(mockFile))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/cohorts/10/gradebook"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    void testGetGradebook_AutoResolvesFinalExamGradeFromGradedAssignmentSubmission() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .processGrade(new BigDecimal("8.00"))
                .finalExamGrade(BigDecimal.ZERO)
                .finalGrade(new BigDecimal("4.00"))
                .isPassed(false)
                .build();

        Lesson finalExamLesson = Lesson.builder()
                .id(100L)
                .title("Báo cáo đồ án cuối kỳ")
                .lessonType("ASSIGNMENT")
                .build();
        sampleCohort.setFinalLesson(finalExamLesson);

        Assignment assignment = Assignment.builder()
                .id(5L)
                .lesson(finalExamLesson)
                .maxScore(new BigDecimal("10.0"))
                .passingScore(new BigDecimal("5.0"))
                .build();

        AssignmentSubmission gradedSubmission = AssignmentSubmission.builder()
                .id(88L)
                .assignment(assignment)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .score(new BigDecimal("9.50"))
                .status("GRADED")
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 100L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(2L);
        when(quizRepository.findByLessonId(100L)).thenReturn(Optional.empty());
        when(assignmentRepository.findByLessonId(100L)).thenReturn(Optional.of(assignment));
        when(assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(5L, 2L, 10L))
                .thenReturn(Optional.of(gradedSubmission));

        GradebookDTO dto = gradebookService.getGradebook(10L, 1L);

        assertNotNull(dto);
        assertEquals(1, dto.getStudents().size());
        assertEquals(new BigDecimal("9.50"), dto.getStudents().get(0).getFinalExamGrade());
        assertEquals(new BigDecimal("8.90"), dto.getStudents().get(0).getFinalGrade());
        assertTrue(dto.getStudents().get(0).getIsPassed());
        verify(cohortMemberRepository).save(member);
    }

    @Test
    void testSyncStudentGrade_UpdatesCohortMemberWithFinalExamScore() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .processGrade(new BigDecimal("8.00"))
                .finalExamGrade(BigDecimal.ZERO)
                .finalGrade(new BigDecimal("4.00"))
                .isPassed(false)
                .build();

        Lesson finalExamLesson = Lesson.builder()
                .id(100L)
                .title("Báo cáo đồ án cuối kỳ")
                .lessonType("ASSIGNMENT")
                .build();
        sampleCohort.setFinalLesson(finalExamLesson);

        Assignment assignment = Assignment.builder()
                .id(5L)
                .lesson(finalExamLesson)
                .maxScore(new BigDecimal("10.0"))
                .passingScore(new BigDecimal("5.0"))
                .build();

        AssignmentSubmission gradedSubmission = AssignmentSubmission.builder()
                .id(88L)
                .assignment(assignment)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .score(new BigDecimal("9.00"))
                .status("GRADED")
                .build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(cohortMemberRepository.findByCohort_IdAndUser_Id(10L, 2L)).thenReturn(Optional.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(100L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(1L);
        when(quizRepository.findByLessonId(100L)).thenReturn(Optional.empty());
        when(assignmentRepository.findByLessonId(100L)).thenReturn(Optional.of(assignment));
        when(assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(5L, 2L, 10L))
                .thenReturn(Optional.of(gradedSubmission));

        gradebookService.syncStudentGrade(10L, 2L);

        assertEquals(new BigDecimal("9.00"), member.getFinalExamGrade());
        assertEquals(new BigDecimal("9.40"), member.getFinalGrade());
        assertTrue(member.getIsPassed());
        verify(cohortMemberRepository).save(member);
    }

    @Test
    void testGetGradebook_IncludesProcessColumnsAndFinalExamColumnAndStudentScores() {
        CohortMember member = CohortMember.builder()
                .id(50L)
                .cohort(sampleCohort)
                .user(sampleStudent)
                .processGrade(new BigDecimal("8.50"))
                .finalExamGrade(new BigDecimal("9.00"))
                .finalGrade(new BigDecimal("8.80"))
                .isPassed(true)
                .build();

        Lesson quizLesson = Lesson.builder().id(101L).title("Quiz 1").lessonType("QUIZ").build();
        Quiz quiz = Quiz.builder().id(201L).lesson(quizLesson).title("Quiz 1").passingScore(new BigDecimal("5.0")).build();
        QuizSubmission quizSub = QuizSubmission.builder().id(301L).quiz(quiz).score(new BigDecimal("8.00")).build();

        Lesson assignLesson = Lesson.builder().id(102L).title("Lab 1").lessonType("ASSIGNMENT").build();
        Assignment assignment = Assignment.builder().id(202L).lesson(assignLesson).title("Lab 1").maxScore(new BigDecimal("10.0")).passingScore(new BigDecimal("5.0")).build();
        AssignmentSubmission assignSub = AssignmentSubmission.builder().id(302L).assignment(assignment).score(new BigDecimal("9.00")).status("GRADED").build();

        Quiz finalQuiz = Quiz.builder().id(200L).lesson(finalLesson).title("Thi Cuối Kỳ").passingScore(new BigDecimal("5.0")).build();
        QuizSubmission finalSub = QuizSubmission.builder().id(300L).quiz(finalQuiz).score(new BigDecimal("9.00")).build();

        when(cohortRepository.findById(10L)).thenReturn(Optional.of(sampleCohort));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleInstructor));
        when(cohortMemberRepository.findByCohort_IdOrderByEnrolledAtAsc(10L)).thenReturn(List.of(member));
        when(cohortScheduleRepository.findLessonIdsByCohortId(10L)).thenReturn(List.of(101L, 102L, 100L));
        when(lessonCompletionRepository.countByCohort_IdAndUser_Id(10L, 2L)).thenReturn(3L);

        when(quizRepository.findByLessonId(101L)).thenReturn(Optional.of(quiz));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(201L, 2L, 10L)).thenReturn(Optional.of(quizSub));

        when(assignmentRepository.findByLessonId(102L)).thenReturn(Optional.of(assignment));
        when(assignmentSubmissionRepository.findByAssignmentIdAndUserIdAndCohortId(202L, 2L, 10L)).thenReturn(Optional.of(assignSub));

        when(quizRepository.findByLessonId(100L)).thenReturn(Optional.of(finalQuiz));
        when(quizSubmissionRepository.findTopByQuizIdAndUserIdAndCohortIdOrderByScoreDesc(200L, 2L, 10L)).thenReturn(Optional.of(finalSub));

        GradebookDTO dto = gradebookService.getGradebook(10L, 1L);

        assertNotNull(dto);
        assertEquals(3, dto.getColumns().size());
        assertEquals(2, dto.getProcessColumns().size());
        assertNotNull(dto.getFinalExamColumn());
        assertEquals(100L, dto.getFinalExamColumn().getLessonId());

        assertEquals(1, dto.getStudents().size());
        var studentDTO = dto.getStudents().get(0);
        assertNotNull(studentDTO.getColumnGrades());
        assertEquals(3, studentDTO.getColumnGrades().size());

        assertEquals(new BigDecimal("8.00"), studentDTO.getColumnGrades().get(101L).getScore());
        assertEquals("GRADED", studentDTO.getColumnGrades().get(101L).getStatus());

        assertEquals(new BigDecimal("9.00"), studentDTO.getColumnGrades().get(102L).getScore());
        assertEquals("GRADED", studentDTO.getColumnGrades().get(102L).getStatus());

        assertEquals(2, studentDTO.getProcessColumnGrades().size());
        assertNotNull(studentDTO.getFinalExamColumnGrade());
        assertEquals(new BigDecimal("9.00"), studentDTO.getFinalExamColumnGrade().getScore());
    }
}
