package vn.edu.ute.utelearn;

import org.junit.jupiter.api.Test;
import vn.edu.ute.utelearn.util.ValidationUtils;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilsSearchTest {

    @Test
    void testRemoveAccentsBasic() {
        assertEquals("kinh te", ValidationUtils.removeAccents("kinh tế"));
        assertEquals("Thiet ke do hoa", ValidationUtils.removeAccents("Thiết kế đồ họa"));
        assertEquals("lap trinh", ValidationUtils.removeAccents("lập trình"));
        assertEquals("toan hoc", ValidationUtils.removeAccents("toán học"));
        assertEquals("Do hoa", ValidationUtils.removeAccents("Đồ họa"));
        assertEquals("da ngon ngu", ValidationUtils.removeAccents("đa ngôn ngữ"));
    }

    @Test
    void testRemoveAccentsEdgeCases() {
        assertEquals("", ValidationUtils.removeAccents(null));
        assertEquals("", ValidationUtils.removeAccents("   "));
        assertEquals("abc 123", ValidationUtils.removeAccents("abc 123"));
        assertEquals("test", ValidationUtils.removeAccents("tést"));
    }

    @Test
    void testUnaccentMatching() {
        String courseTitle = "Khóa học Kinh tế vi mô và Thiết kế đồ họa";
        String normalizedTitle = ValidationUtils.removeAccents(courseTitle).toLowerCase();

        String query1 = "te";
        String normalizedQuery1 = ValidationUtils.removeAccents(query1).toLowerCase();
        assertTrue(normalizedTitle.contains(normalizedQuery1));

        String query2 = "tế";
        String normalizedQuery2 = ValidationUtils.removeAccents(query2).toLowerCase();
        assertTrue(normalizedTitle.contains(normalizedQuery2));

        String query3 = "thiet ke";
        String normalizedQuery3 = ValidationUtils.removeAccents(query3).toLowerCase();
        assertTrue(normalizedTitle.contains(normalizedQuery3));

        String query4 = "thiết kế";
        String normalizedQuery4 = ValidationUtils.removeAccents(query4).toLowerCase();
        assertTrue(normalizedTitle.contains(normalizedQuery4));

        String query5 = "do hoa";
        String normalizedQuery5 = ValidationUtils.removeAccents(query5).toLowerCase();
        assertTrue(normalizedTitle.contains(normalizedQuery5));
    }
}
