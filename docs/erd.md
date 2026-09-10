// ENUMS
Enum Role {
  USER
  ADMIN
}

Enum ApprovalStatus {
  PENDING
  APPROVED
  REJECTED
}

Enum MatchingStatus {
  REQUESTED
  APPROVED
  REJECTED
  CANCELED
}

Enum SpaceCategory {
  PRACTICE_ROOM
  STUDIO
  MEETING_ROOM
  PARTY_ROOM
  CLASSROOM
  POPUP_STORE
  OFFICE
  HALL
  CAFE
  OTHER
}

// USER AGGREGATE
Table Users {
  id varchar [pk, note: "UUID"]
  image_url varchar [note: "프로필 이미지 URL, 없으면 기본 이미지 사용"]
  email varchar [unique, not null, note: "로그인 ID"]
  password varchar [note: "OAuth 가입자는 null"]
  auth_provider varchar [note: "OAuth 제공자(GOOGLE/NAVER/KAKAO). 로컬 가입자는 null"]
  provider_id varchar [note: "소셜 로그인 제공자 유저 식별자. 로컬 가입자는 null"]
  name varchar [not null, note: "사용자 이름 또는 브랜드명"]
  role Role [not null, note: "ADMIN, USER 구분"]
  phone varchar [unique]
  intro text
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
  deleted_at datetime
}

// SPACE AGGREGATE
Table Spaces {
  id bigint [pk, increment]
  host_id varchar [not null]
  address_id bigint [not null]
  name varchar [not null]
  description text
  ai_summary text
  thumbnail_url varchar [not null]
  price_per_hour int [not null]
  like_count int [not null, note: "공간 좋아요 등록/취소 시 함께 증감"]
  admin_status ApprovalStatus [not null, note: "코드가 생성 시점에 PENDING으로 채움 (DB 레벨 default 없음)"]
  is_active boolean [not null, note: "코드가 생성 시점에 true로 채움 (DB 레벨 default 없음)"]
  category SpaceCategory [not null]
  phone varchar
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
  deleted_at datetime
}

Table Addresses {
  id bigint [pk, increment]
  sido varchar [not null, note: "시/도"]
  sigungu varchar [not null, note: "시/군/구"]
  eup_myeon_dong varchar [note: "읍/면/동"]
  road_address varchar [not null, note: "도로명 주소"]
  jibun_address varchar [note: "지번 주소"]
  detail_address varchar [note: "상세 주소"]
  postal_code varchar [note: "우편번호"]
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
}

Table Space_Images {
  id bigint [pk, increment]
  space_id bigint [not null]
  image_url varchar [not null]
}

Table Space_Schedules {
  id bigint [pk, increment]
  space_id bigint [not null]
  start_time datetime [not null]
  end_time datetime [not null]
  is_bookable boolean [not null, note: "호스트가 해당 시간대 예약을 받는지 여부. 코드가 생성 시점에 true로 채움"]
}

// MATCHING AGGREGATE
Table Matchings {
  id bigint [pk, increment]
  space_id bigint [not null]
  host_id varchar [not null]
  seller_id varchar [not null]
  start_time datetime [not null]
  end_time datetime [not null]
  total_price int [not null, note: "계약 성사 시점의 총 결제 금액 박제"]
  status MatchingStatus [not null, note: "코드가 생성 시점에 REQUESTED로 채움 (DB 레벨 default 없음)"]
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
}

Table Matchings_Alarm {
  id bigint [pk, increment]
  user_id varchar [not null, note: "Users.id(varchar)를 참조하므로 bigint가 아니라 varchar"]
  matching_id bigint [not null]
  description text [not null]
  is_read boolean [not null, note: "코드가 생성 시점에 false로 채움 (DB 레벨 default 없음)"]
}

// POPUP AGGREGATE
Table Popups {
  id bigint [pk, increment]
  matching_id bigint [unique, not null, note: "매칭 하나당 팝업 하나만 생성 가능"]
  space_id bigint [not null]
  seller_id varchar [not null]
  title varchar [not null]
  description text
  thumbnail_url varchar [not null]
  ai_brand_summary text
  start_time datetime [not null]
  end_time datetime [not null]
  view_count int [not null, note: "팝업 상세 조회(GET /popups/{id})할 때마다 증가. 실제로 동작함"]
  like_count int [not null, note: "팝업 좋아요 등록/취소 시 함께 증감. 추천 정렬 기준"]
  created_at datetime [default: `now()`]
}

