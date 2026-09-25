-- AARDS placeholder schema (PostgreSQL)
-- V1 entities: users, uploads, students, subjects, results, validations, analytics, recommendations, audit_logs

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN','FACULTY','HOD','PRINCIPAL')),
    full_name VARCHAR(150),
    department VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS uploads (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    academic_session VARCHAR(20),
    year VARCHAR(20),
    semester VARCHAR(20),
    department VARCHAR(100),
    uploaded_by BIGINT REFERENCES users(id),
    status VARCHAR(30) DEFAULT 'UPLOADED',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    seat_no VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    department VARCHAR(100),
    academic_year VARCHAR(20),
    semester VARCHAR(20),
    sgpa DOUBLE PRECISION,
    result VARCHAR(20)
);

-- TODO V1: subjects, student_marks, validation_errors, recommendations, audit_logs
