DROP TABLE IF EXISTS cohort_messages CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS moderation_tasks CASCADE;
DROP TABLE IF EXISTS moderator_attendances CASCADE;
DROP TABLE IF EXISTS coding_submissions CASCADE;
DROP TABLE IF EXISTS coding_problems CASCADE;
DROP TABLE IF EXISTS quiz_submissions CASCADE;
DROP TABLE IF EXISTS questions CASCADE;
DROP TABLE IF EXISTS quizzes CASCADE;
DROP TABLE IF EXISTS cohort_schedules CASCADE;
DROP TABLE IF EXISTS cohort_members CASCADE;
DROP TABLE IF EXISTS cohorts CASCADE;
DROP TABLE IF EXISTS lessons CASCADE;
DROP TABLE IF EXISTS sections CASCADE;
DROP TABLE IF EXISTS courses CASCADE;
DROP TABLE IF EXISTS categories CASCADE;
DROP TABLE IF EXISTS instructor_assets CASCADE;
DROP TABLE IF EXISTS password_reset_tokens CASCADE;
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- 1. ROLE & PERMISSION

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,             -- VD: 'ADMIN', 'INSTRUCTOR'
    name VARCHAR(100) NOT NULL,                   -- VD: 'Quản trị viên', 'Giảng viên'
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) UNIQUE NOT NULL,            -- VD: 'COURSE_CREATE', 'MODERATION_APPROVE'
    name VARCHAR(150) NOT NULL,
    module VARCHAR(50) NOT NULL,                  -- VD: 'COURSE', 'MODERATION', 'OJ'
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt
    full_name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20),
    avatar_url TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expiry_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. DIGITAL ASSET LIBRARY FOR INSTRUCTORS

CREATE TABLE instructor_assets (
    id BIGSERIAL PRIMARY KEY,
    instructor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    asset_type VARCHAR(50) NOT NULL,              -- 'VIDEO', 'DOCUMENT', 'SLIDE', 'CODE_TEMPLATE', 'TESTCASE'
    cloudinary_url TEXT NOT NULL,
    file_size_bytes BIGINT DEFAULT 0,
    folder_path VARCHAR(255) DEFAULT '/',         -- VD: '/JavaSpringBoot/Week01/'
    visibility VARCHAR(30) DEFAULT 'PRIVATE',     -- 'PRIVATE', 'DEPARTMENT_SHARED', 'COHORT_PUBLIC'
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assets_instructor ON instructor_assets(instructor_id);

-- 3. MASTER COURSE & CURRICULUM

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(150) UNIQUE NOT NULL,
    parent_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,             -- VD: 'CS101-SPRINGBOOT'
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE NOT NULL,
    headline VARCHAR(500),
    description TEXT,
    thumbnail_url TEXT,
    level VARCHAR(30) DEFAULT 'ALL_LEVELS',       -- 'BEGINNER', 'INTERMEDIATE', 'ADVANCED'
    status VARCHAR(30) DEFAULT 'DRAFT',           -- 'DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED'
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    objectives JSONB DEFAULT '[]'::jsonb,
    requirements JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sections (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    order_index INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    lesson_type VARCHAR(30) NOT NULL,             -- 'VIDEO', 'DOCUMENT', 'QUIZ', 'CODE_PRACTICE'
    order_index INT NOT NULL DEFAULT 1,
    is_free_preview BOOLEAN DEFAULT FALSE,
    video_url TEXT,                               -- If it's a VIDEO (watch forever)
    duration_seconds INT DEFAULT 0,
    document_content TEXT,                        -- If it's a DOCUMENT
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_courses_category ON courses(category_id);
CREATE INDEX idx_courses_creator ON courses(created_by);
CREATE INDEX idx_sections_course ON sections(course_id, order_index);
CREATE INDEX idx_lessons_section ON lessons(section_id, order_index);

-- 4. COHORT-BASED MODEL

CREATE TABLE cohorts (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id),
    code VARCHAR(50) UNIQUE NOT NULL,             -- VD: 'K23-JAVA-01'
    name VARCHAR(255) NOT NULL,                  -- VD: 'Lớp Spring Boot K23 - Đợt 1'
    
    enrollment_start TIMESTAMPTZ NOT NULL,        -- Open enrollment
    enrollment_end TIMESTAMPTZ NOT NULL,          -- Close enrollment
    study_start TIMESTAMPTZ NOT NULL,             -- Start study
    study_end TIMESTAMPTZ NOT NULL,               -- End study
    
    max_capacity INT DEFAULT 50,
    current_enrolled INT DEFAULT 0,
    price NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) DEFAULT 'UPCOMING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_enrollment_period CHECK (enrollment_start < enrollment_end),
    CONSTRAINT chk_study_period CHECK (study_start < study_end)
);