// LIKE AGGREGATE
Table Popup_Likes {
  id bigint [pk, increment]
  popup_id bigint [not null]
  user_id varchar [not null]
  created_at datetime [default: `now()`]

  indexes {
    (popup_id, user_id) [unique, name: "uk_popup_likes_popup_id_user_id", note: "사용자당 팝업 하나에 좋아요 한 건만 허용"]
    (user_id, created_at) [name: "idx_popup_likes_user_id_created_at", note: "향후 내 좋아요 목록 조회용"]
  }
}

Table Space_Likes {
  id bigint [pk, increment]
  space_id bigint [not null]
  user_id varchar [not null]
  created_at datetime [default: `now()`]

  indexes {
    (space_id, user_id) [unique, name: "uk_space_likes_space_id_user_id", note: "사용자당 공간 하나에 좋아요 한 건만 허용"]
    (user_id, created_at) [name: "idx_space_likes_user_id_created_at", note: "향후 내 좋아요 목록 조회용"]
  }
}

Table Space_Review_Likes {
  id bigint [pk, increment]
  space_review_id bigint [not null]
  user_id varchar [not null]
  created_at datetime [default: `now()`]

  indexes {
    (space_review_id, user_id) [unique, name: "uk_space_review_likes_review_id_user_id", note: "사용자당 공간 리뷰 하나에 좋아요 한 건만 허용"]
    (user_id, created_at) [name: "idx_space_review_likes_user_id_created_at", note: "향후 내 좋아요 목록 조회용"]
  }
}

Table Popup_Review_Likes {
  id bigint [pk, increment]
  popup_review_id bigint [not null]
  user_id varchar [not null]
  created_at datetime [default: `now()`]

  indexes {
    (popup_review_id, user_id) [unique, name: "uk_popup_review_likes_review_id_user_id", note: "사용자당 팝업 리뷰 하나에 좋아요 한 건만 허용"]
    (user_id, created_at) [name: "idx_popup_review_likes_user_id_created_at", note: "향후 내 좋아요 목록 조회용"]
  }
}

// REVIEW AGGREGATE
Table Space_Reviews {
  id bigint [pk, increment]
  matching_id bigint [unique, not null, note: "매칭당 공간 리뷰는 한 건만 작성 가능"]
  space_id bigint [not null]
  user_id varchar [not null, note: "리뷰 작성자(매칭의 seller)"]
  rating int [not null, note: "1~5점"]
  content text [not null]
  like_count int [not null, note: "공간 리뷰 좋아요 등록/취소 시 함께 증감"]
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
}

Table Popup_Reviews {
  id bigint [pk, increment]
  popup_id bigint [not null]
  user_id varchar [not null]
  rating int [not null, note: "1~5점"]
  content text [not null]
  verification_type varchar [not null, note: "QR 또는 RECEIPT"]
  verification_payload text [not null, note: "검증 대상 원문. 현재는 저장만 하며 실제 검증 전에는 is_verified=false"]
  is_verified boolean [not null, note: "코드가 생성 시점에 false로 채움"]
  like_count int [not null, note: "팝업 리뷰 좋아요 등록/취소 시 함께 증감"]
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]

  indexes {
    (popup_id, user_id) [unique]
  }
}

// CHAT AGGREGATE
Table Chat_Rooms {
  id bigint [pk, increment]
  space_id bigint [not null]
  host_id varchar [not null]
  host_name varchar [not null, note: "채팅방 생성 시점의 호스트 이름 스냅샷 (목록 조회 시 User 재조회 방지)"]
  seller_id varchar [not null]
  seller_name varchar [not null, note: "채팅방 생성 시점의 판매자 이름 스냅샷"]
  space_name varchar [not null, note: "채팅방 생성 시점의 공간 이름 스냅샷"]
  created_at datetime [default: `now()`]
}

Table Chat_Messages {
  id bigint [pk, increment]
  chat_room_id bigint [not null]
  sender_id varchar [not null]
  content text [not null]
  is_read boolean [not null, note: "메시지 읽음 여부 확인용. 코드가 생성 시점에 false로 채움"]
  created_at datetime [default: `now()`]
}

