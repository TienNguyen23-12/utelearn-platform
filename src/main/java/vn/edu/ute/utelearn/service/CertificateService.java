package vn.edu.ute.utelearn.service;

import vn.edu.ute.utelearn.dto.CertificateViewDTO;

import java.util.Optional;

public interface CertificateService {
    Optional<CertificateViewDTO> getCertificateByCode(String code);
    CertificateViewDTO submitCourseReview(Long userId, Long cohortId, String certCode, int rating, String comment);
    CertificateViewDTO submitCourseReview(Long userId, Long cohortId, Long courseId, String certCode, int rating, String comment);
}
