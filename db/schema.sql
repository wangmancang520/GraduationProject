CREATE TABLE IF NOT EXISTS `user` (
 u_id INT AUTO_INCREMENT PRIMARY KEY, u_name VARCHAR(100), u_nickname VARCHAR(100),
 u_login_name VARCHAR(255) NOT NULL UNIQUE, u_password VARCHAR(255) NOT NULL,
 u_phone VARCHAR(32), u_stu_num VARCHAR(64), u_type INT NOT NULL DEFAULT 1,
 u_email VARCHAR(255), u_address VARCHAR(255), u_avatar_path VARCHAR(1024),
 u_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS classroom (
 c_id INT AUTO_INCREMENT PRIMARY KEY, c_name VARCHAR(100) NOT NULL,
 c_volume INT, c_building VARCHAR(100), c_floor VARCHAR(32), c_address VARCHAR(255),
 c_admin_id INT, c_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS appointment (
 a_id INT AUTO_INCREMENT PRIMARY KEY, c_id INT NOT NULL, u_id INT NOT NULL,
 a_start_time TIMESTAMP NOT NULL, a_end_time TIMESTAMP NOT NULL,
 a_status INT NOT NULL DEFAULT 1, a_approval_status INT NOT NULL DEFAULT 1,
 a_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_appointment_classroom FOREIGN KEY (c_id) REFERENCES classroom(c_id),
 CONSTRAINT fk_appointment_user FOREIGN KEY (u_id) REFERENCES `user`(u_id)
);
CREATE INDEX idx_appointment_room_time ON appointment(c_id, a_start_time, a_end_time);
CREATE TABLE IF NOT EXISTS message (
 m_id INT AUTO_INCREMENT PRIMARY KEY, m_title VARCHAR(255), m_content TEXT,
 m_sender_id INT, m_receiver_id INT NOT NULL, m_status INT NOT NULL DEFAULT 1,
 m_create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_message_receiver_status ON message(m_receiver_id, m_status);
CREATE TABLE IF NOT EXISTS `file` (
 id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), type VARCHAR(32), size BIGINT,
 url VARCHAR(1024), md5 VARCHAR(32), is_deleted BOOLEAN DEFAULT FALSE,
 is_disabled BOOLEAN DEFAULT FALSE
);
