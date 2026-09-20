-- 프론트 요구사항(ANALYSIS.md §2.1/§2.7, 공간 등록 화면 "사진 없이도 등록됩니다")과 실제 등록
-- API 동작을 실기동 테스트로 맞춰보니, thumbnail_url이 NOT NULL이라 사진 없는 등록이 아예
-- 불가능했다. 사진이 없으면 프론트가 카테고리 색면으로 폴백하는 설계이므로, 백엔드도 이를
-- 허용해야 한다.
ALTER TABLE spaces.spaces ALTER COLUMN thumbnail_url DROP NOT NULL;
ALTER TABLE popups.popups ALTER COLUMN thumbnail_url DROP NOT NULL;
