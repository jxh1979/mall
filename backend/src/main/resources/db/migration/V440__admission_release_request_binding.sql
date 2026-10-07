-- 文件版本：V0.02
-- XM-ONESHOT-RETURN B01（治理地图第 303 行「全局原则 · 同时操作以先成功为准」：同一个操作被重复提交，系统只认一次；
-- 操作员 2026-09-25 确认的追加句：只能用一次的动作，只有这次操作成功完成、返回结果确认后才算用掉；
-- 同一次操作重复提交时，返回第一次的结果）：
-- 准入闸逐笔放行时，在复核单上记下用掉它的那次请求——幂等键 + 请求摘要（方法、路径与查询串的 SHA-256，十六进制 64 位）。
-- 以同一幂等键重放同一请求、且放行人那次请求的幂等记录已成功完成时，准入闸按重放放行，不再用掉放行单、
-- 不再新建待审复核单；能不能拿到第一次的结果由幂等层按提交范围判（同范围返回第一次的结果，其他范围拒绝）。
-- 只加可空列，不回填：存量放行没有请求信息，其重放照旧拦截。复核单仍只禁删除（V148 触发器不变）。
--
-- M724-F16（治理地图第 528 行「每次只能对当前这一笔放行、不得整类关闭校验」、第 303 行「每次只对当前这一笔生效并留痕」；
-- 操作员 2026-10-07 裁决（decisions/20260924T114205Z-M724-iter3-release-bound-to-blocked-request.md 第 1 条）：
-- 「批准只能被『被拦下的那次提交的同样内容』用掉，内容不同的另一笔照常拦截送审。」）：
-- blocked_content_digest＝被准入闸拦下的那次提交的请求内容摘要（受闸方法全部实参去掉提交编号后的规范 JSON 的 SHA-256，十六进制 64 位）。
-- 只有准入闸建的复核单才有值；已批准未用的放行单只能被同发起人、同动作、同对象且摘要相同的提交用掉。
-- 它与 released_request_digest（方法、路径与查询串的摘要）不是一回事，后者含义不变。
-- 只加可空列，不回填；没有内容摘要的复核单不能用于放行，须重新送审。索引不动（消费查询仍走 V388 的部分索引，摘要条件在其上过滤）。
ALTER TABLE supervisor_review_decisions
    ADD COLUMN released_idempotency_key TEXT,
    ADD COLUMN released_request_digest VARCHAR(64),
    ADD COLUMN blocked_content_digest VARCHAR(64);

CREATE INDEX idx_supervisor_review_decisions_released_request
    ON supervisor_review_decisions (released_idempotency_key, released_request_digest)
    WHERE released_idempotency_key IS NOT NULL;
