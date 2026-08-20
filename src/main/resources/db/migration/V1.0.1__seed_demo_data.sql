-- V1.0.1__seed_demo_data.sql
INSERT INTO sys_user (id, username, password, nickname, avatar)
VALUES (1, 'demo', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOAhfr87GURXHaKGm', '极客探索者', 'https://api.dicebear.com/7.x/bottts/svg?seed=LifeTracker')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO user_habit (id, user_id, name, icon, color, target_days, is_deleted)
VALUES
(1, 1, '早起晨光唤醒 (06:30)', '🌅', '#10B981', 7, 0),
(2, 1, '深度专注工作 4h', '💻', '#8B5CF6', 5, 0),
(3, 1, '硬核健身/有氧 45m', '🏋️', '#F59E0B', 4, 0),
(4, 1, '睡前阅读与冥想 30m', '📖', '#3B82F6', 7, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);
