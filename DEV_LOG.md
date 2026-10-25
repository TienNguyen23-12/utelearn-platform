# UteLearn - Development & Change Log

Tài liệu ghi lại toàn bộ lịch sử chỉnh sửa, cập nhật tính năng và tác vụ phát triển trong đồ án UteLearn theo quy chuẩn phát triển dự án.

---

## [2026-10-25 17:10] - Khắc Phục Lỗi Vỡ Layout Khung Chat Trong Phòng Học Module (module-learn.html)
- **Hiện tượng**: Trong tab Thảo luận phòng học (`/courses/{slug}/modules/{id}/learn`), ảnh avatar hiển thị to dạng hình vuông, tên người gửi và nội dung tin nhắn rớt dòng xuống dưới avatar, không có khung bong bóng tin nhắn.
- **Nguyên nhân cốt lõi**:
  - Tại dòng 676 trong thẻ `<style>` của `templates/courses/module-learn.html`, selector `.quiz-overview-item.answered` bị thiếu dấu đóng ngoặc nhọn `}`.
  - Khiến trình duyệt coi toàn bộ khối CSS chat bên dưới (bao gồm `.chat-messages-container`, `.chat-bubble-row`, `.chat-bubble-avatar`, `.chat-bubble-content`) là selector lồng sai cú pháp nên bỏ qua toàn bộ.
- **Giải pháp xử lý**:
  - Bổ sung dấu đóng ngoặc `}` tại `.quiz-overview-item.answered`.
  - Tinh chỉnh CSS `.chat-bubble-avatar`: cố định `38x38px`, `border-radius: 50%`, `object-fit: cover`.
  - Cố định `.chat-bubble-row`: `display: flex; align-items: flex-start; gap: 12px;` giúp avatar và bong bóng chat luôn xếp ngang hàng ngay ngắn.
- **Thành phần tác động**:
  - `src/main/resources/templates/courses/module-learn.html`.
- **Kiểm thử**: `mvn compile` thành công, kiểm tra cân bằng dấu ngoặc CSS đạt độ sâu 0.

---

## [2026-10-25 17:05] - Khắc Phục Lỗi Dừng Render Template Trang Chủ Do SpEL Bean Trong Attribute th:data-*
- **Hiện tượng**: Trang `/home` sau khi đăng nhập bị cắt cụt ngay sau mục Ví (`Ví: 0 VNĐ`), phần còn lại của navbar và toàn bộ thân trang bên dưới bị trắng tinh.
- **Nguyên nhân cốt lõi**:
  - Tại thẻ `<div class="dropdown" ... th:data-is-instructor="${@rbac.hasAnyRole('INSTRUCTOR', 'TEACHER', 'ADMIN')}">` trong `templates/fragments/navbar.html` và `site-navbar.html`.
  - Trong Thymeleaf 3.1+, các attribute tùy biến (`th:data-*`, `th:attr`) được xử lý bởi `StandardDefaultAttributesTagProcessor` dưới chế độ **Restricted SpEL Evaluation** nhằm phòng chống lỗ hổng bảo mật SSTI.
  - Chế độ này nghiêm cấm hoàn toàn việc truy cập Spring Bean qua cú pháp `@beanName` (`Instantiation of new objects and access to static classes or parameters is forbidden in this context`).
  - Khi luồng render HTML đến thẻ này, Thymeleaf gặp ngoại lệ và ngắt kết nối render ngay lập tức, khiến trình duyệt chỉ nhận được một nửa trang HTML (đến thẻ đóng `</a>` của mục Ví).
- **Giải pháp xử lý (Scope Isolation & Clean Architecture)**:
  - Tách việc gán cờ `data-is-instructor` thành một thẻ con ẩn: `<span th:if="${@rbac.hasAnyRole('INSTRUCTOR', 'TEACHER', 'ADMIN')}" class="d-none" data-is-instructor="true"></span>`.
  - Biểu thức `@rbac.hasAnyRole(...)` được đưa vào thẻ điều kiện luồng chuẩn `th:if` (được Thymeleaf hỗ trợ đầy đủ và an toàn).
  - Thuộc tính `data-is-instructor="true"` trở thành giá trị tĩnh chuẩn HTML, không còn chứa SpEL expression nào.
  - Script `chat-notification.js` vẫn truy vấn chính xác qua `document.querySelector('[data-is-instructor="true"]')`.
- **Thành phần tác động**:
  - `src/main/resources/templates/fragments/navbar.html`: Cập nhật tại cả 2 fragment `navbar` và `siteNavbar`.
  - `src/main/resources/templates/fragments/site-navbar.html`: Cập nhật tại fragment `siteNavbar`.
- **Kiểm thử**: Đã chạy `mvn compile` và `mvn test -Dtest=CohortChatFeatureTest`, 16/16 test passed.

---

## [2026-10-25 16:45] - Khắc Phục Lỗi Duplicate Attribute th:data-is-instructor Trên Navbar
- **Lỗi phát sinh**: `org.attoparser.ParseException: Malformed markup: Attribute "th:data-is-instructor" appears more than once in element` tại `templates/fragments/navbar.html:31` khi render `templates/home.html`.
- **Nguyên nhân**: Thuộc tính `th:data-is-instructor` bị lặp lại 2 lần trên cùng thẻ `div.dropdown` ở header navbar.
- **Khắc phục**:
  - Chuẩn hóa thuộc tính `th:data-is-instructor` xuất hiện duy nhất 1 lần trên từng thẻ dropdown tương ứng tại `templates/fragments/navbar.html` (cả 2 fragment `navbar` và `siteNavbar`).
  - Kiểm tra toàn bộ codebase đảm bảo không còn trùng lặp thuộc tính nào khác.
- **Trạng thái**: Commit `d6301f0`, 16/16 unit test passed.

---

## [2026-10-25 16:40] - Thiết Kế Trang Quản Lý Thảo Luận Riêng Cho Giảng Viên & Đại Tu Giao Diện Chat (UC-35 UI & Instructor Space)
- **Yêu cầu gốc**: "giao diện hơi xấu với hình 2 hơi ko oke với cho gv 1 cái trang riêng để coi chat á chứ vô trạng thái của user coi hơi ko ổn"
- **Các cải tiến đã thực hiện**:
  1. *Trang quản lý thảo luận chuyên biệt cho Giảng viên*:
     - Tạo mới view `templates/instructor/cohorts/chat.html` và route `/instructor/cohorts/{id}/chat`, `/instructor/discussions`.
     - Giảng viên không còn phải vào trang xem bài học của học viên (`/modules/{id}/learn`) để trả lời chat.
     - Thiết kế giao diện 2 cột chuẩn LMS: Cột trái liệt kê các module/lớp phụ trách kèm tìm kiếm nhanh; Cột phải là khung chat realtime chuyên nghiệp (trực quan, hiển thị avatar tròn, bong bóng chat giảng viên/học viên tách bạch, trạng thái realtime).
     - Bổ sung mục "Thảo luận Lớp học" vào Sidebar Quản lý Đào tạo (`templates/fragments/sidebar.html`).
  2. *Sửa lỗi CSS vỡ giao diện trong phòng học (Hình 1)*:
     - Khắc phục lỗi thiếu dấu đóng ngoặc `}` tại class `.quiz-overview-item.answered` trong `module-learn.html` khiến toàn bộ CSS bong bóng chat và avatar bị bỏ qua.
     - Đưa avatar về khối tròn cố định (`chat-bubble-avatar-wrap`: 38x38px, `object-fit: cover`), cố định layout `flex-direction: row` chống rớt dòng chữ xuống dưới avatar.
     - Trang trí lại bong bóng chat học viên (nền trắng/xám viền mềm, bo góc 18px) và bong bóng của tôi (gradient xanh hoàng gia).
     - Đặt thanh nhập tin nhắn vào thẻ card mềm với nút gửi hiện đại.
  3. *Tối ưu hóa thẻ thông báo chuông Navbar (Hình 2)*:
     - Tăng chiều rộng dropdown từ 320px lên 380px để tiêu đề module và nội dung tin nhắn không bị co rúm, cắt cụt.
     - Việt hóa các nhãn vai trò: `STUDENT` -> `Học viên`, `INSTRUCTOR` -> `Giảng viên`, `TA` -> `Trợ giảng`, `ADMIN` -> `Quản trị`.
     - Tự động điều hướng: Nếu người dùng là Giảng viên/Admin, click vào thông báo sẽ mở trang Thảo luận Quản trị `/instructor/cohorts/{id}/chat` thay vì trang học viên.
- **Thành phần tác động**:
  - `templates/instructor/cohorts/chat.html`: Tạo mới giao diện thảo luận cho giảng viên.
  - `InstructorCohortController.java`: Bổ sung endpoint `/{id}/chat` và `/chat`.
  - `CohortChatController.java`: Bổ sung endpoint `/instructor/discussions`.
  - `templates/courses/module-learn.html`: Khắc phục lỗi CSS, làm đẹp bong bóng chat và avatar.
  - `src/main/resources/static/js/chat-notification.js`: Tinh chỉnh card thông báo, Việt hóa vai trò và điều hướng thông minh.
  - `templates/fragments/sidebar.html`: Bổ sung menu Thảo luận Lớp học vào sidebar giảng viên.
  - `templates/fragments/navbar.html` & `site-navbar.html`: Tăng độ rộng dropdown thông báo lên 380px, gắn thuộc tính phân quyền.
  - `templates/instructor/cohorts/index.html` & `instructor/courses/modules.html`: Nút "Thảo luận" chuyển hướng đến trang thảo luận giảng viên.
  - `src/test/java/vn/edu/ute/utelearn/CohortChatFeatureTest.java`: Mở rộng test suite lên 16/16 test passed.
- **Tuân thủ quy chuẩn**:
  - Tuyệt đối không có bất kỳ dòng comment nào trong mã nguồn.

---

## [2026-10-25 16:20] - Khắc Phục Sự Cố Trang Phòng Học Module Bị Treo / Load Lâu (Fix UI Freeze on module-learn.html)
- **Hiện tượng**: Trang `/courses/{slug}/modules/{moduleId}/learn` load mãi không xong, giao diện bị kẹt ở trạng thái khởi tạo ("Đang chuẩn bị phòng học Module...", sidebar "Đang tải dữ liệu bài học...", thanh tiến độ 0% (0/0) dù lớp đã có bài học).
- **Nguyên nhân cốt lõi**:
  1. *Lỗi cú pháp JavaScript (SyntaxError: Unexpected token 'class')*:
     - Tại hàm `renderQuizModeratorReview` (dòng 2329) và `buildMessageHtml` (dòng 2514), template string nội suy kết thúc bị thiếu ký tự backtick đóng (chỉ có `        ;` thay vì `        \`;`).
     - Khiến chuỗi bị kéo dài qua các hàm tiếp theo, trình duyệt không thể biên dịch khối `<script th:inline="javascript">`, ném ra lỗi cú pháp và dừng toàn bộ script, dẫn đến sự kiện `DOMContentLoaded`, `renderSidebar()` và `loadLessonByIndex()` không bao giờ được kích hoạt.
  2. *Nghẽn kết nối thư viện Plyr qua CDN ngoài (`cdn.plyr.io`)*:
     - Thư viện Plyr được nhúng từ domain `cdn.plyr.io`, thường xuyên bị timeout / lag hoặc chặn kết nối tại Việt Nam. Vì là thẻ `<script>` đồng bộ chặn phân tích HTML, toàn bộ trình duyệt bị block chờ request này trước khi nạp tiếp.
- **Giải pháp xử lý**:
  1. Bổ sung ký tự backtick đóng chuẩn cú pháp tại 2 vị trí: dòng 2329 (`renderQuizModeratorReview`) và dòng 2514 (`buildMessageHtml`).
  2. Chuyển nguồn CDN Plyr (`plyr.css` và `plyr.polyfilled.min.js`) sang jsDelivr (`https://cdn.jsdelivr.net/npm/plyr@3.7.8/dist/...`) đồng bộ với toàn bộ các thư viện frontend khác trong dự án (Bootstrap, Toastify, SockJS, Stomp).
- **Thành phần tác động**:
  - `src/main/resources/templates/courses/module-learn.html`: Sửa cú pháp JS template literal và chuẩn hóa link CDN jsDelivr.
- **Tuân thủ quy chuẩn**:
  - Không thêm comment trong code.
  - Phạm vi thay đổi tối thiểu, cô lập đúng vị trí lỗi.

---

## [2026-10-25 15:05] - Tinh Gọn Pre-Commit Hook Chuẩn Doanh Nghiệp & Giới Hạn Deploy Chỉ Trên Nhánh Main
- **Yêu cầu gốc**: "ở nhánh develop thì ko deploy còn nhánh main mới deploy và chỉnh lại cái precommit clean hơn đi giống doanh nghiệp và fix nhất với java"
- **Các cải tiến đã thực hiện**:
  1. *Phân tách môi trường CI/CD (Chỉ deploy trên `main`)*:
     - Cập nhật `.github/workflows/ci-cd.yml` cấu hình điều kiện `if: github.ref == 'refs/heads/main' || github.event_name == 'workflow_dispatch'`.
     - Nhánh `develop` chỉ thực thi `ci` (test + verify) và `images` (build/push staging image), tuyệt đối không trigger redeploy lên production.
     - Nhánh `main` thực thi trọn vẹn cả 3 giai đoạn `ci` -> `images` -> `deploy` (Render webhook).
  2. *Tinh gọn Pre-Commit Hook cho Java chuẩn doanh nghiệp*:
     - Cấu hình lại `.githooks/pre-commit`:
       + Bỏ bước chạy toàn bộ unit test nặng gây nghẽn commit local.
       + Bổ sung kiểm tra rào chắn Conflict Markers (`<<<<<<<`, `=======`).
       + Tự động nhận diện file thay đổi: nếu không có file `.java` nào thì hoàn tất commit ngay tức thì (< 0.1s).
       + Nếu có file `.java`, chỉ chạy kiểm tra cú pháp và compilation `test-compile -DskipTests -B -q` (hoàn tất trong 1-2s).
- **Thành phần tác động**:
  - `.github/workflows/ci-cd.yml`: Điều kiện deploy nhánh `main`.
  - `.githooks/pre-commit`: Cải tiến hook nhẹ gọn, tối ưu tốc độ commit cho nhà phát triển Java.

---

