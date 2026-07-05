-- 薪资资金安全④【P2】个税税表无档位不重叠约束(fix/payroll-safety)。
--   缺陷:重叠区间致月结 tax 子查询 JOIN 命中多档 → 同 (user_id,month) 多行 → ON CONFLICT 报错 → 整月关账崩溃(DoS)。
--   修:同 category 下毛收入区间[floor,ceiling]排他不重叠(EXCLUDE USING gist)。
--       NULL ceiling = 无上界(最高档);category 相等用 btree_gist 支持的 = 运算。
--   种子四档相邻不相接([1,400000]/[400001,2500000]/...),不触发约束。
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE payroll_tax_brackets DROP CONSTRAINT IF EXISTS excl_tax_bracket_no_overlap;
ALTER TABLE payroll_tax_brackets
    ADD CONSTRAINT excl_tax_bracket_no_overlap
    EXCLUDE USING gist (
        category WITH =,
        int8range(income_floor_cent, income_ceiling_cent, '[]') WITH &&
    );