// CHATBOT AGGREGATE
// 별도 FastAPI 서비스(ChatBot 레포)가 소유. 기존 users/spaces/matchings 스키마는 읽기 전용으로만 접근하고,
// 이 chatbot 스키마(conversations/messages)만 직접 쓴다.
Table Conversations {
  id uuid [pk]
  user_id varchar [not null, note: "Users.id 참조, 스키마 간 FK 없음"]
  created_at datetime [default: `now()`]
  updated_at datetime [default: `now()`]
}

Table Messages {
  id bigint [pk, increment]
  conversation_id uuid [not null]
  role varchar [not null, note: "user | assistant | tool"]
  content text [not null]
  recommended_space_ids "bigint[]" [note: "이 메시지에서 추천된 Spaces.id 목록, 배열 컬럼이라 FK 없음"]
  tool_call jsonb
  created_at datetime [default: `now()`]
}


// RELATIONSHIPS
// 참고: 실제 DB에는 이 관계들에 대한 FK 제약이 걸려있지 않습니다.
// 스키마가 users/spaces/matchings/alarms/popups/likes/chating으로 분리되어 있고,
// 나중에 다시 별도 서비스/DB로 쪼갤 수 있도록 스키마 간 FK를 의도적으로 걸지 않았습니다.
Ref: Spaces.host_id > Users.id
Ref: Space_Images.space_id > Spaces.id
Ref: Space_Schedules.space_id > Spaces.id
Ref: Spaces.address_id > Addresses.id
Ref: Matchings.space_id > Spaces.id
Ref: Matchings.seller_id > Users.id
Ref: Matchings.host_id > Users.id
Ref: Matchings_Alarm.user_id > Users.id
Ref: Matchings_Alarm.matching_id > Matchings.id
Ref: Popups.matching_id > Matchings.id
Ref: Popups.space_id > Spaces.id
Ref: Popups.seller_id > Users.id
Ref: Popup_Likes.popup_id > Popups.id
Ref: Popup_Likes.user_id > Users.id
Ref: Space_Likes.space_id > Spaces.id
Ref: Space_Likes.user_id > Users.id
Ref: Space_Review_Likes.user_id > Users.id
Ref: Space_Review_Likes.space_review_id > Space_Reviews.id
Ref: Popup_Review_Likes.user_id > Users.id
Ref: Popup_Review_Likes.popup_review_id > Popup_Reviews.id
Ref: Space_Reviews.matching_id > Matchings.id
Ref: Space_Reviews.space_id > Spaces.id
Ref: Space_Reviews.user_id > Users.id
Ref: Popup_Reviews.popup_id > Popups.id
Ref: Popup_Reviews.user_id > Users.id
Ref: Chat_Rooms.space_id > Spaces.id
Ref: Chat_Rooms.host_id > Users.id
Ref: Chat_Rooms.seller_id > Users.id
Ref: Chat_Messages.chat_room_id > Chat_Rooms.id
Ref: Chat_Messages.sender_id > Users.id

// 아래 관계는 예외적으로 실제 FK 제약이 걸려 있음 (같은 chatbot 스키마 내부, ON DELETE CASCADE)
Ref: Messages.conversation_id > Conversations.id
// 아래는 문서화 목적 참조일 뿐, 스키마 간 FK 없음 (위 전역 노트와 동일)
Ref: Conversations.user_id > Users.id


// TABLE GROUPS
TableGroup User_Aggregate {
  Users
}
TableGroup Space_Aggregate {
  Spaces
  Space_Images
  Space_Schedules
  Addresses
}
TableGroup Matching_Aggregate {
  Matchings
  Matchings_Alarm
}
TableGroup Popup_Aggregate {
  Popups
}
TableGroup Like_Aggregate {
  Popup_Likes
  Space_Likes
  Space_Review_Likes
  Popup_Review_Likes
}
TableGroup Review_Aggregate {
  Space_Reviews
  Popup_Reviews
}
TableGroup Chat_Aggregate {
  Chat_Rooms
  Chat_Messages
}
TableGroup Chatbot_Aggregate {
  Conversations
  Messages
}
