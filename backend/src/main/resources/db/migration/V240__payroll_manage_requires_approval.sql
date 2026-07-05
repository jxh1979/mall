-- 薪资资金安全③【P1】关账/调薪单一权限通吃无审批(fix/payroll-safety)。
--   缺陷:staff.payroll.manage(risk=high 但 requires_approval=FALSE)一把通吃调薪+奖惩+关账。
--   修:置 requires_approval=TRUE,授出该高危权限须走既有授权审批链(AccessService.requireGrantApprovalIfNeeded,
--       与 staff.tax_bracket.manage 同范式);不自造 per-action 审批拦截器。
UPDATE permissions
   SET requires_approval = TRUE
 WHERE permission_code = 'staff.payroll.manage';