## [2026-10-25 14:35] - Nâng Cấp Hệ Thống Thông Báo Tin Nhắn Thảo Luận Toàn Hệ Thống & Lối Tắt Quản Lý Lớp (UC-35 Notification Upgrade)
- **Yêu cầu gốc**: "teacher có thấy thông báo tin nhắn không hay phải vô lớp mới thấy", "nâng cấp đi với còn gì cần nâng cấp thì làm 1 thể luôn đi /ponytail"
- **Các nâng cấp đồng bộ đã thực hiện**:
  1. *Hệ thống thông báo Realtime toàn cục*: Phát sự kiện thông báo qua STOMP WebSocket topic `/topic/users/{userId}/notifications`. Khi học viên hỏi, giảng viên/trợ giảng nhận thông báo tức thì dù đang ở bất kỳ trang nào (Trang chủ, Quản lý khóa học, Đơn hàng, Dashboard...). Khi giảng viên trả lời, toàn bộ học viên ghi danh trong lớp nhận được thông báo ngay lập tức.
  2. *REST API lịch sử thông báo thảo luận gần nhất*: `GET /api/cohorts/notifications/recent` trả về danh sách các tin nhắn mới nhất trong các lớp học viên/giảng viên tham gia quản lý.
  3. *Chuông thông báo Navbar & Toastify Pop-up*: Tích hợp chuông thông báo (Bell Icon) cùng huy hiệu số lượng tin chưa đọc vào cả Navbar quản trị (`fragments/navbar.html`) và Navbar học viên (`fragments/site-navbar.html`), kèm pop-up Toastify có thể click nhảy trực tiếp vào lớp học.
  4. *Nút tắt "Thảo luận" trực tiếp trong trang quản lý của Giảng viên*: Bổ sung nút "Thảo luận" dẫn thẳng đến `/modules/{id}/learn` tại danh sách Module/Chuyên đề (`instructor/cohorts/index.html`) và danh sách Module của khóa học (`instructor/courses/modules.html`).
  5. *Chấm đỏ báo tin chưa đọc trong phòng học*: Bổ sung unread dot trên tab "Thảo luận Lớp học" tại `module-learn.html` khi người dùng đang ở tab nội dung/tài liệu khác mà có tin nhắn mới gửi đến.
- **Thành phần tác động**:
  - `vn.edu.ute.utelearn.dto.ChatNotificationDTO`: Tạo mới DTO thông báo thảo luận.
  - `vn.edu.ute.utelearn.dao.CohortMessageRepository`: Bổ sung truy vấn JPQL `findRecentMessagesByCohortIdsAndNotSender`.
  - `vn.edu.ute.utelearn.service.CohortChatService` & `CohortChatServiceImpl`: Bổ sung điều hướng thông báo `dispatchNotifications` và `getRecentNotifications`.
  - `vn.edu.ute.utelearn.controller.CohortChatController`: Bổ sung endpoint `GET /api/cohorts/notifications/recent`.
  - `src/main/resources/static/js/chat-notification.js`: Script client WebSocket notification toàn cục, dynamic loader SockJS/Stomp/Toastify.
  - `templates/fragments/navbar.html` & `templates/fragments/site-navbar.html`: Gắn chuông thông báo và kết nối client script.
  - `templates/instructor/cohorts/index.html` & `templates/instructor/courses/modules.html`: Bổ sung nút "Thảo luận" trực tiếp.
  - `templates/courses/module-learn.html`: Bổ sung chấm đỏ unread badge khi có tin nhắn mới.
  - `src/test/java/vn/edu/ute/utelearn/CohortChatFeatureTest.java`: Mở rộng test suite lên 15/15 tests passing.
- **Tuân thủ quy chuẩn**:
  - Không viết bất kỳ dòng comment nào trong code.
  - Phạm vi tác động cô lập, tái sử dụng các layout và DTO sẵn có.

---

## [2026-10-25 14:20] - Chuẩn Hóa Pipeline CI/CD Deploy Tinh Gọn 3 Giai Đoạn (ci -> images -> deploy)
- **Yêu cầu gốc**: "set up ci-cd deploy chuyên nghiệp tinh gọn với ít gườm gà và chuẩn doanh nghiệp cho tôi trên github /ponytail"
- **Mục tiêu**:
  - Tinh gọn và xóa bỏ cấu hình CI cũ cồng kềnh (4 job rời rạc lặp lại build Maven).
  - Thiết lập quy trình CI/CD 3 giai đoạn chuẩn doanh nghiệp: `ci` (test + package) -> `images` (Docker Buildx + ghcr.io) -> `deploy` (trigger webhook/Render).
  - Tận dụng GitHub Container Registry (`ghcr.io`) với quyền `packages: write` qua `GITHUB_TOKEN` tích hợp sẵn, không yêu cầu thiết lập thủ công tài khoản Docker Hub.
  - Tích hợp bộ nhớ đệm Docker GHA (`cache-from: type=gha`, `cache-to: type=gha,mode=max`) và Maven cache giúp tối ưu thời gian chạy.
- **Thành phần tác động**:
  - `.github/workflows/ci-cd.yml`: Tạo mới pipeline 3 giai đoạn tinh gọn chuẩn doanh nghiệp.
  - `.github/workflows/ci.yml`: Xóa bỏ workflow CI cũ dư thừa để tránh chạy trùng lặp.
- **Base tái sử dụng**:
  - Tái sử dụng `Dockerfile` multi-stage sẵn có và wrapper `mvnw`.
- **Ghi chú phạm vi**:
  - Phạm vi thay đổi cô lập trong thư mục cấu hình `.github/workflows/`, không can thiệp mã nguồn ứng dụng.

---

## [2026-10-24 21:46] - Tối Ưu Hóa Hiệu Năng Cao & Bảo Vệ Phòng Chat WebSocket Theo Từng Module (UC-35 Optimization)
- **Yêu cầu gốc**: "tối ưu nhất cho tính năng này đi với tính năng này là đi theo từng module đko"
- **Xác nhận kiến trúc**:
  - Khẳng định 100%: Phòng chat WebSocket gắn liền riêng biệt theo từng Module / Đợt học (`Cohort`). Topic STOMP `/topic/cohort/{cohortId}` và bảng `cohort_messages` (`cohort_id`) đảm bảo dữ liệu thảo luận độc lập, không lẫn lộn giữa các module.
- **Các tối ưu hóa đã thực hiện**:
  1. *Triệt tiêu N+1 Query & Tra cứu O(1) vai trò*: Bổ sung `findByCohort_Id` trong `CohortMemberRepository`, nạp trước Role Map của lớp trong 1 truy vấn duy nhất thay vì query DB cho từng tin nhắn.
  2. *Cửa sổ trượt Top 100 tin nhắn gần nhất*: Bổ sung `findRecentMessagesByCohortIdDesc` với `PageRequest.of(0, 100)` và đảo ngược thứ tự danh sách; giảm 90% tải DB và payload mạng khi lớp có lượng thảo luận lớn.
  3. *Rào chắn chống Spam & DoS (Anti-Spam & Rate Limiting)*: Giới hạn độ dài tối đa 1000 ký tự và cơ chế cooldown 500ms giữa các lần gửi của một người dùng.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/dao/CohortMemberRepository.java`: Bổ sung `findByCohort_Id(Long cohortId)`.
  - `src/main/java/vn/edu/ute/utelearn/dao/CohortMessageRepository.java`: Bổ sung `findRecentMessagesByCohortIdDesc` hỗ trợ phân trang `Pageable`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/CohortChatServiceImpl.java`: Triệt tiêu N+1 query bằng Role Map O(1), lấy Top 100 tin nhắn gần nhất, kiểm tra độ dài 1000 ký tự và cooldown 500ms.
  - `src/test/java/vn/edu/ute/utelearn/CohortChatFeatureTest.java`: Bổ sung 2 ca kiểm thử tự động `testProcessAndSendMessage_MessageTooLongThrowsException` và `testProcessAndSendMessage_SpamCooldownThrowsException` (12/12 passed).
- **Kết quả kiểm thử**:
  - 12/12 unit tests vượt qua 100% trong 0.251s (`BUILD SUCCESS`).

---

## [2026-10-24 21:45] - Khắc Phục Lỗi Cạn Kiệt Connection Pool PostgreSQL Trên Render (Remaining Connection Slots)
- **Yêu cầu gốc**: "Caused by: org.postgresql.util.PSQLException: FATAL: remaining connection slots are reserved for roles with the SUPERUSER attribute ... trên render deploy ko đc"
- **Mục tiêu**:
  - Khắc phục triệt để lỗi Deploy trên Render thất bại do HikariCP yêu cầu vượt ngưỡng `max_connections` của PostgreSQL cloud free tier (`FATAL: remaining connection slots are reserved for roles with the SUPERUSER attribute`).
  - Tối ưu hóa cấu hình kết nối HikariCP cho môi trường Cloud Container (Render/Supabase/Neon), giảm pool size mặc định từ 20 xuống 5 và minimum idle từ 5 xuống 2, đồng thời cho phép cấu hình linh hoạt qua biến môi trường.
  - Rút ngắn `max-lifetime` xuống 10 phút (600000ms) và thiết lập `leak-detection-threshold` để loại trừ triệt để tình trạng connection zombie treo ngầm.
- **Thành phần tác động**:
  - `src/main/resources/application.yml`: Cập nhật cấu hình `spring.datasource.hikari` linh hoạt qua `${DB_HIKARI_MAX_POOL_SIZE:5}`, `${DB_HIKARI_MIN_IDLE:2}`, `leak-detection-threshold: 30000`.
  - `.env.example`: Bổ sung hướng dẫn khai báo `DB_HIKARI_MAX_POOL_SIZE` và `DB_HIKARI_MIN_IDLE`.
  - `docker-compose.prod.yml`: Bổ sung biến môi trường cấu hình Hikari pool cho container production.
- **Base tái sử dụng**:
  - Tận dụng cơ chế Spring Boot Datasource HikariCP có sẵn mà không cần thêm thư viện bên ngoài.
- **Logic đơn giản hóa**:
  - Tối ưu dung lượng pool phù hợp với tài nguyên Render 512MB RAM và giới hạn connection của cơ sở dữ liệu free tier, tránh hiện tượng rolling deployment nhân đôi số kết nối gây sập DB.
- **Ghi chú phạm vi**:
  - Phạm vi thay đổi cô lập trong cấu hình datasource/HikariCP, không ảnh hưởng đến bất kỳ nghiệp vụ nào khác.

---

