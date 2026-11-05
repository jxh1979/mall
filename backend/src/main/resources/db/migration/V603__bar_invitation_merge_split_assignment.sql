-- 文件版本：V0.01
-- 第十八批 T 段第 167 条 167-R3a：并台期间新发出的邀请及其形成的成员，首次拆台前由授权人员分配到某一张原台并留痕。
-- 依据：BSM:1079「并台期间新发出的邀请，以及由这些邀请形成的已接受成员、已入场成员，在首次拆台前由授权人员逐一分配到当前合并台所包含的
-- 某一张原台并留痕；已入场成员只调整所属原台，不得被移出，已占名额不得释放。」；ENUM:114 同句。
-- 签发时打标记、不按时间推算：单拆压缩层级后来源链会改指，按并台调整记录的时间推不准签发时是否处于并台。
ALTER TABLE bar_reservations ADD COLUMN invitation_merge_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bar_reservations ADD COLUMN invitation_assigned_session_id VARCHAR(64) REFERENCES bar_table_sessions(id);
COMMENT ON COLUMN bar_reservations.invitation_merge_pending IS '本次签发邀请时订台所在台组正处于并台（并台期间新发出的邀请）；吊销后再签发按当时是否并台重置';
COMMENT ON COLUMN bar_reservations.invitation_assigned_session_id IS '并台期间新发出的邀请在首次拆台时分配到的原台会话；为空＝未分配或无须分配（按订台所在会话归原台）';

ALTER TABLE bar_reservation_invitation_members ADD COLUMN merge_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bar_reservation_invitation_members ADD COLUMN assigned_session_id VARCHAR(64) REFERENCES bar_table_sessions(id);
COMMENT ON COLUMN bar_reservation_invitation_members.merge_pending IS '由并台期间新发出、接受时尚未分配的邀请形成的成员，首次拆台前须分配到某一张原台';
COMMENT ON COLUMN bar_reservation_invitation_members.assigned_session_id IS '成员所属原台会话（拆台时人工分配，或接受时继承邀请的分配）；为空＝按订台所在会话归原台';
