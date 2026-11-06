-- 사용자가 직접 입력하는 공간 텍스트 칸을 255자에서 1000자로 늘린다. 요청 DTO의 @Size(max = 1000)와 맞춘다.
-- 구역 이름은 "공간 이름 · 구역 이름"으로 합쳐 name에 저장되고, 채팅방은 그 이름을 space_name에 복사해 둔다.
ALTER TABLE spaces.spaces
    ALTER COLUMN name TYPE VARCHAR(1000),
    ALTER COLUMN floor TYPE VARCHAR(1000),
    ALTER COLUMN parking_info TYPE VARCHAR(1000);

ALTER TABLE spaces.addresses
    ALTER COLUMN detail_address TYPE VARCHAR(1000);

ALTER TABLE spaces.space_panorama_scenes
    ALTER COLUMN name TYPE VARCHAR(1000);

ALTER TABLE chating.chat_rooms
    ALTER COLUMN space_name TYPE VARCHAR(1000);