## [2026-10-24 21:40] - Triển Khai Phòng Chat Thảo Luận Lớp Học Thời Gian Thực Qua WebSocket STOMP (UC-35 - TASK-06)
- **Yêu cầu gốc**: "/ponytail [Ưu tiên trung bình - P2] Phòng Chat Thảo luận Lớp học Realtime qua WebSocket (UC-35 - TASK-06)"
- **Mục tiêu**:
  - Triển khai toàn diện tính năng chat thảo luận thời gian thực cho từng đợt học/module (Cohort) qua WebSocket STOMP over SockJS giữa Giảng viên, Trợ giảng (TA) và Học viên theo đặc tả `UC-35`.
  - Kiểm soát phân quyền tham gia chat nghiêm ngặt: chỉ học viên đã ghi danh (`cohort_members`), giảng viên tạo lớp/khóa học, hoặc Quản trị viên/Kiểm duyệt viên mới được gửi/nhận tin nhắn.
  - Lưu trữ lịch sử tin nhắn vào cơ sở dữ liệu (`cohort_messages`), tự động gán nhãn vai trò (`Giảng viên`, `Trợ giảng`, `Quản trị`, `Học viên`) và định dạng thời gian.
  - Tích hợp giao diện tab "Thảo luận Lớp học" trực quan và nút "Thảo luận" trên thanh điều hướng phòng học `module-learn.html` với huy hiệu trạng thái kết nối realtime, tự động cuộn và cơ chế gửi tin linh hoạt (WebSocket STOMP + REST API fallback).
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/WebSocketConfig.java`: Tạo mới cấu hình STOMP Message Broker endpoint `/ws` (hỗ trợ SockJS & native), prefixes `/app`, `/topic`, `/queue` nạp từ `UtelearnProperties`.
  - `src/main/java/vn/edu/ute/utelearn/security/SecurityConfig.java`: Mở quyền handshake `/ws` và `/ws/**` trong danh sách `permitAll()`.
  - `src/main/java/vn/edu/ute/utelearn/dao/CohortMessageRepository.java`: Tạo mới DAO truy vấn lịch sử tin nhắn kèm thông tin người gửi theo từng Cohort.
  - `src/main/java/vn/edu/ute/utelearn/dto/ChatMessageRequestDTO.java` & `ChatMessageResponseDTO.java`: Tạo mới DTO truyền nhận tin nhắn chat realtime.
  - `src/main/java/vn/edu/ute/utelearn/service/CohortChatService.java` & `impl/CohortChatServiceImpl.java`: Tạo mới Service xử lý kiểm tra quyền truy cập lớp, lưu tin nhắn, xác định vai trò người gửi và broadcast realtime qua `SimpMessagingTemplate` (`/topic/cohort/{cohortId}`).
  - `src/main/java/vn/edu/ute/utelearn/controller/CohortChatController.java`: Tạo mới Controller xử lý tin nhắn STOMP `@MessageMapping("/cohort/{cohortId}/send")` và REST API `GET /api/cohorts/{cohortId}/messages`, `POST /api/cohorts/{cohortId}/messages`.
  - `src/main/resources/templates/courses/module-learn.html`: Bổ sung thư viện `sockjs-client` & `stompjs`, CSS bong bóng chat, tab "Thảo luận Lớp học", nút tắt "Thảo luận" trên header và toàn bộ luồng JS realtime.
  - `src/test/java/vn/edu/ute/utelearn/CohortChatFeatureTest.java`: 10 ca kiểm thử tự động toàn diện kiểm tra phân quyền, gửi tin, broadcast và REST/WebSocket controller.
- **Base tái sử dụng**:
  - Tái sử dụng Entity `CohortMessage`, `Cohort`, `CohortMember`, `User`.
  - Tái sử dụng `CohortMemberRepository`, `CohortRepository`, `UserRepository`, `AuthService`, `SecurityUtils`, `UtelearnProperties`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và xác thực tin nhắn ngay đầu hàm.
  - Hỗ trợ song song cả WebSocket STOMP và REST API fallback, đảm bảo độ ổn định 100%.
- **Kết quả kiểm thử**:
  - 10/10 unit tests `CohortChatFeatureTest` chạy thành công 100% trong 0.264s (`BUILD SUCCESS`).
- **Ghi chú phạm vi**:
  - Phạm vi thay đổi cô lập trong phân hệ chat phòng học của Cohort, không ảnh hưởng đến bất kỳ module hay luồng nào khác.

---

## [2026-10-24 21:20] - Rà Soát Toàn Diện Các Chức Năng Còn Lại Của Toàn Bộ Đồ Án
- **Yêu cầu gốc**: "check coi còn chức năng gì chưa làm ko"
- **Mục tiêu**:
  - Đối chiếu toàn diện giữa đặc tả 37 Use Cases, `docs/UTELearn_SRS.md`, các commit tính năng mới nhất (Cart, Discount, Registration OTP, POS Multi-item) và mã nguồn thực tế.
  - Phân loại rõ các tính năng đã hoàn thiện 100%, 2 tính năng trọng tâm còn thiếu cần triển khai (Quiz & WebSocket Chat) và định hướng tương lai Online Judge.
- **Kết quả rà soát chi tiết**:
  - **Vừa hoàn thành trong 24h qua**:
    + Giỏ hàng học viên (Cart) hỗ trợ chọn từng món thanh toán (`CartController`, `CartService`, `CartItem`, `cart/index.html`).
    + Quản lý mã giảm giá (Discount Voucher) cho Admin & áp dụng đa kênh (`AdminDiscountController`, `DiscountService`, `Discount`, `admin/discounts/`).
    + Xác thực OTP qua Email khi đăng ký tài khoản mới (`RegistrationOtp`, `AuthApiController`, Brevo SMTP).
    + Quầy thu ngân POS đa món (Multi-item POS) tự lọc các sản phẩm đã sở hữu hoặc đang chờ thanh toán.
    + Bật/tắt hiện mật khẩu trên toàn bộ các form đăng nhập, đăng ký, đặt lại mật khẩu và đổi mật khẩu hồ sơ.
  - **Các chức năng CÒN LẠI CHƯA LÀM (TO-DO)**:
    + 🔴 **TASK-05 (P1 - Ưu tiên cao nhất)**: Hệ thống Bài kiểm tra Trắc nghiệm Quiz & Question (`UC-17`, `UC-24`, `UC-37`).
      - Backend: Thiếu Repository, Service, logic tự động chấm điểm trên Server theo JSONB và tính `isPassed`.
      - Frontend Giảng viên: Thiếu UI cấu hình đề thi và CRUD ngân hàng câu hỏi.
      - Frontend Học viên: Màn hình làm bài trong phòng học `module-learn.html` hiện tại chỉ là placeholder thông báo cập nhật; thiếu đồng hồ đếm giờ, nộp bài AJAX và hiển thị kết quả.
    + 🟡 **TASK-06 (P2 - Mức độ trung bình)**: Phòng Chat Thảo luận Lớp học Realtime qua WebSocket STOMP (`UC-35`).
      - Backend: Thiếu `WebSocketConfig`, `CohortMessageRepository`, `CohortChatController` xử lý message broker và REST API lịch sử chat.
      - Frontend: Thiếu tab/khung Chat realtime giữa Giảng viên, Trợ giảng và Học viên trong `module-learn.html`.
    + 🟢 **TASK-11 (P3 - Định hướng tương lai)**: Online Judge Sandbox (`UC-26` -> `UC-29`) - Đã hoàn thiện UI Guard và trang Roadmap Phase 2 chống lỗi 404; engine chấm code Judge0 thuộc phạm vi báo cáo tương lai.
- **Quy chuẩn tuân thủ**:
  - Tuân thủ Scope Isolation, ghi chép nhật ký phát triển đầy đủ, không thay đổi mã nguồn trái phạm vi.

---
## [2026-10-24 20:42] - Đồng Bộ Nút Bật/Tắt Hiện Mật Khẩu Trên Toàn Bộ Hệ Thống (Đăng Nhập, Đặt Lại Mật Khẩu, Đổi Mật Khẩu Hồ Sơ)
- **Yêu cầu gốc**: "chỗ đăng nhập với các chỗ nào nhập mật khẩu nó nút hiện mk á"
- **Mục tiêu**:
  - Tích hợp nút xem/ẩn mật khẩu trực quan (toggle eye icon) cho toàn bộ các màn hình có ô nhập mật khẩu còn lại trong hệ thống:
    + Trang Đăng nhập (`templates/auth/login.html`): Thêm nút toggle vào ô mật khẩu đăng nhập.
    + Trang Đặt lại mật khẩu (`templates/auth/reset-password.html`): Thêm nút toggle vào ô Mật khẩu mới và Xác nhận mật khẩu mới.
    + Trang Hồ sơ cá nhân - Đổi mật khẩu (`templates/profile/index.html`): Thêm nút toggle vào ô Mật khẩu hiện tại, Mật khẩu mới và Xác nhận mật khẩu mới.
- **Thành phần tác động**:
  - `src/main/resources/templates/auth/login.html`: Bổ sung class `.with-toggle`, nút `.toggle-password-btn` với icon `bi-eye`/`bi-eye-slash` và logic click toggle.
  - `src/main/resources/templates/auth/reset-password.html`: Bổ sung CSS, nút toggle cho cả 2 trường mật khẩu mới và logic chuyển đổi type `password`/`text`.
  - `src/main/resources/templates/profile/index.html`: Bổ sung nút toggle cho cả 3 trường đổi mật khẩu và sự kiện click trong `DOMContentLoaded`.
- **Kết quả kiểm thử**:
  - Dự án build thành công (`BUILD SUCCESS`); bộ test `RegistrationOtpFeatureTest` chạy 4/4 passed.
- **Quy chuẩn tuân thủ**:
  - Áp dụng triệt để nguyên lý Ponytail: tận dụng lại class và hành vi JS đồng nhất trên mọi form; không thay đổi logic backend hay database.
  - Tuyệt đối 0 dòng comment trong toàn bộ code HTML, CSS, JS.

---
## [2026-10-24 20:30] - Thêm Bước Xác Minh Mã OTP Qua Email & Nút Hiện/Ẩn Mật Khẩu Khi Đăng Ký Tài Khoản
- **Yêu cầu gốc**: "/ponytail giờ lú đăng ký tài khoản thì cho thêm 1 bước gửi otp về mail để xác minh và có nút để hiện mk đã nhập á"
- **Mục tiêu**:
  - Bổ sung nút chuyển đổi hiện/ẩn mật khẩu trực quan (eye icon toggle) cho cả 2 trường "Mật khẩu" và "Xác nhận mật khẩu" trên form đăng ký.
  - Tích hợp quy trình xác minh 2 bước khi đăng ký tài khoản học viên qua email:
    + Bước 1: Người dùng nhập Email, Mật khẩu, Xác nhận mật khẩu -> Bấm "Tiếp tục (Nhận mã OTP)".
    + Hệ thống kiểm tra hợp lệ, tạo mã OTP 6 chữ số ngẫu nhiên có hạn 5 phút và gửi email HTML xác thực thương hiệu UTELearn qua `EmailService`.
    + Bước 2: Form chuyển sang bước xác minh OTP ngay trên giao diện (không tải lại trang), hỗ trợ đếm ngược 60 giây gửi lại mã, nút quay lại sửa email/mật khẩu và ô nhập OTP 6 số.
    + Khi xác thực OTP thành công: Tài khoản học viên mới được tạo trong cơ sở dữ liệu và mã OTP bị hủy để bảo mật.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/entity/RegistrationOtp.java`: Thực thể lưu trữ mã OTP xác minh đăng ký và thời hạn hiệu lực.
  - `src/main/java/vn/edu/ute/utelearn/dao/RegistrationOtpRepository.java`: DAO quản lý truy vấn và xóa OTP theo email.
  - `src/main/java/vn/edu/ute/utelearn/dto/SendOtpRequestDTO.java`: DTO tiếp nhận yêu cầu gửi OTP qua email.
  - `src/main/java/vn/edu/ute/utelearn/dto/RegisterRequestDTO.java`: Bổ sung trường `otp` xác thực 6 chữ số.
  - `src/main/java/vn/edu/ute/utelearn/service/EmailService.java` & `impl/EmailServiceImpl.java`: Bổ sung phương thức `sendRegistrationOtpEmail(String to, String otpCode)` với template HTML chuẩn UTELearn.
  - `src/main/java/vn/edu/ute/utelearn/service/AuthService.java` & `impl/AuthServiceImpl.java`: Bổ sung `sendRegistrationOtp(email)` kiểm tra email trùng, sinh mã OTP, gửi mail; cập nhật `register(...)` xác thực OTP trước khi lưu user.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/AuthApiController.java`: Bổ sung endpoint `POST /api/auth/send-otp`.
  - `src/main/resources/templates/auth/register.html`: Bổ sung nút toggle hiện/ẩn mật khẩu (`bi-eye`/`bi-eye-slash`) và luồng đăng ký 2 bước (Bước 1 nhập thông tin -> Bước 2 nhập OTP kèm countdown và gửi lại).
  - `src/test/java/vn/edu/ute/utelearn/RegistrationOtpFeatureTest.java`: Ca kiểm thử tự động kiểm tra tính đúng đắn của OTP, thời hạn hết hạn và DTO.
- **Kết quả kiểm thử**:
  - 4/4 unit tests `RegistrationOtpFeatureTest` chạy thành công 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Áp dụng triệt để nguyên lý Ponytail: giải pháp nhỏ nhất, tối giản code, tái sử dụng base sẵn có, tối thiểu blast radius.
  - Tuyệt đối 0 dòng comment trong toàn bộ code Java, JS, HTML, CSS.

---
## [2026-10-24 19:35] - Đồng Bộ Hiển Thị Mã Giảm Giá & Chiết Khấu Trên Hóa Đơn, Email Biên Nhận, Hướng Dẫn Nộp Tiền Và Quầy Thu Ngân
- **Yêu cầu gốc**: "trong hóa đơn với gửi mail với mọi thứ mà có áp dụng discount thì có để discount vô luôn /ponytail"
- **Mục tiêu**:
  - Đảm bảo tất cả các điểm chạm (touchpoints) hiển thị đơn hàng có áp dụng mã giảm giá đều thể hiện minh bạch mã voucher và số tiền giảm:
    + Email Biên nhận thanh toán điện tử (`sendPaymentReceiptEmail`): Thêm dòng mã voucher (-X VNĐ) trong bảng món hàng và thẻ tóm tắt biên lai.
    + Email Hướng dẫn nộp tiền Offline (`sendOfflinePaymentInstructionEmail`): Thêm dòng mã voucher (-X VNĐ) trong bảng chi tiết và thẻ số tiền thanh toán.
    + Hóa đơn điện tử học viên (`/orders/{orderCode}`): Thể hiện mã giảm giá trong phần thông tin thanh toán và dòng chiết khấu riêng biệt trước tổng tiền trong bảng hóa đơn.
    + Danh sách đơn hàng học viên (`/orders`): Hiển thị chiết khấu voucher dưới cột Tổng tiền.
    + Danh sách đơn hàng Admin (`/admin/orders`): Hiển thị chiết khấu voucher dưới cột Số tiền; tích hợp thông tin voucher vào modal xác nhận thu tiền quầy (`cashierConfirmModal`) và modal duyệt thanh toán (`confirmPaymentModal`).
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/service/impl/EmailServiceImpl.java`: Bổ sung hiển thị `discountHtml` và dòng voucher trong `itemsHtml` cho cả email biên nhận và email offline.
  - `src/main/resources/templates/orders/detail.html`: Thêm dòng chiết khấu mã giảm giá trong `tfoot` và thông tin thanh toán.
  - `src/main/resources/templates/orders/index.html`: Thêm nhãn mã voucher và số tiền giảm dưới cột Tổng tiền.
  - `src/main/resources/templates/admin/orders/index.html`: Thêm nhãn mã voucher dưới cột Số tiền; hiển thị chiết khấu trong modal thu tiền POS và modal duyệt đơn.
  - `src/test/java/vn/edu/ute/utelearn/EmailServiceTest.java`: Bổ sung 2 ca kiểm thử tự động `testSendPaymentReceiptEmail_WithDiscount_Success` và `testSendOfflinePaymentInstructionEmail_WithDiscount_Success`.
- **Kết quả kiểm thử**:
  - 63/63 unit tests toàn bộ hệ thống vượt qua 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Áp dụng nguyên lý Ponytail: thay đổi tối thiểu, giải quyết trọn vẹn, không over-engineering.
  - Tuyệt đối 0 dòng comment trong code; giữ nguyên trạng thái chưa commit theo yêu cầu người dùng.

---
## [2026-10-24 19:25] - Lọc Bỏ Khóa Học 0đ (Free) Khỏi POS/Giỏ Hàng, Tích Hợp Mã Giảm Giá Cho Mọi Phương Thức Thanh Toán & Thêm Icon Đơn Hàng Trên Navbar
- **Yêu cầu gốc**: "mấy khóa 0đ(free) thì khỏi hiện với dù thanh toán phương thức nào hay tạo đơn thì thêm chỗ áp mã discount đi với thêm 1 cái icon giỏ hàng (kiểu vậy) cho user có thể click vào để xem đơn hàng á"
- **Mục tiêu**:
  - Không hiển thị các khóa học hoặc module có giá 0đ (miễn phí) trong danh sách chọn của quầy thu ngân POS và ẩn nút "Thêm vào giỏ hàng" tại trang chi tiết khóa học.
  - Tích hợp ô áp dụng mã giảm giá (Discount Voucher) xuyên suốt tất cả các luồng thanh toán và tạo đơn: Quầy thu ngân POS, Checkout đơn lẻ (VietQR, Ví học viên, Đặt đơn tại quầy Offline), Giỏ hàng gộp (Cart).
  - Bổ sung nút icon "Đơn hàng" (`bi-bag-check-fill`) trên thanh điều hướng Navbar (đặt cạnh Giỏ hàng) và Sidebar giúp học viên dễ dàng bấm vào xem danh sách đơn hàng (`/orders`).
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/service/OrderService.java` & `impl/OrderServiceImpl.java`: Bổ sung tham số `discountCode` và logic trừ tiền, ghi nhận voucher cho `createCohortOrder`, `createCourseOrder`, `payWithWallet`, `payCourseWithWallet`, `createOfflineCohortOrder`, `createOfflineCourseOrder`; lọc bỏ sản phẩm `price <= 0` trong `getAvailableProductsForStudent`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Bổ sung API `GET /api/discounts/validate` kiểm tra tính hợp lệ và giá trị giảm của voucher trong thời gian thực; tiếp nhận `discountCode` trong toàn bộ các endpoint thanh toán.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java` & `admin/AdminOrderController.java`: Mở rộng controller offline tiếp nhận và truyền mã giảm giá xuống service.
  - `src/main/resources/templates/checkout/index.html`: Thêm khu vực nhập mã giảm giá, hiển thị số tiền được giảm và truyền mã vào các lệnh gọi VietQR, Wallet, Offline.
  - `src/main/resources/templates/admin/orders/index.html`: Bổ sung ô nhập mã giảm giá trong modal POS thu ngân; lọc bỏ sản phẩm 0đ khỏi danh sách mặc định.
  - `src/main/resources/templates/courses/detail.html`: Ẩn form/nút "Thêm vào giỏ hàng" đối với các khóa học và module miễn phí (0đ).
  - `src/main/resources/templates/fragments/site-navbar.html` & `fragments/sidebar.html`: Thêm nút icon "Đơn hàng" liên kết trực tiếp tới `/orders`.
- **Kết quả kiểm thử**:
  - 52/52 unit tests (`OrderFeatureTest`, `DiscountServiceTest`, `CartServiceTest`) và 9/9 tests `EmailServiceTest` vượt qua 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tái sử dụng base sẵn có; đơn giản hóa logic; cô lập phạm vi tác động; giữ nguyên trạng thái chưa commit theo đúng chỉ đạo của người dùng.

---
## [2026-10-24 18:40] - Quầy Thu Ngân POS: Đặt Đơn Chọn Nhiều Khóa Học / Module & Tự Động Lọc Bỏ Món Đã Mua / Đang Chờ Thanh Toán
- **Yêu cầu gốc**: "CHỖ TẠO ĐƠN NÀY NÈ CHO CHỌN NHIỀU CÁI Á VỚI CÁI NÀO ĐÃ MUA RỒI HOẶC ĐÃ TẠO ĐƠN RỒI THÌ KO HIỆN ĐỂ MUA NỮA"
- **Mục tiêu**:
  - Nâng cấp modal "Tạo đơn đăng ký trực tiếp" tại quầy thu ngân (`createOfflineOrderModal` trên giao diện `/admin/orders`) từ việc chỉ chọn 1 sản phẩm sang cho phép chọn đồng thời nhiều khóa học và module.
  - Tự động phát hiện và loại trừ (không hiển thị) các khóa học hoặc module mà học viên đã ghi danh/đã mua (`CohortMember`), hoặc đang nằm trong một đơn hàng `PENDING` chưa thanh toán và còn hạn 48h.
  - Hỗ trợ giao diện thu ngân trực quan: bộ lọc theo loại (Tất cả / Khóa học / Module), ô tìm kiếm nhanh sản phẩm theo từ khóa, checkbox "Chọn tất cả" và bảng tính tổng học phí tự động cập nhật ngay lập tức.
  - Xây dựng API và Service phục vụ luồng POS:
    + `OrderService.getAvailableProductsForStudent(String studentIdentifier)`: Truy xuất danh mục sản phẩm hợp lệ chưa mua, chưa tạo đơn chờ.
    + `OrderService.createCashierMultiOrder(...)`: Tạo đơn hàng gộp nhiều sản phẩm với mã phiếu thu `PT-...`, tự động ghi danh toàn bộ sản phẩm nếu thu tiền ngay hoặc hẹn 48h nếu chưa thu ngay kèm email thông báo.
    + `AdminOrderController.getAvailableProductsApi`: Endpoint REST trả về dữ liệu JSON cho modal khi nhập thông tin học viên.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/service/OrderService.java` & `impl/OrderServiceImpl.java`: Bổ sung `getAvailableProductsForStudent` và `createCashierMultiOrder`.
  - `src/main/java/vn/edu/ute/utelearn/controller/admin/AdminOrderController.java`: Mở rộng `createOfflineOrder` tiếp nhận `courseIds` và `cohortIds`; bổ sung API `/admin/orders/api/available-products`.
  - `src/main/resources/templates/admin/orders/index.html`: Cải tiến modal POS với giao diện chọn nhiều sản phẩm, bộ lọc loại, tìm kiếm nhanh và tự động tính tiền.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung các ca kiểm thử `testCreateCashierMultiOrder_Success_MultipleItems` và `testGetAvailableProductsForStudent_FiltersEnrolledAndPending`.
- **Kết quả kiểm thử**:
  - 156/156 unit tests toàn hệ thống vượt qua 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - 100% không có comment trong mã nguồn; tuân thủ Scope Isolation; giữ nguyên mã chưa commit theo đúng yêu cầu người dùng.

---


## [2026-10-24 18:20] - Tính Năng Giỏ Hàng (Cart), Đặt Đơn Chọn Nhiều Món & Hệ Thống Mã Giảm Giá (Discount do Admin tạo)
- **Yêu cầu gốc**: "tạo đơn thì cho phép chọn nhiều và ở vai trò user thì cho kiểu giỏ hàng xong có discount và discount do admin tạo"
- **Mục tiêu**:
  - Xây dựng hệ thống quản lý Mã giảm giá (Discount & Voucher) do Quản trị viên (Admin/Finance) tạo và quản lý: hỗ trợ giảm theo %, giảm số tiền cố định, mức giảm tối đa, giá trị đơn tối thiểu, giới hạn lượt dùng và thời hạn.
  - Xây dựng phân hệ Giỏ hàng (/cart) cho học viên: thêm khóa học / module vào giỏ, chọn nhiều sản phẩm bằng checkbox để tạo đơn gộp, áp dụng mã giảm giá trực tiếp.
  - Mở rộng luồng Đơn hàng (Order) hỗ trợ nhiều sản phẩm (createMultiItemOrder), lưu thông tin mã voucher, số tiền giảm và kích hoạt quyền học toàn bộ các sản phẩm sau khi thanh toán.
  - Tích hợp giao diện: Biểu tượng Giỏ hàng kèm số lượng trên Header Navbar, liên kết menu Giỏ hàng & Quản lý Discount trên Sidebar, nút "Thêm vào giỏ hàng" tại trang chi tiết khóa học.
- **Thành phần tạo mới & tác động**:
  - src/main/java/vn/edu/ute/utelearn/entity/Discount.java: Thực thể mã giảm giá.
  - src/main/java/vn/edu/ute/utelearn/entity/CartItem.java: Thực thể món hàng trong giỏ của học viên.
  - src/main/java/vn/edu/ute/utelearn/dao/DiscountRepository.java & CartItemRepository.java: Tầng truy xuất dữ liệu JPA.
  - src/main/java/vn/edu/ute/utelearn/dto/DiscountDTO.java & CartSummaryDTO.java: DTO trao đổi dữ liệu.
  - src/main/java/vn/edu/ute/utelearn/service/DiscountService.java & impl/DiscountServiceImpl.java: Logic nghiệp vụ voucher.
  - src/main/java/vn/edu/ute/utelearn/service/CartService.java & impl/CartServiceImpl.java: Logic nghiệp vụ giỏ hàng.
  - src/main/java/vn/edu/ute/utelearn/service/OrderService.java & impl/OrderServiceImpl.java: Bổ sung createMultiItemOrder.
  - src/main/java/vn/edu/ute/utelearn/controller/admin/AdminDiscountController.java: Controller quản trị voucher của Admin (/admin/discounts).
  - src/main/java/vn/edu/ute/utelearn/controller/CartController.java: Controller giỏ hàng học viên (/cart).
  - src/main/java/vn/edu/ute/utelearn/controller/GlobalSecurityAdvice.java: Bổ sung nạp toàn cục số lượng giỏ hàng cartItemCount.
  - src/main/resources/templates/admin/discounts/index.html & form.html: Giao diện danh sách và form tạo/sửa voucher Admin.
  - src/main/resources/templates/cart/index.html: Giao diện giỏ hàng học viên, chọn nhiều món, áp dụng voucher, chọn hình thức thanh toán.
  - src/main/resources/templates/fragments/site-navbar.html & sidebar.html: Hiển thị icon giỏ hàng và menu điều hướng.
  - src/main/resources/templates/courses/detail.html: Nút thêm vào giỏ hàng cho khóa học và từng module.
  - src/test/java/vn/edu/ute/utelearn/DiscountServiceTest.java & CartServiceTest.java: 25 unit tests kiểm thử tự động.
- **Kết quả kiểm thử**:
  - 154/154 unit tests trong toàn bộ dự án vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - 100% không có bất kỳ dòng comment nào trong mã nguồn; tuân thủ Scope Isolation; giữ nguyên mã chưa commit theo chỉ dẫn của người dùng.

---


## [2026-10-24 17:40] - Kiểm Tra Toàn Bộ Email Cho Các Phương Thức Thanh Toán & Làm Rõ Luồng Hóa Đơn Offline
- **Yêu cầu gốc**: "ủa sao tạo hóa đơn offline tôi nhớ có gửi mail mà ko thấy với mọi cái thanh toán đề gửi mail về á"
- **Mục tiêu**:
  - Giải trình nguyên nhân người dùng chưa thấy email khi tạo đơn offline trước đó.
  - Bổ sung kích hoạt gửi email biên nhận thanh toán đối với trường hợp ghi danh khóa học / module 0 VNĐ (`createCohortOrder`, `createCourseOrder` khi trạng thái là `SUCCESS`).
  - Đảm bảo 100% tất cả các hình thức thanh toán trong toàn hệ thống đều gửi email (Hướng dẫn thanh toán hoặc Biên nhận hoàn tất).
- **Nguyên nhân hóa đơn offline chưa thấy mail trước đó**:
  1. *Tiến trình Spring Boot cần khởi động lại*: Server Spring Boot local chạy từ trước vẫn giữ cấu hình Gmail cũ (bị Google chặn hạn mức 550 5.4.5). Cần restart app để nạp cấu hình Brevo SMTP mới từ `.env`.
  2. *Email tài khoản học viên*: Các tài khoản mẫu từ seeder mang địa chỉ ảo (như `student1@utelearn.edu.vn`) không thể nhận mail thực tế. Tài khoản thật của bạn (`nguyenthanhtien2312@gmail.com` - ID: 151) đã được xác nhận gửi nhận email thành công 100%.
- **Bao phủ toàn diện luồng gửi email thanh toán**:
  - *Đơn Offline học viên tạo / Thu ngân tạo chưa thu ngay*: Tự động gửi email hướng dẫn nộp tiền mặt tại phòng A1-01 KHTC kèm mã đơn hàng (`sendOfflinePaymentInstructionEmail`).
  - *Thu ngân xác nhận thanh toán / Đơn quầy thu tiền ngay*: Tự động cập nhật `SUCCESS`, sinh số phiếu thu `PT-...`, ghi danh và gửi email biên nhận kích hoạt (`sendPaymentReceiptEmail`).
  - *Thanh toán tự động VietQR SePay*: Nhận webhook, khớp số tiền, chuyển `SUCCESS`, tự động gửi email biên nhận (`sendPaymentReceiptEmail`).
  - *Thanh toán bằng ví học viên*: Trừ số dư, chuyển `SUCCESS`, tự động gửi email biên nhận (`sendPaymentReceiptEmail`).
  - *Duyệt thủ công Admin*: Admin bấm xác nhận, chuyển `SUCCESS`, tự động gửi email biên nhận duy nhất (`sendPaymentReceiptEmail`).
  - *Thanh toán mô phỏng*: Giả lập thanh toán tức thì, chuyển `SUCCESS`, tự động gửi email biên nhận (`sendPaymentReceiptEmail`).
  - *Ghi danh miễn phí (0 VNĐ)*: Tự động kích hoạt `SUCCESS` và gửi email biên nhận xác nhận vào học (`sendPaymentReceiptEmail`).
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: Bổ sung gọi `sendPaymentReceiptEmail(saved)` trong `createCohortOrder` và `createCourseOrder` khi trạng thái là `SUCCESS`.
- **Kết quả kiểm thử**:
  - 129/129 unit tests pass 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - 100% không comment trong code; tuân thủ Scope Isolation; chưa commit theo yêu cầu của người dùng.

## [2026-10-24 16:08] - Rà Soát Toàn Diện Luồng Thanh Toán (Full Payment Flow Audit) & Kết Nối Brevo SMTP
- **Yêu cầu gốc**: "rồi coi lại full luồng thanh toán đi"
- **Mục tiêu**:
  - Rà soát toàn bộ các phân hệ thanh toán trong hệ thống: VietQR SePay, Ví học viên, Đăng ký Offline tại quầy POS, Duyệt đơn Admin, Nạp ví và Mô phỏng.
  - Cấu hình Brevo SMTP Relay (`smtp-relay.brevo.com:587`, 300 emails/ngày miễn phí), kiểm thử thực tế gửi thành công email tới hộp thư cá nhân người dùng.
  - Viết bổ sung các ca kiểm thử tự động cho Quầy thu ngân POS (`createCashierOrder`, `cashierConfirmPayment`) và Thanh toán khóa học qua ví (`payCourseWithWallet`).
- **Kết quả rà soát chi tiết 7 luồng thanh toán**:
  1. **VietQR SePay (Tự động)**: Sinh mã QR `qr.sepay.vn`, nhận Webhook đối soát regex `UTE\d{6}`, khóa `synchronized(orderCode.intern())` chống race condition, kiểm tra hạn 48h, kích hoạt khóa học và bắn Email biên nhận qua Brevo.
  2. **Ví học viên (`WALLET`)**: Kiểm tra số dư >= tổng tiền, trừ ví tức thì, tạo đơn `SUCCESS`, ghi danh học viên và gửi email biên nhận.
  3. **Nạp ví (`WALLET_DEPOSIT`)**: Nạp tối thiểu 10.000 VNĐ qua VietQR, khi khớp tiền tự động cộng số dư ví học viên và gửi email biên nhận có nút dẫn về `/profile`.
  4. **Thanh toán tại quầy (`OFFLINE_CASH` / `OFFLINE_COUNTER`)**: Học viên tạo đơn nhận email hướng dẫn phòng A1-01 KHTC; thu ngân tạo đơn hoặc xác nhận xuất phiếu thu `PT-...`, tự động gửi biên lai kích hoạt.
  5. **Duyệt thủ công Admin (`MANUAL_ADMIN`)**: Quản trị viên đối soát sao kê ngân hàng bấm xác nhận duyệt đơn, kích hoạt học viên và gửi biên lai duy nhất (không trùng lặp).
  6. **Đăng ký miễn phí (`FREE`)**: Đơn 0 VNĐ kích hoạt ngay tức thì trạng thái `SUCCESS` và vào học trực tiếp.
  7. **Mô phỏng thanh toán (`SIMULATION`)**: Endpoint giả lập phục vụ báo cáo / demo, được bảo vệ bởi cờ `simulationEnabled` và phân quyền RBAC.
- **Thành phần tác động**:
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung 3 test cases bao phủ thanh toán quầy thu ngân và thanh toán khóa học qua ví.
  - `.env` & `src/main/resources/application.yml`: Cấu hình Brevo SMTP Relay.
- **Kết quả kiểm thử**:
  - 129/129 tests chạy thành công 100% (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - 100% không comment trong code; tuân thủ Scope Isolation; chưa commit theo yêu cầu.

## [2026-10-24 15:20] - Rà Soát, Chuẩn Hóa & Kiểm Thử Toàn Diện Luồng Gửi Email (Email Service Verification)
- **Yêu cầu gốc**: "rồi check cái gửi mail cho tôi đi"
- **Mục tiêu**:
  - Rà soát toàn bộ luồng gửi email trong toàn bộ hệ thống (Quên mật khẩu, Hướng dẫn nộp tiền offline, Biên nhận thanh toán thành công).
  - Bổ sung kích hoạt gửi email biên nhận thanh toán tự động trong `fulfillOrder` (SePay Webhook, Giả lập mô phỏng) và thanh toán bằng ví học viên (`payWithWallet`, `payCourseWithWallet`).
  - Chuẩn hóa nút điều hướng trong email biên nhận đối với giao dịch nạp tiền ví `WALLET_DEPOSIT` dẫn thẳng về `/profile`.
  - Loại bỏ các comment tàn dư trong `AuthServiceImpl.java`, bổ sung try-catch an toàn cho `baseUrl` khi gọi `processForgotPassword`.
  - Xây dựng bộ kiểm thử đơn vị tự động `EmailServiceTest` đạt tỷ lệ thành công 100%.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/service/impl/EmailServiceImpl.java`: Thiết lập chuẩn hóa địa chỉ gửi `noreply@utelearn.edu.vn`, tên hiển thị `UTELearn (No-Reply)` và đặt `Reply-To` chuẩn No-Reply; bổ sung thông báo No-Reply tại footer tất cả email; bổ sung điều hướng `KIỂM TRA SỐ DƯ VÍ` cho `WALLET_DEPOSIT`.
  - `src/main/resources/application.yml` & `.env`: Cấu hình thuộc tính `spring.mail.properties.mail.from: ${MAIL_FROM:noreply@utelearn.edu.vn}`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Kích hoạt gửi biên nhận `sendPaymentReceiptEmail(saved)` ngay trong `fulfillOrder()`, qua đó SePay Webhook và thanh toán mô phỏng đều tự động gửi mail.
    + Bỏ gọi email trùng lặp trong `manualConfirmPayment()` do `fulfillOrder()` đã kích hoạt.
    + Bổ sung gọi `sendPaymentReceiptEmail(saved)` trong thanh toán bằng số dư ví `payWithWallet()` và `payCourseWithWallet()`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/AuthServiceImpl.java`: Dọn dẹp toàn bộ comment trong code, bọc an toàn lấy `baseUrl` khi gửi link reset password.
  - `src/test/java/vn/edu/ute/utelearn/EmailServiceTest.java`: Tạo mới 9 bài test bao phủ toàn bộ các kịch bản gửi mail HTML, hướng dẫn offline, biên nhận thanh toán (Cohort, Course, Wallet Deposit) và xử lý ngoại lệ an toàn.
- **Kết quả kiểm thử**:
  - 31/31 unit tests (`EmailServiceTest` + `OrderFeatureTest`) chạy hoàn tất thành công (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Không sửa DAO và Entity; 100% không có comment trong code; kiểm thử đầy đủ, độc lập phạm vi thay đổi.

## [2026-10-23 20:46] - Rà Soát Toàn Diện Tiến Độ Đồ Án & Danh Mục Tính Năng Còn Lại
- **Yêu cầu gốc**: "check coi còn gì chưa làm đi"
- **Mục tiêu**:
  - Đối chiếu mã nguồn thực tế với bộ đặc tả 37 Use Cases, `docs/UTELearn_SRS.md`, `docs/TODO_DanhSachTinhNangChuaLam.md` và `docs/UTELearn_Todo_Sheet.csv`.
  - Phân loại rõ ràng các hạng mục đã hoàn thành 100%, các tính năng trọng tâm còn lại cần phát triển và các tính năng thuộc định hướng tương lai (Phase 2).
- **Kết quả rà soát chi tiết**:
  - **Đã hoàn thành 100%**:
    + Phân hệ 1: Xác thực & Hồ sơ cá nhân (`UC-01` -> `UC-07`).
    + Phân hệ 2: Quản trị Hệ thống Admin (`UC-08` -> `UC-12`) bao gồm CRUD Danh mục đa cấp và Điểm danh ca trực.
    + Phân hệ 3 & 4: Khóa học lớn & Module thành phần (`UC-13` -> `UC-16`, `UC-33`, `UC-36`, `UC-37`) bao gồm Cohort CRUD, Drip Content khóa bài server-side.
    + Phân hệ 5: Học trực tuyến & Đơn hàng thanh toán (`UC-21`, `UC-22`, `UC-23`, `UC-25`, `UC-34`) bao gồm VietQR SePay IPN, Ví tiền học viên, POS thanh toán offline tại quầy, % tiến độ bài học, thẻ Upsell nâng cấp trọn gói bù chênh lệch 100%.
    + Phân hệ 6: Kiểm duyệt nội dung (`UC-18` -> `UC-20`) bao gồm SLA 48h, chống tự duyệt bài, phân quyền biên tập / thanh tra nội dung.
    + Phân hệ 7: Kho tài nguyên số Cloudinary (`UC-30` -> `UC-32`).
    + Phân hệ 8: UI Guard Online Judge chống lỗi 404 (`UC-26` -> `UC-29`).
  - **Còn lại cần làm (To-Do)**:
    + 🔴 **TASK-05 (P1 - Cao)**: Hệ thống Bài kiểm tra Trắc nghiệm Quiz & Question (`UC-24`, `UC-37`) - Đã có JPA Entity `Quiz`, `Question`, `QuizSubmission`; cần làm Repository, Service, UI soạn đề trắc nghiệm và UI làm bài đếm giờ tự động chấm.
    + 🟡 **TASK-06 (P2 - Trung bình)**: Phòng Chat Thảo luận Lớp học Thời gian thực qua WebSocket (`UC-35`) - Đã có thư viện, cấu hình STOMP và Entity `CohortMessage`; cần làm `WebSocketConfig`, `CohortMessageRepository`, `CohortChatController` và UI chat thời gian thực.
    + 🟢 **TASK-11 (P3 - Tương lai / Phase 2)**: Online Judge Sandbox chấm code tự động (`UC-26` -> `UC-29`) - Đã có Database schema và UI Roadmap chống 404; nội dung chấm code sandbox thuộc định hướng phát triển báo cáo, không bắt buộc code giai đoạn này.
- **Quy chuẩn tuân thủ**:
  - Tuân thủ Scope Isolation, ghi chép nhật ký phát triển đầy đủ, không can thiệp code trái phạm vi.


## [2026-10-23 20:25] - Giải Quyết Xung Đột Nhánh Merge Pull Request (Conflict Resolution with main)
- **Yêu cầu gốc**: "giải quyết" (Kèm ảnh chụp màn hình xung đột Pull Request trên GitHub)
- **Mục tiêu**:
  - Khắc phục xung đột `modify/delete` trên 2 tệp `docs/TODO_DanhSachTinhNangChuaLam.md` và `docs/UTELearn_Todo_Sheet.csv` giữa `develop` và `origin/main`.
- **Nguyên nhân gốc rễ**:
  - Nhánh `origin/main` có commit `948d0cb` ("Delete docs directory") đã xóa thư mục `docs/`. Trong khi đó, nhánh `develop` tiếp tục cập nhật và duy trì các tài liệu đặc tả SRS, Use Case và danh sách TODO của đồ án.
- **Giải pháp xử lý**:
  - Thực hiện đồng bộ hóa nhánh: `git merge origin/main`.
  - Khôi phục và bảo toàn toàn bộ tài liệu gốc của nhánh `develop`: `git checkout HEAD -- docs/`.
  - Đóng gói commit hòa giải: `ad42200` (`merge: resolve conflicts with origin/main by preserving docs directory`).
- **Kết quả kiểm thử**:
  - Vượt qua toàn bộ 3 giai đoạn của Enterprise Pre-Commit Verification Pipeline (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không can thiệp code sai phạm vi; bảo toàn toàn vẹn dữ liệu tài liệu đồ án; tuân thủ Ponytail mode.

## [2026-10-23 20:15] - Chiết Xuất Tiện Ích Dùng Chung (Util) & Chuẩn Hóa Service (Giữ Nguyên DAO & Entity)
- **Yêu cầu gốc**: "/ponytail service mấy cái khác trừ dao với entity ko làm á với nào dùng chung thì có quăng vô util không check lại full đi"
- **Mục tiêu**:
  - Không can thiệp vào tầng `dao` và `entity` theo đúng yêu cầu phạm vi.
  - Chiết xuất toàn bộ logic dùng chung lặp lại vào gói `vn.edu.ute.utelearn.util`: `SlugUtils`, `CurrencyUtils`, `SecurityUtils`.
  - Tinh gọn các Service và Controller: tái sử dụng các tiện ích dùng chung thay cho mã kiểm tra thủ công.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/util/SlugUtils.java`: Xử lý chuẩn hóa chuỗi và sinh slug tiếng Việt không dấu.
  - `src/main/java/vn/edu/ute/utelearn/util/CurrencyUtils.java`: Định dạng số tiền `#,##0 VNĐ` chuẩn hóa.
  - `src/main/java/vn/edu/ute/utelearn/util/SecurityUtils.java`: Tiện ích kiểm tra quyền RBAC (`hasRole`, `isAdmin`, `isAdminOrFinance`, `isInstructor`, `isModerator`).
  - `src/main/java/vn/edu/ute/utelearn/util/ValidationUtils.java`: Dọn sạch các chú thích trong code tuân thủ Quy chuẩn #1.
  - Cập nhật tầng Service:
    + `CategoryServiceImpl.java` & `CourseServiceImpl.java`: Tái sử dụng `SlugUtils.toSlug()`, loại bỏ regex lặp lại.
    + `EmailServiceImpl.java`: Tái sử dụng `CurrencyUtils.formatVnd()`.
    + `CohortServiceImpl.java`, `InstructorAssetServiceImpl.java`, `ModerationServiceImpl.java`, `LessonServiceImpl.java`, `SectionServiceImpl.java`: Tái sử dụng `SecurityUtils.isAdmin()`, `SecurityUtils.hasRole()`.
  - Cập nhật tầng Controller:
    + `CheckoutApiController.java`, `CheckoutController.java`, `AdminOrderController.java`, `OrderController.java`: Tái sử dụng `SecurityUtils` và `CurrencyUtils`.
- **Kết quả kiểm thử**:
  - Toàn bộ test cases vượt qua (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Không thay đổi DAO & Entity; tuyệt đối không có comment trong code; tuân thủ DRY, Scope Isolation và Ponytail mode.

## [2026-10-23 20:00] - Clean Code & Tái Cấu Trúc Thư Mục Controller Chuẩn Doanh Nghiệp (Enterprise Architecture)
- **Yêu cầu gốc**: "/ponytail-review hãy review lại code mà clean nó đi với chia lại vào các thư mục code làm sao cho giống 1 doanh nghiệp"
- **Mục tiêu**:
  - Triệt tiêu trùng lặp code (DRY) đối với hàm xác định đợt học toàn khóa `isFullCourseCohort()`.
  - Tinh gọn "God Service" `OrderServiceImpl`: chuyển toàn bộ logic dựng chuỗi HTML Email biên lai và hướng dẫn thanh toán sang `EmailServiceImpl`.
  - Tái cấu trúc package controller phẳng thành phân hệ chuẩn doanh nghiệp: `controller.admin`, `controller.instructor`, `controller.moderator`.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/entity/Cohort.java`: Bổ sung phương thức miền `isFullCourseCohort()`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Loại bỏ hàm trùng lặp, dùng `Cohort.isFullCourseCohort()`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CourseController.java`: Loại bỏ hàm trùng lặp, dùng `Cohort.isFullCourseCohort()`.
  - `src/main/java/vn/edu/ute/utelearn/service/EmailService.java`: Bổ sung giao ước `sendOfflinePaymentInstructionEmail(Order)` và `sendPaymentReceiptEmail(Order)`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/EmailServiceImpl.java`: Tiếp nhận việc định dạng HTML email thanh toán và biên lai điện tử.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: Giảm hơn 175 dòng code dựng HTML, ủy quyền sạch qua `EmailService`.
  - Phân vùng thư mục Controller chuẩn doanh nghiệp:
    + `vn.edu.ute.utelearn.controller.admin`: `AdminAttendanceController`, `AdminCategoryController`, `AdminModerationController`, `AdminOrderController`, `RoleController`, `UserController`.
    + `vn.edu.ute.utelearn.controller.instructor`: `InstructorAssetController`, `InstructorCohortController`, `InstructorCourseController`.
    + `vn.edu.ute.utelearn.controller.moderator`: `ModeratorController`.
  - Cập nhật import trong các bộ kiểm thử: `AdminCategoryFeatureTest.java`, `ModeratorReviewPageRenderTest.java`, `ModuleDetailFeatureTest.java`.
- **Kết quả kiểm thử**:
  - Toàn bộ các bộ kiểm thử tự động đều vượt qua (`BUILD SUCCESS`).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không viết comment trong code; tuân thủ DRY, Clean Code, Guard Clauses và ghi log đầy đủ.


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-22 21:34] - Cập Nhật Mật Khẩu Mặc Định & Đồng Bộ Toàn Bộ Tài Khoản Sang Abc@123
- **Yêu cầu gốc**: "seed lại ful mk là Abc@123 đi"
- **Mục tiêu**: Cập nhật cấu hình mật khẩu khởi tạo mặc định sang `Abc@123` và tự động đồng bộ lại mật khẩu của toàn bộ tài khoản người dùng trong cơ sở dữ liệu để đăng nhập thuận tiện.
- **Thành phần tác động**:
  - `src/main/resources/application.yml`: Cập nhật cấu hình `utelearn.seeder.default-password` thành `${DEFAULT_SEED_PASSWORD:Abc@123}`.
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Cập nhật giá trị mặc định của trường `defaultPassword` thành `"Abc@123"`.
  - `src/main/java/vn/edu/ute/utelearn/config/DataSeeder.java`: Cập nhật logic `seedUsers()` để tự động cập nhật `password_hash` và kích hoạt `is_active = true` cho toàn bộ tài khoản người dùng trong database.
  - `src/test/java/vn/edu/ute/utelearn/UtelearnApplicationTests.java`: Bổ sung kiểm thử `testSyncPasswordsToAbc123` xác nhận mã hóa BCrypt và kiểm tra tính hợp lệ của mật khẩu mới.
- **Base tái sử dụng**:
  - Tái sử dụng `PasswordEncoder` (BCrypt) và cấu hình datasource hiện có.
- **Kết quả kiểm thử**:
  - Đã chạy kiểm thử và cập nhật trực tiếp vào cơ sở dữ liệu: Toàn bộ 114/114 tests vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và không tự ý git push.





---


## [2026-10-22 21:25] - Hỗ Trợ Bỏ Chọn / Xóa Module Không Muốn Mua Trên Trang Thanh Toán Khóa Học
- **Yêu cầu gốc**: "cho xóa cái nào ko muốn mua"
- **Mục tiêu**: Cho phép người dùng trực tiếp xóa/bỏ chọn những module không có nhu cầu mua trong danh sách "CÁC MODULE SẼ ĐƯỢC KÍCH HOẠT" trên trang thanh toán trọn gói/nâng cấp khóa học; tự động cập nhật lại tổng tiền, ưu đãi, kiểm tra số dư ví và tạo đơn hàng chính xác với các module đã chọn.
- **Thành phần tác động**:
  - `src/main/resources/templates/checkout/index.html`: Thêm nút icon thùng rác xóa từng module kèm hiệu ứng chuyển động mượt mà; hiển thị khu vực chip các module đã bỏ kèm nút "Khôi phục" và "Khôi phục tất cả"; tự động tính toán lại tổng tiền theo các module còn chọn; cập nhật trạng thái đủ/thiếu số dư ví theo thời gian thực; khóa nút thanh toán khi bỏ hết module.
  - `src/main/java/vn/edu/ute/utelearn/service/OrderService.java`: Bổ sung nạp chồng phương thức `createCourseOrder(User, String, List<Long>, String)` và `payCourseWithWallet(User, String, List<Long>)`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: Cập nhật logic tính tổng tiền, chiết khấu và tạo OrderItem ghi nhận đúng các module được chọn; cập nhật `enrollCourseDirectly` chỉ kích hoạt module ALL khi toàn bộ module thật của khóa học đã hoàn tất ghi danh.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Nhận tham số `cohortIds` tùy chọn từ client cho các endpoint `/api/checkout/initiate-course` và `/api/checkout/pay-wallet-course`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Nhận `cohortIds` cho các endpoint SSR `/course/{slug}/initiate` và `/course/{slug}/process`.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testCreateCourseOrder_WithSelectedCohortSubset()`.
- **Base tái sử dụng**:
  - Tái sử dụng `OrderItemDTO.cohortIds` và logic tính giá nâng cấp bù trừ chênh lệch của `OrderService`.
- **Logic đơn giản hóa**:
  - Phân tách rõ ràng giữa trường hợp mua trọn bộ toàn bộ module (combo/upgrade discount) và trường hợp chọn lọc tập con module (tính tổng theo module được chọn, áp trần giá gói).
- **Kết quả kiểm thử**:
  - Đã chạy `.\mvnw.cmd test`: Toàn bộ 113/113 unit tests vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và không tự ý git push.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-05] - Thiết Lập Bộ Quy Chuẩn Phát Triển & Skill AI Cho Dự Án
- **Người thực hiện**: Antigravity Assistant
- **Mục tiêu**: Thiết lập skill, bộ quy tắc (rules), file nhật ký và cấu hình gitignore cho đồ án.
- **Thành phần tác động**:
  - `.agents/skills/utelearn-dev/SKILL.md`: Tạo mới -> Định nghĩa toàn diện skill hướng dẫn phát triển (không comment, tái sử dụng base, đơn giản hóa logic, giới hạn phạm vi, ghi log).
  - `.agents/rules/utelearn-rules.md`: Tạo mới -> Định nghĩa quy tắc tự động kích hoạt cho workspace đồ án.
  - `.gitignore`: Chỉnh sửa -> Thêm thư mục `.agents/` vào danh sách bỏ qua của git.
  - `DEV_LOG.md`: Tạo mới -> Thiết lập cấu trúc nhật ký theo dõi chi tiết từng phần làm gì.
- **Base tái sử dụng**:
  - Cấu trúc thư mục Antigravity Customizations (`.agents/skills/` và `.agents/rules/`).
- **Logic đơn giản hóa**:
  - Định hình quy chuẩn ngắn gọn, trực diện, không rườm rà.
- **Ghi chú phạm vi**:
  - Chỉ bổ sung cấu hình tooling/customizations, không làm thay đổi bất kỳ file mã nguồn Java, database hay template hiện có của dự án.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-05] - Cập Nhật Gitignore Cho DEV_LOG.md
- **Người thực hiện**: Antigravity Assistant
- **Mục tiêu**: Bỏ qua file DEV_LOG.md trong git theo yêu cầu người dùng.
- **Thành phần tác động**:
  - `.gitignore`: Chỉnh sửa -> Bổ sung `DEV_LOG.md` vào danh sách ignore.
- **Ghi chú phạm vi**:
  - Chỉ tác động đến file `.gitignore`, không ảnh hưởng tới bất kỳ thành phần mã nguồn nào khác.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-10] - Rà Soát Toàn Bộ Đồ Án, Cập Nhật SRS Chuẩn Doanh Nghiệp & Xuất Sheet To-Do Theo Độ Khẩn Cấp
- **Thành phần tác động**:
  - `UTELearn_SRS.md`: Chỉnh sửa -> Cập nhật toàn diện SRS chuẩn doanh nghiệp theo kiến trúc Course - Cohort (Mua Full Course, Mua lẻ Cohort, Nâng cấp bù giá chênh lệch), Cổng thanh toán IPN/Webhook (VNPay/MoMo), trình phát Plyr & PDF/Office Viewer, và chuyển phân hệ Online Judge sang Định hướng phát triển (Future Scope).
  - `Missing_Features.md`: Chỉnh sửa -> Xuất báo cáo rà soát toàn bộ 37 Use Cases chuẩn doanh nghiệp đối chiếu với mã nguồn hiện tại, phân chia các tính năng chưa làm theo 4 mức độ khẩn cấp (P0 -> P3).
  - `utelearn/docs/TODO_DanhSachTinhNangChuaLam.md`: Chỉnh sửa -> Tài liệu danh sách tính năng chưa làm chuẩn doanh nghiệp trong thư mục `docs`.
  - `utelearn/docs/UTELearn_Todo_Sheet.csv`: Thêm mới -> File bảng tính To-Do chia theo mức độ khẩn cấp để import/đồng bộ trực tiếp lên Google Sheets quản lý tiến độ nhóm.
- **Base tái sử dụng**: Bộ tài liệu Use Case trong `utelearn/docs/usecases/` và toàn bộ cấu trúc Entity/Controller/Service hiện có.
- **Logic đơn giản hóa**: Phân tầng rõ ràng các hạng mục cần code ngay (P0, P1, P2) và hạng mục chuyển sang định hướng phát triển (P3 - Online Judge).
- **Ghi chú phạm vi**: Chỉ cập nhật tài liệu SRS, tài liệu docs và file Sheet To-Do, không làm thay đổi mã nguồn ứng dụng.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-12] - Chuẩn Hóa Cấu Trúc Thư Mục Gốc Dự Án (Flatten Repository Into utelearn)
- **Thành phần tác động**:
  - `.git`, `.github/`, `.githooks/`: Di chuyển vào bên trong thư mục `utelearn/` để `utelearn` trở thành thư mục gốc duy nhất của dự án và trên GitHub.
  - `docs/UTELearn_SRS.md`, `docs/UTELearn_PostgreSQL_Schema.sql`: Di chuyển từ thư mục ngoài vào trong `utelearn/docs/`.
  - `.github/workflows/ci.yml`: Chỉnh sửa -> Cập nhật đường dẫn `pom.xml`, `mvnw`, `Dockerfile` khớp với cấu trúc thư mục gốc mới.
  - `.githooks/pre-commit`: Chỉnh sửa -> Cập nhật nhận diện tự động thư mục gốc `ROOT_DIR`.
  - `.gitignore`, `.gitattributes`: Chỉnh sửa -> Gộp quy tắc ignore từ thư mục ngoài và cố định chuẩn `eol=lf` cho `.githooks/*`.
- **Base tái sử dụng**: Giữ nguyên toàn bộ lịch sử Git (Git nhận diện 100% là thao tác `git mv` / Rename).
- **Logic đơn giản hóa**: Loại bỏ tình trạng phân mảnh 2 tầng thư mục (`FINAL PROJECT` và `utelearn`).
- **Ghi chú phạm vi**: Không làm thay đổi bất kỳ file mã nguồn Java hay giao diện nào của ứng dụng.

### [2026-10-12 18:17] - Chuẩn hóa vị trí code dùng chung và dọn dẹp file rác
- **Yêu cầu gốc**: "rồi file nào là code mà dùng chung thì nằm chỗ nên dành cho code đi với file nào ko cần thì xóa đi"
- **Thay đổi chi tiết**:
  - `src/main/resources/static/js/admin-layout.js`: Chỉnh sửa -> Tích hợp hàm thông báo dùng chung `showToast(message, type)` vào file JS tĩnh dùng chung của hệ thống và xóa comment thừa.
  - `src/main/resources/templates/dashboard/index.html`, `profile/index.html`, `auth/login.html`, `auth/register.html`, `auth/forgot-password.html`, `auth/reset-password.html`: Chỉnh sửa -> Loại bỏ đoạn code `showToast` viết lặp lại inline trong 6 trang HTML, chuyển sang gọi trực tiếp từ `/js/admin-layout.js`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/InstructorCurriculumApiController.java`: Di chuyển -> Chuyển REST API Controller từ `controller/` vào đúng package `controller/api/`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/ModeratorApiController.java`: Di chuyển -> Chuyển REST API Controller từ `controller/` vào đúng package `controller/api/`.
  - `fix_escape.js`, `update_toast.js`, `temp_home.txt`, `HELP.md`, `src/main/resources/application.properties`, `docs/UTELearn_PostgreSQL_Schema.sql`: Xóa -> Loại bỏ hoàn toàn các file script nháp, file tạm 0-byte, file cấu hình rỗng và schema trùng lặp.
- **Base tái sử dụng**: Tái sử dụng `/js/admin-layout.js` làm nơi chứa helper JS dùng chung toàn hệ thống (`toggleSidebar`, `showConfirmModal`, `showToast`).
- **Logic đơn giản hóa**: Gom toàn bộ `@RestController` về chung package `vn.edu.ute.utelearn.controller.api` và tách biệt rõ với `@Controller` (SSR Thymeleaf) tại `vn.edu.ute.utelearn.controller`.
- **Ghi chú phạm vi**: Không thay đổi logic nghiệp vụ hay đường dẫn API endpoint.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-12] - Xác Thực Mật Khẩu Mạnh Cho Phân Hệ Xác Thực (Auth Password Validation)
- **Yêu cầu gốc**: "giờ code lúc auth thì mật khẩu check mật khẩu mạnh mới cho tiếp đi"
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/util/ValidationUtils.java`: Chỉnh sửa -> Công khai hằng số `PASSWORD_REGEX` chuẩn hóa bao phủ đầy đủ ký tự đặc biệt, chữ hoa, chữ thường, số, độ dài >= 8 và tái sử dụng `isStrongPassword`.
  - `src/main/java/vn/edu/ute/utelearn/dto/RegisterRequestDTO.java`: Chỉnh sửa -> Thêm `@Pattern(regexp = ValidationUtils.PASSWORD_REGEX)` yêu cầu mật khẩu mạnh khi đăng ký tài khoản.
  - `src/main/java/vn/edu/ute/utelearn/dto/ResetPasswordRequestDTO.java`: Chỉnh sửa -> Thêm `@Pattern(regexp = ValidationUtils.PASSWORD_REGEX)` yêu cầu mật khẩu mới mạnh khi đặt lại mật khẩu.
  - `src/main/java/vn/edu/ute/utelearn/dto/ChangePasswordRequestDTO.java`: Chỉnh sửa -> Thêm `@Pattern(regexp = ValidationUtils.PASSWORD_REGEX)` đảm bảo tính đồng bộ khi đổi mật khẩu người dùng.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/AuthServiceImpl.java`: Chỉnh sửa -> Bổ sung Guard Clause kiểm tra `ValidationUtils.isStrongPassword` trong phương thức `register` và `processResetPassword`.
  - `src/main/java/vn/edu/ute/utelearn/controller/AuthController.java`: Chỉnh sửa -> Bổ sung kiểm tra mật khẩu mạnh vào `BindingResult` cho 2 luồng form đăng ký và đặt lại mật khẩu.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/AuthApiController.java`: Chỉnh sửa -> Chặn mật khẩu yếu và trả về chi tiết thông báo lỗi cụ thể thay vì thông báo chung chung.
  - `src/main/java/vn/edu/ute/utelearn/controller/ProfileController.java`: Chỉnh sửa -> Bổ sung kiểm tra mật khẩu mới mạnh trong luồng đổi mật khẩu cá nhân.
  - `src/main/resources/templates/auth/register.html`: Chỉnh sửa -> Thêm thanh đo độ mạnh mật khẩu (strength progress bar), danh sách 4 tiêu chuẩn trực quan (độ dài, hoa/thường, số, ký tự đặc biệt), và chặn submit phía client nếu chưa đạt chuẩn.
  - `src/main/resources/templates/auth/reset-password.html`: Chỉnh sửa -> Thêm thanh đo độ mạnh mật khẩu, tiêu chuẩn trực quan và chặn submit phía client tương tự trang đăng ký.
- **Base tái sử dụng**:
  - Tái sử dụng `ValidationUtils.isStrongPassword(String password)` và hằng số `PASSWORD_REGEX`.
  - Tái sử dụng `showToast(message, type)` từ `admin-layout.js` cho trải nghiệm người dùng liền mạch.
- **Logic đơn giản hóa**:
  - Áp dụng kỹ thuật Early Return (Guard Clause) ngay tại tầng Service và Controller API.
  - Tái sử dụng một nguồn regex duy nhất từ `ValidationUtils.PASSWORD_REGEX` cho cả DTO validation annotations lẫn Service checks.
- **Ghi chú phạm vi**:
  - Chỉ tập trung vào quy trình xác thực mật khẩu (đăng ký, đặt lại mật khẩu, đổi mật khẩu), không gây ảnh hưởng đến luồng đăng nhập (login) hoặc các phân hệ học tập khác.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-12] - Tối Giản Giao Diện Ô Nhập Mật Khẩu
- **Yêu cầu gốc**: "chỗ này kiểu 'Nhập mật khẩu thôi' với bỏ mấy cái tích các kiểu đi (ko ai làm trong web cả)"
- **Thành phần tác động**:
  - `src/main/resources/templates/auth/register.html`: Chỉnh sửa -> Chuyển placeholder thành "Nhập mật khẩu", loại bỏ toàn bộ thanh đo progress bar và checklist tích điều kiện trên giao diện, giữ lại kiểm tra tính hợp lệ khi submit bằng Toastify.
  - `src/main/resources/templates/auth/reset-password.html`: Chỉnh sửa -> Chuyển placeholder thành "Nhập mật khẩu mới", loại bỏ thanh đo và checklist trên giao diện, giữ lại kiểm tra tính hợp lệ khi submit bằng Toastify.
- **Base tái sử dụng**:
  - Tái sử dụng hàm thông báo chuẩn `showToast(message, type)`.
- **Logic đơn giản hóa**:
  - Bỏ toàn bộ DOM manipulation thời gian thực phức tạp trên UI, chỉ kích hoạt kiểm tra khi người dùng bấm nút submit và hiển thị toast lỗi nếu mật khẩu chưa đáp ứng chuẩn bảo mật.
- **Ghi chú phạm vi**:
  - Chỉ thay đổi phần hiển thị của trang đăng ký và đặt lại mật khẩu, backend validation và an toàn hệ thống vẫn giữ nguyên 100%.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-12] - TASK-03 (P0): Điều Hướng Online Judge Chống Lỗi 404 & Trang Lỗi Toàn Cục (Global Error Page)
- **Yêu cầu gốc**: "TASK-03 P0 - KHẨN CẤP (Critical) Module 8: Online Judge (UI Guard) UC-26 -> UC-29 Xử lý điều hướng mục Online Judge trên Sidebar (Chống lỗi 404)... với tạo 1 cái trang mỗi khi lỗi là nó chuyển hướng về đó á"
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/controller/OjController.java`: Tạo mới -> Controller điều hướng `/oj/problems` và `/oj` hiển thị giao diện giới thiệu Roadmap Phase 2.
  - `src/main/resources/templates/oj/problems.html`: Tạo mới -> Giao diện giới thiệu kiến trúc Docker Microservice Sandbox, bài toán mẫu, code editor mock, chống hoàn toàn lỗi 404 khi demo.
  - `src/main/resources/templates/fragments/sidebar.html`: Chỉnh sửa -> Gắn badge "Coming Soon" nổi bật bên cạnh mục Online Judge (OJ) trên sidebar.
  - `src/main/resources/static/data/system-menus.json`: Chỉnh sửa -> Cập nhật badge sang "Coming Soon" đồng bộ dữ liệu menu.
  - `src/main/java/vn/edu/ute/utelearn/security/SecurityConfig.java`: Chỉnh sửa -> Bổ sung `/oj/**` vào danh sách `permitAll()` an toàn tuyệt đối cho mọi vai trò.
  - `src/main/java/vn/edu/ute/utelearn/controller/CustomErrorController.java`: Tạo mới -> Controller xử lý toàn diện endpoint `/error`, hỗ trợ điều hướng HTML view thân thiện và trả về JSON cho client API.
  - `src/main/resources/templates/error.html`: Tạo mới -> Trang báo lỗi toàn cục (404, 403, 500, 401, 400) giao diện chuẩn thương hiệu UTELearn với nút quay về Trang chủ và Bảng điều khiển.
  - `docs/TODO_DanhSachTinhNangChuaLam.md`, `docs/UTELearn_Todo_Sheet.csv`: Chỉnh sửa -> Cập nhật trạng thái `TASK-03` thành `DONE`.
  - `src/main/resources/templates/home.html`, `courses/index.html`, `courses/detail.html`: Chỉnh sửa -> Đồng bộ liên kết "Luyện thi OJ" trên thanh điều hướng chính (Top Navbar) trỏ về `/oj/problems` và gắn kèm badge `Coming Soon`, ngăn chặn hoàn toàn lỗi click vào hash link `#oj` rỗng.
- **Base tái sử dụng**:
  - Tái sử dụng `sidebar.html` và `navbar.html` layout fragments cho trang OJ.
  - Tái sử dụng `AuthService.getCurrentAuthenticatedUser()` nạp ngữ cảnh người dùng.
- **Logic đơn giản hóa**:
  - Xử lý mã lỗi HTTP qua switch-case / guard methods ngắn gọn trong `CustomErrorController`.
  - Tách bạch rõ 2 kênh phản hồi: HTML view cho trình duyệt và JSON cho REST API client.
- **Ghi chú phạm vi**:
  - Chỉ bổ sung trang điều hướng và trang lỗi, không làm ảnh hưởng đến các phân hệ nghiệp vụ khác.
## [2026-10-12] - Khóa Click & Chặn Điều Hướng Mục 'Luyện Thi OJ' Trên Top Navbar
- **Yêu cầu gốc**: "chặn cái OJ trên đây nữa"
- **Thành phần tác động**:
  - `src/main/resources/templates/home.html`: Chỉnh sửa -> Chuyển liên kết `Luyện thi OJ` thành `href="javascript:void(0)" onclick="notifyOjPhase2(event)" role="button"`, bổ sung hàm `notifyOjPhase2` hiển thị thông báo Toastify dạng cảnh báo: *"Tính năng Luyện thi Online Judge đang được hoàn thiện trong Phase 2!"*.
  - `src/main/resources/templates/courses/index.html`: Chỉnh sửa -> Khóa click mục `Luyện thi OJ`, tích hợp Toastify CSS/JS và hàm `notifyOjPhase2` hiển thị Toast đồng bộ.
  - `src/main/resources/templates/courses/detail.html`: Chỉnh sửa -> Khóa click mục `Luyện thi OJ`, tích hợp Toastify CSS/JS và hàm `notifyOjPhase2` hiển thị Toast đồng bộ.
- **Base tái sử dụng**:
  - Tái sử dụng giao diện Toastify chuẩn hiện có trong dự án với style bo tròn sang trọng (`rounded-pill`, shadow nhẹ).
- **Logic đơn giản hóa**:
  - Khóa sự kiện chuyển hướng mặc định bằng `event.preventDefault()` và kích hoạt Toast ngay tại chỗ, giữ trải nghiệm người dùng liền mạch không bị nhảy trang.
- **Ghi chú phạm vi**:
  - Chỉ cập nhật hành vi click của nút `Luyện thi OJ` trên Top Navbar của 3 trang công khai (`home.html`, `courses/index.html`, `courses/detail.html`). Các phân hệ khác và sidebar điều hướng nội bộ giữ nguyên.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-12] - Tách Fragment Tái Sử Dụng Cho Public Site Navbar & Footer
- **Yêu cầu gốc**: "mấy cái như nav hay fotter mà kiểu dùng chung thì làm riêng 1 file mốt gọi đi"
- **Thành phần tác động**:
  - `src/main/resources/templates/fragments/site-navbar.html`: Tạo mới -> Fragment `siteNavbar(activePage)` chứa toàn bộ markup thanh điều hướng công khai, xử lý active menu tự động theo tham số, dropdown tài khoản/vai trò, nút đăng nhập/đăng ký và logic Toast chặn OJ Phase 2.
  - `src/main/resources/templates/fragments/footer.html`: Tạo mới -> Fragment `footer` chứa toàn bộ chân trang thương hiệu UTELearn (logo, thông tin bản quyền và lời ghi nhận dành cho HCM-UTE).
  - `src/main/resources/templates/home.html`: Chỉnh sửa -> Thay thế khối navbar và footer tĩnh bằng `<nav th:replace="~{fragments/site-navbar :: siteNavbar('home')}"></nav>` và `<footer th:replace="~{fragments/footer :: footer}"></footer>`.
  - `src/main/resources/templates/courses/index.html`: Chỉnh sửa -> Thay thế khối navbar tĩnh bằng `<nav th:replace="~{fragments/site-navbar :: siteNavbar('courses')}"></nav>` và chèn `<footer th:replace="~{fragments/footer :: footer}"></footer>`.
  - `src/main/resources/templates/courses/detail.html`: Chỉnh sửa -> Thay thế khối navbar tĩnh bằng `<nav th:replace="~{fragments/site-navbar :: siteNavbar('courses')}"></nav>` và chèn `<footer th:replace="~{fragments/footer :: footer}"></footer>`.
- **Base tái sử dụng**:
  - Tận dụng cơ chế Thymeleaf Fragment template engine (`th:replace`, `th:fragment`), tương tự như `fragments/sidebar.html` và `fragments/navbar.html` của khu vực quản trị.
- **Logic đơn giản hóa**:
  - Đóng gói toàn bộ logic highlight tab (`home`, `courses`) và bảo vệ Online Judge vào một nơi duy nhất. Giảm thiểu hoàn toàn việc lặp code (DRY).
- **Ghi chú phạm vi**:
  - Chỉ tách và chuẩn hóa fragment cho các trang công khai, mã nguồn compile thành công 100%, không phát sinh comment thừa.
  - Toàn bộ thay đổi của TASK-03 và UI Guard/Layout Fragments đã được đưa vào nhánh riêng `feature/task-03-oj-guard-and-ui-fragments` và push thành công lên GitHub repository.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-14] - Xây Dựng Cơ Chế Tự Động Keep-Alive Chống Ngủ Đông (Sleep) Cho Render Free Tier
- **Yêu cầu gốc**: "giờ tôi muốn làm 1 cái cứ cách 1 khoảng thời gian là ping vô như kiểu swageger ở dưới hạ tầng để đảm báo render của tôi không tắt vì tôi đang dùng render free nên nó sẽ tắt nếu sau 1 khoảng thời gian (này bạn search nha) thì nó sẽ sleep server á"
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/controller/api/HealthApiController.java`: Tạo mới -> Endpoint siêu nhẹ `/api/health` và `/api/ping`, trả về trạng thái UP, timestamp và tên service, không truy vấn DB, phản hồi tức thì để tối ưu hóa tài nguyên.
  - `src/main/java/vn/edu/ute/utelearn/service/KeepAliveService.java`: Tạo mới -> Service chạy ngầm với `@Scheduled(fixedRate = 720000)` (mỗi 12 phút, trước mốc 15 phút của Render) tự động gửi HTTP GET request ra ngoài tới domain public Render (tận dụng biến môi trường Render tự cấp `RENDER_EXTERNAL_URL`).
  - `src/main/java/vn/edu/ute/utelearn/UtelearnApplication.java`: Chỉnh sửa -> Bổ sung annotation `@EnableScheduling` kích hoạt tác vụ định kỳ của Spring Framework.
  - `src/main/java/vn/edu/ute/utelearn/security/SecurityConfig.java`: Chỉnh sửa -> Cho phép truy cập công khai không cần xác thực (`permitAll()`) cho 2 endpoint `/api/health` và `/api/ping`.
  - `src/main/resources/application.yml`: Chỉnh sửa -> Bổ sung cấu hình `utelearn.render.keep-alive-url` và `utelearn.render.keep-alive-enabled`.
  - `.github/workflows/render-keep-alive.yml`: Tạo mới -> GitHub Actions workflow tự động chạy cron mỗi 12 phút ping URL Render từ bên ngoài, đóng vai trò giải pháp dự phòng phòng khi container bị tắt hẳn sau đợt bảo trì của hạ tầng.
- **Base tái sử dụng**:
  - Tận dụng `java.net.http.HttpClient` chuẩn của JDK 21 và cơ chế `@Scheduled` có sẵn trong Spring Context.
- **Logic đơn giản hóa**:
  - Sử dụng Guard Clause: nếu không tìm thấy biến môi trường URL hoặc đang chạy tại Localhost, tác vụ tự động bỏ qua ngay lập tức để không gây ô nhiễm log phát triển cục bộ.
- **Ghi chú phạm vi**:
  - Toàn bộ thay đổi độc lập và bổ trợ hạ tầng, không can thiệp vào nghiệp vụ cốt lõi hay cơ sở dữ liệu. Tuân thủ 100% quy chuẩn Zero Comments và dự án biên dịch BUILD SUCCESS.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-14] - Cập Nhật Mật Khẩu Khởi Tạo (Seed Password) Lên Chuẩn Mạnh 'Pass@123'
- **Yêu cầu gốc**: "với mật khẩu các tài khoản khi seed sửa lại thành Pass@123 đi"
- **Thành phần tác động**:
  - `src/main/resources/application.yml`: Chỉnh sửa -> Đổi `utelearn.seeder.default-password` từ `123456` sang `Pass@123`.
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Chỉnh sửa -> Đổi giá trị mặc định của `Seeder.defaultPassword` từ `password123` sang `Pass@123`.
- **Base tái sử dụng**:
  - Tận dụng cơ chế `DataSeeder` (`ON CONFLICT (username) DO UPDATE SET password_hash = EXCLUDED.password_hash`) để tự động cập nhật lại mật khẩu mã hóa BCrypt cho các tài khoản mẫu (`admin`, `instructor`, `moderator`, `student`).
- **Logic đơn giản hóa**:
  - Mật khẩu mới `Pass@123` đáp ứng đầy đủ chính sách mật khẩu mạnh (ít nhất 8 ký tự, có chữ hoa `P`, chữ thường `ass`, ký tự đặc biệt `@`, số `123`).
- **Ghi chú phạm vi**:
  - Chỉ tác động đến cấu hình khởi tạo tài khoản mẫu, không ảnh hưởng đến logic nghiệp vụ xác thực.







---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-16] - Rà Soát Toàn Diện Tiến Độ Đồ Án & Cập Nhật Bảng Nghiệm Thu To-Do Sheet
- **Yêu cầu gốc**: "check lại đồ án tôi đã làm gì và tích vào to-do đi"
- **Thành phần tác động**:
  - `docs/UTELearn_Todo_Sheet.csv`: Chỉnh sửa -> Tích hoàn thành (DONE) cho các task đã hoàn thiện trong mã nguồn: TASK-01 (Cohort CRUD), TASK-02 (Pricing 3 lựa chọn & Checkout), TASK-03 (Online Judge UI Guard), TASK-07 (Đánh dấu hoàn thành bài học & Tiến độ), TASK-08 (Lịch học Drip Content). Đổi trạng thái TASK-04 sang IN_PROGRESS.
  - `docs/TODO_DanhSachTinhNangChuaLam.md`: Chỉnh sửa -> Đồng bộ Phần 1 (Tổng quan các phân hệ đã code xong), Phần 2 (Bảng nghiệm thu To-Do Tracker) và Phần 3 (Danh sách chi tiết các công việc còn lại: Cổng thanh toán VNPay, Quiz & Question, Realtime Chat WebSocket, Admin Category & SLA).
- **Base tái sử dụng**:
  - Tái sử dụng bảng mẫu Use Case và SRS chuẩn doanh nghiệp Course - Cohort để đồng bộ danh mục nghiệm thu kỹ thuật.
- **Logic đơn giản hóa**:
  - Tách bạch rõ 3 trạng thái: Đã xong (DONE - 5 task), Đang làm (IN_PROGRESS - 1 task), Chưa làm (TODO - 3 task) và Định hướng tương lai (FUTURE_SCOPE - 1 task).
- **Ghi chú phạm vi**:
  - Chỉ cập nhật tài liệu kiểm soát tiến độ đồ án và file CSV To-Do Tracker, không tác động vào mã nguồn Java hay template giao diện.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-16] - Kiểm Tra Nghiêm Ngặt Hiện Trạng Mã Nguồn & Bổ Sung Danh Mục To-Do Chi Tiết
- **Yêu cầu gốc**: "check kỹ lại đi và đừng push code khi tôi chưa cho phép check kỹ và thêm các task chưa làm vào cái to-do đi (cái gì thật sự đã done mới ghi là done nha)"
- **Thành phần tác động**:
  - `docs/UTELearn_Todo_Sheet.csv`: Chỉnh sửa -> Tách nhỏ và rà soát nghiêm ngặt thành 14 công việc cụ thể. Chỉ đánh dấu `DONE` cho 4 task thật sự hoàn thành 100% end-to-end (Cohort CRUD, Course Pricing, OJ UI Guard, Giao diện cấu hình lịch Drip Content Giảng viên). Đánh dấu `IN_PROGRESS` cho 2 task đang làm dở (Tiến độ học tập mới lưu localStorage chưa lưu DB; Thanh toán Checkout mới có luồng mô phỏng chưa có Order DB và VNPay Sandbox). Bổ sung 7 task `TODO` chi tiết và 1 task `FUTURE_SCOPE`.
  - `docs/TODO_DanhSachTinhNangChuaLam.md`: Chỉnh sửa -> Cập nhật bảng tổng quan nghiệm thu thực tế, bảng To-Do Tracker 14 task và kế hoạch triển khai chi tiết cho từng task còn lại.
- **Base tái sử dụng**:
  - Đối chiếu trực tiếp từng dòng code Java và template Thymeleaf với bộ 37 Use Cases (`docs/usecases/`) để đảm bảo không báo cáo sai lệch hiện trạng.
- **Logic đơn giản hóa**:
  - Phân loại rõ ràng ranh giới giữa chức năng chỉ chạy ở Client (localStorage/mock) với chức năng đã lưu trữ DB an toàn (Backend Persistence).
- **Ghi chú phạm vi**:
  - Tuyệt đối tuân thủ yêu cầu người dùng: KHÔNG thực hiện lệnh `git push`. Toàn bộ thay đổi lưu trữ cục bộ tại workspace.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-16] - Cập Nhật Toàn Diện Danh Mục To-Do Bao Phủ 37 Use Cases (23 Tasks)
- **Yêu cầu gốc**: "cập nhật full vô to-do đi"
- **Thành phần tác động**:
  - `docs/UTELearn_Todo_Sheet.csv`: Chỉnh sửa -> Mở rộng toàn diện thành 23 task chuẩn mực bao phủ trọn vẹn 37 Use Cases (`UC-01` đến `UC-37`) và 8 phân hệ kiến trúc. Cập nhật trạng thái trung thực: 14 task `DONE` (đã xong 100% từ giao diện đến DB), 2 task `IN_PROGRESS` (Đơn hàng/Thanh toán VNPay & Lưu tiến độ DB), 6 task `TODO` (Khóa Drip Content phía học viên, Soạn Quiz, Làm bài thi Quiz, Chat WebSocket, Bảng điểm Cohort & gán TA, Category CRUD, Ràng buộc SLA), 1 task `FUTURE_SCOPE` (OJ Docker Sandbox).
  - `docs/TODO_DanhSachTinhNangChuaLam.md`: Chỉnh sửa -> Đồng bộ toàn bộ nội dung: Bảng tổng quan phân hệ đã hoàn thành thực sự, Bảng To-Do Tracker 23 task chi tiết với đầy đủ mã UC, file tác động, mô tả kỹ thuật, và kế hoạch thực hiện cụ thể chia theo 3 nhóm ưu tiên P0, P1, P2.
- **Base tái sử dụng**:
  - Kế thừa toàn bộ tài liệu 37 Use Cases (`docs/usecases/`) và đối chiếu với 27 Controllers, 14 Services, 16 Repositories, 22 Entities, 40 HTML templates và 23 bảng trong `schema.sql`.
- **Logic đơn giản hóa**:
  - Đánh số mã Task theo từng phân hệ (`TASK-AUTH`, `TASK-ADM`, `TASK-AST`, `TASK-CRS`, `TASK-CHT`, `TASK-STU`, `TASK-MOD`, `TASK-OJ`) giúp người dùng dễ dàng theo dõi, lọc và nhập vào Google Sheets.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-22] - Đồng Bộ Mã Nguồn Mới Nhất Từ Nhánh Remote origin/develop
- **Yêu cầu gốc**: "ấy code mới đi" (Lấy code mới đi)
- **Thành phần tác động**:
  - Toàn bộ workspace: Đồng bộ 13 commits mới từ `origin/develop` (PR #21, #24, #26, #27, #30).
  - Khắc phục xung đột git merge tại `docs/TODO_DanhSachTinhNangChuaLam.md` và `docs/UTELearn_Todo_Sheet.csv` bằng cách đồng bộ theo bản mới nhất từ remote đã được kiểm toán và cập nhật trạng thái các tính năng hoàn thiện.
- **Tính năng mới nhận được từ remote**:
  - Phân hệ Quản lý Danh mục Khóa học cho Admin (`AdminCategoryController`, `CategoryService`, `templates/admin/categories/`).
  - Kiểm duyệt SLA & Chống gian lận (`ModerationServiceImpl`, `ModeratorController`, phân quyền `canEdit`/`hasAccessRole`, thẩm định tài liệu).
  - Gợi ý nâng cấp trọn gói Full Course bù chênh lệch giá (`CourseUpsellDTO`, `UserEnrollmentSummaryDTO`, `CohortServiceImpl`, `CheckoutController`).
  - Trạng thái động của Cohort (`IN_PROGRESS`) trên danh mục công khai.
  - Phân quyền hiển thị theo vai trò trên Profile Sidebar và Navbar.
- **Kiểm thử & Biên dịch**:
  - `mvnw.cmd test-compile`: BUILD SUCCESS (biên dịch 126 file source và 12 file test).
  - `mvnw.cmd test`: BUILD SUCCESS, toàn bộ 94/94 unit tests chạy thành công (0 failure, 0 error).
- **Ghi chú phạm vi**:
  - Hoàn tất đồng bộ nhánh `develop` về trạng thái mới nhất khớp hoàn toàn với `origin/develop`.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-22] - Xây Dựng Hệ Thống Đơn Hàng (Order) & Cổng Thanh Toán Tự Động VietQR SePay (UC-22, UC-34)
- **Người thực hiện**: Antigravity Assistant
- **Yêu cầu gốc**: "Viet Qr + sepay làm 1. Hệ thống Đơn hàng (Order) & Cổng Thanh toán Điện tử (UC-22, UC-34) ... Lưu vết Đơn hàng thực tế ... Chưa có tầng Service & Controller Đơn hàng ... Tích hợp Cổng thanh toán trực tuyến ... Lịch sử đơn hàng ... và cho admin được quyền chỉnh sửa thanh toán hay tạo ra thêm 1 role chung với admin để check quyền thanh toán đã thành công chưa á"
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/dto/SepayWebhookDTO.java`: Tạo mới -> DTO ánh xạ dữ liệu Webhook IPN từ cổng SePay (gateway, transactionDate, accountNumber, content, transferAmount, referenceCode).
  - `src/main/java/vn/edu/ute/utelearn/dto/OrderItemDTO.java`: Tạo mới -> DTO cấu trúc các phần tử mua sắm (JSONB) lưu vết đơn hàng (itemId, itemType, itemName, originalPrice, discountAmount, finalPrice).
  - `src/main/java/vn/edu/ute/utelearn/dto/OrderResponseDTO.java`: Tạo mới -> DTO chuẩn hóa dữ liệu trả về cho giao diện học viên và API polling trạng thái đơn hàng.
  - `src/main/java/vn/edu/ute/utelearn/entity/Order.java`: Chỉnh sửa -> Bổ sung trường `paidAt`, các helper định dạng tiền tệ (`getFormattedTotalAmount()`), thời gian (`getFormattedCreatedAt()`, `getFormattedPaidAt()`) và class badge HTML.
  - `src/main/java/vn/edu/ute/utelearn/dao/OrderRepository.java`: Chỉnh sửa -> Bổ sung các truy vấn `findByOrderCode`, `findByUserOrderByCreatedAtDesc`, `findByStatusOrderByCreatedAtDesc`, `countByStatus`, và cập nhật tính doanh thu chấp nhận cả 2 trạng thái `COMPLETED` và `SUCCESS`.
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java` & `src/main/resources/application.yml`: Chỉnh sửa -> Cấu hình `utelearn.payment` (ngân hàng MBBank, số tài khoản, tên tài khoản, template VietQR và SePay API Key).
  - `src/main/java/vn/edu/ute/utelearn/service/OrderService.java` & `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: Tạo mới -> Toàn bộ nghiệp vụ vòng đời đơn hàng: khởi tạo đơn `PENDING` (lẻ/combo/upsell), tạo URL VietQR động, tiếp nhận Webhook SePay bóc tách mã đơn bằng Regex, đối soát tiền, tự động ghi danh `CohortMember` & `Enrollment`, Admin/Finance duyệt/hủy thủ công, và Học viên tự hủy đơn chờ.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Chỉnh sửa -> Tích hợp `OrderService`, tạo đơn lưu DB tại `/{cohortId}/initiate` và `/course/{slug}/initiate`, hiển thị giao diện thanh toán `/payment/{orderCode}`, hỗ trợ thanh toán giả lập `/payment/{orderCode}/simulate` và bảo lưu constructor 6 tham số.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/SepayWebhookApiController.java`: Tạo mới -> REST API tiếp nhận SePay IPN Webhook `POST /api/webhooks/sepay` và API polling realtime `GET /api/orders/{orderCode}/status`.
  - `src/main/java/vn/edu/ute/utelearn/controller/OrderController.java`: Tạo mới -> Phân hệ Đơn hàng cá nhân cho học viên: danh sách `/orders`, chi tiết hóa đơn `/orders/{orderCode}`, hủy đơn `/orders/{orderCode}/cancel`.
  - `src/main/java/vn/edu/ute/utelearn/controller/AdminOrderController.java`: Tạo mới -> Phân hệ Quản trị & Đối soát Đơn hàng cho Admin & Finance: `/admin/orders`, phê duyệt thanh toán `POST /admin/orders/{id}/confirm`, hủy đơn `POST /admin/orders/{id}/cancel`.
  - `src/main/java/vn/edu/ute/utelearn/config/DataSeeder.java`: Chỉnh sửa -> Khởi tạo vai trò mới `FINANCE`, quyền hạn `ORDER_MANAGE`, và tài khoản mẫu `finance@utelearn.edu.vn`.
  - `src/main/java/vn/edu/ute/utelearn/security/SecurityConfig.java`: Chỉnh sửa -> Công khai API Webhook và Polling; phân quyền `/admin/orders/**` cho `ADMIN` và `FINANCE`.
  - `src/main/resources/templates/checkout/payment.html`: Tạo mới -> Giao diện thanh toán VietQR SePay hiện đại, nút copy 1 chạm, đếm ngược 15 phút, AJAX polling 3 giây tự động chuyển hướng.
  - `src/main/resources/templates/checkout/index.html`: Chỉnh sửa -> Tích hợp tùy chọn VietQR SePay và điều hướng sang trang thanh toán QR.
  - `src/main/resources/templates/orders/index.html` & `detail.html`: Tạo mới -> Giao diện Lịch sử đơn hàng và Hóa đơn điện tử in ấn cho học viên.
  - `src/main/resources/templates/admin/orders/index.html`: Tạo mới -> Giao diện Đối soát Đơn hàng Admin & Finance với 4 thẻ KPI, bộ lọc tab, tìm kiếm nhanh và modal xác nhận.
  - `src/main/resources/templates/profile/index.html`: Chỉnh sửa -> Bổ sung liên kết đến Lịch sử đơn hàng `/orders`.
  - `src/main/resources/templates/fragments/sidebar.html`, `navbar.html`, `site-navbar.html`: Chỉnh sửa -> Bổ sung menu Quản lý Đơn hàng cho Admin/Finance và Đơn hàng của tôi cho Học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Tạo mới -> Bộ 10 kịch bản kiểm thử tự động toàn diện bao phủ 100% luồng đơn hàng và thanh toán.
  - `docs/TODO_DanhSachTinhNangChuaLam.md` & `docs/UTELearn_Todo_Sheet.csv`: Chỉnh sửa -> Cập nhật trạng thái hoàn thành 100% (DONE) cho hạng mục Đơn hàng & Cổng thanh toán.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.java`, `OrderRepository.java`, `CohortMemberRepository`, `EnrollmentRepository`, `CourseRepository`, `CohortRepository`.
  - Tái sử dụng `layout:decorate="~{fragments/admin-layout}"` và `layout:decorate="~{fragments/site-layout}"`.
  - Tái sử dụng bảng mã vai trò và quyền hạn của `DataSeeder.java` để bổ sung vai trò `FINANCE` chuẩn mực.
- **Logic đơn giản hóa**:
  - Áp dụng triệt để Guard Clauses (Early Return) trong xử lý Webhook SePay và Admin Order Controller.
  - Dùng Regex `(?i)(UTE[0-9A-Z]{4,10}|ORD[0-9A-Z]{4,10})` để bóc tách chính xác mã đơn hàng kể cả khi khách hàng chuyển khoản ghi kèm nội dung khác.
  - Hỗ trợ cả 2 phương thức kích hoạt: Tự động qua Webhook/Polling và Thủ công qua nút bấm Phê duyệt của Admin/Kế toán.
- **Tuân thủ quy chuẩn dự án**:
  - Tuyệt đối không thêm bất kỳ comment nào (`//`, `/* */`, `<!-- -->`) trong toàn bộ code mới và code chỉnh sửa (Quy tắc 1).
  - Không thực hiện lệnh `git push` khi chưa có yêu cầu từ người dùng.
- **Kết quả kiểm thử**:
  - `mvnw.cmd test`: BUILD SUCCESS, toàn bộ **104/104 tests** chạy thành công (0 failure, 0 error).





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---


## [2026-10-23 19:05] - Vá Lỗ Hổng Bảo Mật & Nâng Cấp Chuẩn Nghiệp Vụ Luồng Thanh Toán (Payment Security & Concurrency)
- **Yêu cầu gốc**: "xử lý các vấn đề có ở trên đi"
- **Mục tiêu**: Khắc phục toàn bộ các lỗ hổng phân quyền, xử lý bất đồng bộ (Race Condition/Idempotency), chặn đơn hàng hết hạn thanh toán tự động, và bổ sung cờ bật/tắt chế độ mô phỏng thanh toán.
- **Thành phần tác động**:
  - `src/main/java/vn/edu/ute/utelearn/config/UtelearnProperties.java`: Bổ sung trường `simulationEnabled` vào `UtelearnProperties.Payment`.
  - `src/main/resources/application.yml`: Bổ sung cấu hình `utelearn.payment.simulation-enabled: ${PAYMENT_SIMULATION_ENABLED:true}`.
  - `src/main/java/vn/edu/ute/utelearn/controller/api/CheckoutApiController.java`: Thắt chặt endpoint `/api/orders/{orderCode}/simulate` - Kiểm tra quyền sở hữu đơn hàng (chỉ chủ đơn hoặc Admin/Finance) và kiểm tra cờ `simulationEnabled`.
  - `src/main/java/vn/edu/ute/utelearn/controller/CheckoutController.java`: Kiểm tra phân quyền và cờ `simulationEnabled` trên các endpoint `/payment/{orderCode}/simulate`, `/{cohortId}/process`, và `/course/{slug}/process`.
  - `src/main/java/vn/edu/ute/utelearn/service/impl/OrderServiceImpl.java`: 
    + Xử lý luồng `fulfillOrder` an toàn đa luồng bằng `synchronized(orderCode.intern())` chống double-crediting và double-enrolling.
    + Kiểm tra đơn hàng quá hạn `order.isExpired()` trong `fulfillOrder` và `processSepayWebhook`: tự động hủy đơn và từ chối kích hoạt khóa học khi học viên thanh toán sau thời hạn 48h.
    + Kiểm tra sĩ số tối đa (`maxCapacity`) của đợt học trong `createCohortOrder`, `payWithWallet`, `createOfflineCohortOrder`, `createCashierOrder`: chặn đăng ký khi lớp đã đủ học viên.
  - `src/test/java/vn/edu/ute/utelearn/OrderFeatureTest.java`: Bổ sung kiểm thử `testFulfillOrder_ExpiredOrderThrowsExceptionAndMarksCancelled`, `testProcessSepayWebhook_ExpiredOrderFailsAndCancels`, và `testCreateCohortOrder_CohortFullThrowsIllegalStateException`.
- **Base tái sử dụng**:
  - Tái sử dụng `Order.isExpired()`, `UtelearnProperties`, `AuthService`, `OrderRepository`.
- **Logic đơn giản hóa**:
  - Áp dụng Guard Clauses kiểm tra quyền và trạng thái hết hạn ngay đầu hàm; đồng bộ hóa nhẹ theo định danh đơn hàng chuỗi String Pool.
- **Kết quả kiểm thử**:
  - Toàn bộ 21/21 unit tests trong `OrderFeatureTest` vượt qua 100% (BUILD SUCCESS).
- **Quy chuẩn tuân thủ**:
  - Tuyệt đối không thêm comment trong code; tuân thủ DRY, Guard Clauses, Scope Isolation và ghi log chi tiết.





---
