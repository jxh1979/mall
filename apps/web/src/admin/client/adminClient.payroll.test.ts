// 文件版本：V0.01
import { afterEach, expect, it, vi } from "vitest";
import { AdminApi } from "./adminClient";

afterEach(() => vi.unstubAllGlobals());
it("工资接口保留等级及扣分规则字典键，普通字段仍双向转换", async () => {
  const fetchMock = vi.fn().mockImplementation(async () => new Response(JSON.stringify({ code: "OK", data: { performance_rules: { grade_coefficients: { A: 1.2, B: 1, C: 0.8 }, deduction_rules: { LATE_A_1: 10, late_shift_2: 5 } } } })));
  vi.stubGlobal("fetch", fetchMock);
  const api = new AdminApi();
  const rules = { gradeCoefficients: { A: 1.2, B: 1, C: 0.8 }, deductionRules: { LATE_A_1: 10, late_shift_2: 5 } };
  await expect(api.get("/api/v1/staff/payroll/schemes?user_id=employee")).resolves.toEqual({ performanceRules: rules });
  await api.post("/api/v1/staff/payroll/schemes", { idempotencyKey: "payroll-dictionary", performanceRules: rules });
  expect(JSON.parse(String((fetchMock.mock.calls[1]![1] as RequestInit).body)).performance_rules).toEqual({ grade_coefficients: rules.gradeCoefficients, deduction_rules: rules.deductionRules });
});
