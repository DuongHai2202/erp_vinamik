UPDATE identity.permission
SET description = REPLACE(description, 'nguyên vật liệu', 'vật tư')
WHERE description LIKE '%nguyên vật liệu%';
