-- AARDS schema (PostgreSQL, for reference - JPA auto-creates tables in dev)
-- NOTE: Will be refined when actual SPPU PDF format is received.
-- Entities use simple Long ids for relations to stay fresher-friendly.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    email VARCHAR(150),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','FACULTY','HOD','PRINCIPAL')),
    department VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS departments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL UNIQUE,
    hod_id BIGINT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS academic_sessions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    prn VARCHAR(50) NOT NULL UNIQUE,
    roll_number VARCHAR(50),
    full_name VARCHAR(150) NOT NULL,
    department_id BIGINT,
    admission_year INT,
    current_year INT,
    current_semester INT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    department_id BIGINT,
    study_year INT,
    semester INT,
    credits INT,
    max_marks INT,
    passing_marks INT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS results (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    academic_session_id BIGINT NOT NULL REFERENCES academic_sessions(id),
    study_year INT,
    semester INT,
    marks_obtained DOUBLE PRECISION,
    grade VARCHAR(5),
    status VARCHAR(10) DEFAULT 'PASS',
    is_backlog BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS semester_results (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    academic_session_id BIGINT NOT NULL,
    study_year INT,
    semester INT,
    sgpa DOUBLE PRECISION,
    backlog_count INT DEFAULT 0,
    status VARCHAR(10) DEFAULT 'PASS',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS upload_batches (
    id BIGSERIAL PRIMARY KEY,
    uploaded_by_user_id BIGINT,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    status VARCHAR(20) DEFAULT 'UPLOADED',
    total_records INT DEFAULT 0,
    error_records INT DEFAULT 0,
    uploaded_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS validation_errors (
    id BIGSERIAL PRIMARY KEY,
    upload_batch_id BIGINT NOT NULL,
    student_prn VARCHAR(50),
    field_name VARCHAR(100),
    extracted_value VARCHAR(500),
    corrected_value VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recommendations (
    id BIGSERIAL PRIMARY KEY,
    academic_session_id BIGINT,
    department_id BIGINT,
    study_year INT,
    semester INT,
    subject_id BIGINT,
    problem VARCHAR(200),
    reason VARCHAR(500),
    recommendation_text TEXT,
    priority VARCHAR(10) DEFAULT 'MEDIUM',
    created_at TIMESTAMP DEFAULT NOW()
);