CREATE TABLE cohort_members (
    id BIGSERIAL PRIMARY KEY,
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_in_cohort VARCHAR(30) DEFAULT 'STUDENT', -- 'STUDENT', 'TEACHER', 'TEACHING_ASSISTANT'
    enrolled_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    final_grade NUMERIC(5, 2),
    is_passed BOOLEAN DEFAULT FALSE,
    certificate_url TEXT,
    CONSTRAINT uq_cohort_user UNIQUE (cohort_id, user_id)
);

-- Lesson unlock schedule, Deadline, & Teacher Score Visibility Settings
CREATE TABLE cohort_schedules (
    id BIGSERIAL PRIMARY KEY,
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    
    unlock_at TIMESTAMPTZ NOT NULL,               -- Time to unlock lesson / open exam
    deadline_at TIMESTAMPTZ,                      -- Deadline for SUBMISSION (NULL = Video can be watched again freely)
    
    show_score_type VARCHAR(30) DEFAULT 'IMMEDIATELY', -- 'IMMEDIATELY', 'AFTER_DEADLINE', 'MANUAL'
    is_score_published BOOLEAN DEFAULT TRUE,           -- Score has been publicly opened for the class
    allow_review_answers BOOLEAN DEFAULT FALSE,        -- Allow reviewing correct answers / explanations
    review_available_at TIMESTAMPTZ,                   -- Time to open score review for the class
    
    CONSTRAINT uq_cohort_lesson UNIQUE (cohort_id, lesson_id)
);

CREATE INDEX idx_cohorts_course ON cohorts(course_id);
CREATE INDEX idx_cohort_members_user ON cohort_members(user_id);
CREATE INDEX idx_cohort_members_cohort ON cohort_members(cohort_id);

-- 5. MULTIPLE CHOICE QUIZZES, CHECKBOXES, FILL-IN-THE-BLANK, ESSAYS

CREATE TABLE quizzes (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT UNIQUE NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    duration_minutes INT DEFAULT 45,              -- Countdown timer when taking the test
    passing_score NUMERIC(5, 2) DEFAULT 5.0,
    max_attempts INT DEFAULT 1,                   -- 1 = True test, >1 = Practice
    shuffle_questions BOOLEAN DEFAULT TRUE,       -- Shuffle questions to prevent cheating
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    content TEXT NOT NULL,                        -- Question text (Markdown/HTML)
    question_type VARCHAR(30) NOT NULL,           -- 'SINGLE_CHOICE', 'MULTI_SELECT', 'FILL_BLANK', 'ESSAY'
    points NUMERIC(5, 2) DEFAULT 1.0,
    explanation TEXT,                             -- Detailed solution
    options JSONB DEFAULT '[]'::jsonb,            -- List of options or fill-in keywords
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_questions_options_gin ON questions USING gin (options);
CREATE INDEX idx_questions_quiz ON questions(quiz_id);

CREATE TABLE quiz_submissions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id),
    
    started_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, -- Time to start taking the test
    submitted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,-- Time to submit the test
    
    score NUMERIC(5, 2),
    is_passed BOOLEAN DEFAULT FALSE,
    status VARCHAR(30) DEFAULT 'SUBMITTED',       -- 'SUBMITTED', 'GRADED', 'PENDING_MANUAL_GRADE'
    answers JSONB DEFAULT '[]'::jsonb,            -- All answers of the student
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submissions_user_quiz ON quiz_submissions(user_id, quiz_id, cohort_id);
CREATE INDEX idx_submissions_answers_gin ON quiz_submissions USING gin (answers);

-- 6. ONLINE JUDGE (OJ) FOR PROGRAMMING EXERCISES

