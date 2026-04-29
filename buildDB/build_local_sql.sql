CREATE DATABASE localchat;
USE localchat;

CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50), NOT NULL, UNIQUE
    password VARCHAR(50), NOT NULL,
    nickname VARCHAR(50), NOT NULL,
    role ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER'
);

INSERT INTO users (username, password, nickname, role) 
VALUES ('ducanh', '123', 'Duc Anh', 'MANAGER'),
		('vanquang', '123', 'Quang', 'MEMBER'),
		('dinhhieu', '123', 'Hieu', 'MEMBER'),
		('vantoan', '123', 'Van Toan', 'MANAGER');



----------------------
--Mở DB Bôi đen Đoạn này và chạy
USE localchat;
DROP TABLE users;
CREATE TABLE users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    `password` VARCHAR(100) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    `role` ENUM('MANAGER', 'MEMBER') DEFAULT 'MEMBER'
);
INSERT INTO users (username, password, nickname, role)
VALUES ('ducanh', '$2a$12$AwqraqEAznNZdB7lVomfCOJ8i7Y5XRRuNTFFWLIYTby2tDL50FkDS', 'Duc Anh', 'MANAGER'),
		('vanquang', '$2a$12$ZgdLhWDxfvT09/UjWG/wlubDnO5rVXTh0pou.XAB43BNMUvB2CIqa', 'Quang', 'MEMBER'),
		('dinhhieu', '$2a$12$xUVOxsIFoDBh6lWRAgL10uaCvUo1Zw1DJ7XU3xsOFhOV08SyhAPWO', 'Hieu', 'MEMBER'),
		('vantoan', '$2a$12$NQmQoGxVptAY0sqUk5t5C.hY2EjKVcQEArWR9LpVT8zpVRrUQeU7G', 'Van Toan', 'MANAGER');

---------
---chạy cái này để tạo cột avatar trong table users
USE localchat;
ALTER TABLE users ADD COLUMN avatar LONGBLOB AFTER role;


---Chạy cái này để xóa 2 cột file_size và file_path trong table messages
USE localchat;
ALTER TABLE messages DROP COLUMN file_size;
USE localchat;
ALTER TABLE messages DROP COLUMN file_path;

---Chạy cái này để tạo table conversation_status : dùng để check tin nhắn đã read
USE localchat;
CREATE TABLE conversation_status (
    user_id INT,
    partner_id INT, -- Nếu partner_id = 0, hiểu là Thông báo chung
    last_read_message_id INT,
    PRIMARY KEY (user_id, partner_id)
);

---------
--Chạy cái SQL này, để tạo table messages
USE localchat;
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT NOT NULL,
    receiver_id INT DEFAULT NULL,


    message_type ENUM('TEXT', 'IMAGE', 'FILE') DEFAULT 'TEXT',
    content TEXT,


    file_path VARCHAR(512),
    file_name VARCHAR(255),
    file_size BIGINT,

    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,


    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,


    INDEX idx_chat_lookup (sender_id, receiver_id, sent_at)
);


-- 1. Tạo thêm bảng Quản lý Nhóm
CREATE TABLE IF NOT EXISTS chat_groups (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Tạo thêm bảng Quản lý Thành viên trong Nhóm
CREATE TABLE IF NOT EXISTS group_members (
    group_id INT,
    user_id INT,
    PRIMARY KEY (group_id, user_id),
    FOREIGN KEY (group_id) REFERENCES chat_groups(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE messages
ADD COLUMN group_id INT DEFAULT NULL AFTER receiver_id;

ALTER TABLE messages
ADD FOREIGN KEY (group_id) REFERENCES chat_groups(id) ON DELETE CASCADE;