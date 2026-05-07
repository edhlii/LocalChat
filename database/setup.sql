-- ======================================================
-- DATABASE INITIALIZATION SCRIPT FOR LOCALCHAT
-- Author: Nguyễn Đức Anh, Nguyễn Văn Quang, Lê Đình Hiếu
-- ======================================================
CREATE DATABASE IF NOT EXISTS localchat;
USE localchat;

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    `role` ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER',
    avatar LONGBLOB DEFAULT NULL
    );

-- 2. Table: chat_groups
CREATE TABLE IF NOT EXISTS chat_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
    );

-- 3. Table: group_members
CREATE TABLE IF NOT EXISTS group_members (
    group_id INT,
    user_id INT,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES chat_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- 4. Table: messages
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT DEFAULT NULL,
    group_id INT DEFAULT NULL,
    message_type ENUM('TEXT', 'IMAGE', 'FILE') DEFAULT 'TEXT',
    content TEXT,
    file_name VARCHAR(255),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES chat_groups(id) ON DELETE CASCADE,
    INDEX idx_chat_lookup (sender_id, receiver_id, group_id, sent_at)
    );

-- 5. Table: conversation_status
CREATE TABLE IF NOT EXISTS conversation_status (
    user_id INT,
    partner_id INT,
    group_id INT,
    last_read_message_id INT,
    PRIMARY KEY (user_id, partner_id, group_id)
    );

--------------------------------------------------------
-- ======================================================
-- SAMPLE DATA (Initial accounts)
-- Passwords are BCrypt hashed (Cost 12), default : 123
-- ======================================================
INSERT INTO users (username, password, nickname, role)
VALUES
    ('ducanh', '$2a$12$AwqraqEAznNZdB7lVomfCOJ8i7Y5XRRuNTFFWLIYTby2tDL50FkDS', 'Duc Anh', 'MANAGER'),
    ('vanquang', '$2a$12$ZgdLhWDxfvT09/UjWG/wlubDnO5rVXTh0pou.XAB43BNMUvB2CIqa', 'Quang', 'MEMBER'),
    ('dinhhieu', '$2a$12$xUVOxsIFoDBh6lWRAgL10uaCvUo1Zw1DJ7XU3xsOFhOV08SyhAPWO', 'Hieu', 'MEMBER'),
    ('vantoan', '$2a$12$NQmQoGxVptAY0sqUk5t5C.hY2EjKVcQEArWR9LpVT8zpVRrUQeU7G', 'Van Toan', 'MANAGER');