CREATE TABLE coding_problems (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT UNIQUE NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    statement_markdown TEXT NOT NULL,
    allowed_languages VARCHAR(255) DEFAULT 'JAVA,PYTHON,CPP',
    time_limit_ms INT DEFAULT 2000,
    memory_limit_mb INT DEFAULT 256,
    starter_code TEXT,
    show_hidden_test_details BOOLEAN DEFAULT FALSE, -- Hide/show detailed hidden test case results
    test_cases JSONB DEFAULT '[]'::jsonb,         -- Array of test cases
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coding_problems_lesson ON coding_problems(lesson_id);
CREATE INDEX idx_coding_problems_test_cases_gin ON coding_problems USING gin (test_cases);

CREATE TABLE coding_submissions (
    id BIGSERIAL PRIMARY KEY,
    coding_problem_id BIGINT NOT NULL REFERENCES coding_problems(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id),
    source_code TEXT NOT NULL,
    language VARCHAR(30) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',         -- 'PENDING', 'ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'COMPILE_ERROR'
    execution_time_ms INT,
    memory_used_kb INT,
    score NUMERIC(5, 2) DEFAULT 0.0,
    log_output TEXT,
    test_results JSONB DEFAULT '[]'::jsonb,       -- Test results from Sandbox
    submitted_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_coding_submissions_user ON coding_submissions(cohort_id, user_id, coding_problem_id);

-- 7. WORKLOAD QUEUE FOR MODERATION & PROGRESS STATISTICS

-- Attendance & Daily Workload Tracking
CREATE TABLE moderator_attendances (
    id BIGSERIAL PRIMARY KEY,
    moderator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    work_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,               -- TRUE: On duty, FALSE: Off/Absent
    
    -- WORKLOAD BALANCING & REAL-TIME WORKSTAT
    max_daily_minutes INT DEFAULT 480,            -- Maximum working minutes per day (480 minutes = 8 hours)
    assigned_tasks_count INT DEFAULT 0,           -- Total tasks assigned by the system today
    completed_tasks_count INT DEFAULT 0,          -- Completed tasks
    active_tasks_count INT DEFAULT 0,             -- Active tasks not yet completed
    total_workload_minutes INT DEFAULT 0,         -- Total accumulated working minutes today
    
    check_in_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    check_out_at TIMESTAMPTZ,
    
    CONSTRAINT uq_mod_date UNIQUE (moderator_id, work_date)
);

CREATE TABLE moderation_tasks (
    id BIGSERIAL PRIMARY KEY,
    item_type VARCHAR(50) NOT NULL,               -- 'LESSON', 'QUIZ', 'CODING_PROBLEM'
    item_id BIGINT NOT NULL,                      -- ID of the entity to be reviewed
    author_id BIGINT NOT NULL REFERENCES users(id),-- Author of the content (Lecturer who created the lesson)
    cohort_id BIGINT REFERENCES cohorts(id) ON DELETE SET NULL,
    
    assigned_to BIGINT REFERENCES users(id) ON DELETE SET NULL, -- Moderator or Lecturer for cross-review
    status VARCHAR(30) DEFAULT 'PENDING',         -- 'PENDING', 'ASSIGNED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'ESCALATED', 'RE_QUEUED'
    
    -- DYNAMIC REVIEW TIME BASED ON CONTENT VOLUME
    estimated_review_minutes INT DEFAULT 60,      -- Estimated review time based on video length/test cases
    assigned_at TIMESTAMPTZ,
    deadline_at TIMESTAMPTZ,                      -- Deadline for review (SLA Timeout)
    
    -- ESCALATION SYSTEM FOR DIFFICULT CASES
    escalation_level INT DEFAULT 1,               -- 1: Cross-review/Base, 2: Head of Department, 3: Admin
    escalated_by BIGINT REFERENCES users(id),     -- Who initiated the escalation
    escalation_reason TEXT,                       -- Reason for escalating
    
    feedback TEXT,                                -- Feedback if rejected
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- CORE CONSTRAINT: LECTURER CANNOT REVIEW THEIR OWN CONTENT!
    CONSTRAINT chk_not_self_review CHECK (assigned_to IS NULL OR author_id <> assigned_to)
);

CREATE INDEX idx_tasks_queue ON moderation_tasks(status, assigned_to);
CREATE INDEX idx_tasks_assigned_to ON moderation_tasks(assigned_to);

-- 8. ORDERS & PAYMENTS

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    order_code VARCHAR(50) UNIQUE NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',         -- 'PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'
    payment_method VARCHAR(50),
    transaction_id VARCHAR(100),
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_user ON orders(user_id);

-- 9. REAL-TIME MESSAGES (WEBSOCKET CHAT / Q&A)

CREATE TABLE cohort_messages (
    id BIGSERIAL PRIMARY KEY,
    cohort_id BIGINT NOT NULL REFERENCES cohorts(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message TEXT NOT NULL,
    message_type VARCHAR(30) DEFAULT 'TEXT',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_cohort ON cohort_messages(cohort_id, created_at);