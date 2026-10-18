// 文件版本：V0.01
export type PayrollStatement = { id: string; userId?: string; month?: string; baseCent?: number; commissionCent?: number; bonusCent?: number; penaltyCent?: number; leaveDeductionCent?: number; taxCent?: number; totalCent?: number; payerCompanyId?: string | null; taxAssessmentId?: string | null; taxVersionId?: string | null };
export type PayrollCalculation = { id: string; userId: string; month: string; statementKind: string; originEventId: string | null; schemeVersionId: string; factsId: string; projectId: string; merchantId: string | null; payerCompanyId: string; beforeTaxCent: number; sourceFingerprint: string; calculationSnapshot: Record<string, unknown>; status: string; createdAt: string };
export type PayrollTaxAssessment = { id: string; userId: string; month: string; payrollCalculationId: string | null; incomeCent: number; taxCent: number; status: string; professionalCalculationReference: string | null };
export type PayrollPendingItem = { code: string; sourceId: string; message: string };
export type PayrollLine = { id: string; componentCode: string; componentType: string; description: string; grossCent: number; embeddedLeaveCent: number; additionalLeaveCent: number; amountCent: number; sourceType: string; sourceId: string; calculationSnapshot: Record<string, unknown> };
export type PayrollWorkResult = { segments: { shiftId: string; date: string; kind: string; seconds: number; payrollMonth: string }[]; ordinarySeconds: number; restDaySeconds: number; holidaySeconds: number; ordinaryOvertimeCent: number; restDayOvertimeCent: number; holidayOvertimeCent: number; pendingItems: PayrollPendingItem[] };
export type PayrollStatementDetail = {
  statement: PayrollStatement; status: string; statementKind: string; schemeVersionId: string | null; factsId: string | null;
  paymentDueDate: string | null; paymentDueBasis: Record<string, unknown>; arApItemId: string | null; paidAmountCent: number;
  lines: PayrollLine[]; worktime: PayrollWorkResult | null;
  corrections: { id: string; originalStatementId: string; originalMonth: string; sourceReviewId: string; sourceAdjustmentId: string; amountCent: number | null; status: string; targetStatementId: string | null; calculationSnapshot: Record<string, unknown> }[];
  payments: { id: string; financePaymentId: string; amountCent: number; payMethod: string; payRef: string; paidAt: string }[];
  pendingItems: PayrollPendingItem[];
};


export type PayrollComponent = { code: string; label: string; type: string; method: string; amountCent: number; unit: string; leaveMode: string; calculationBasis: string };
export type PayrollPerformanceRules = { basePoints: number; minPoints: number; maxPoints: number; gradeCoefficients: Record<string, number>; aCondition: string; cMissedPeriods: number; assessmentPeriodMonths: number; deductionRules: Record<string, number>; deductionRuleReviewId: string | null };
export type PayrollWorktimeTerms = { system: string; standardDailyMinutes: number; standardWeeklyMinutes: number; hourlyBaseCent: number; ordinaryOvertimeBps: number; restDayOvertimeBps: number; holidayOvertimeBps: number; approvalBasis: string; calculationBasis: string };
export type PayrollProposal = { id: string; status: string; reviewId: string | null; evidenceAttachmentIds: string[]; reason: string };
export type PayrollScheme = PayrollProposal & { userId: string; projectId: string; merchantId: string | null; payerCompanyId: string; version: string; effectiveFrom: string; effectiveTo: string | null; positionCode: string; components: PayrollComponent[]; performanceRules: PayrollPerformanceRules | null; worktimeTerms: PayrollWorktimeTerms; paymentBasis: string };
export type PayrollCalendar = PayrollProposal & { projectId: string; version: string; startDate: string; endDate: string; days: { date: string; kind: string }[]; sourceBasis: string };
export type PayrollComponentFact = { componentCode: string; quantity: number; denominator: number; embeddedLeaveCent: number; appliedLeaveIds: string[]; evidenceBasis: string };
export type PayrollPeriodFacts = PayrollProposal & { userId: string; month: string; projectId: string; merchantId: string | null; payerCompanyId: string; schemeVersionId: string; calendarVersionId: string; componentFacts: PayrollComponentFact[]; performanceFacts: { aConditionMet: boolean; targetMet: boolean; evidenceBasis: string } | null; cycleStart: string; cycleEnd: string; cycleStandardMinutes: number; restCompensatedMinutes: Record<string, number>; agreedDueDate: string; statutoryDueDate: string | null };
export type PayrollScoreRuleVersion = PayrollProposal & { projectId: string; merchantId: string | null; ownerCompanyId: string; version: string; effectiveFrom: string; deductions: Record<string, number> };
export type PayrollScoreAdjustment = { id: string; userId: string; month: string; points: number; ruleCode: string; reason: string; reviewId: string; status: string; reversalOf: string | null; createdAt: string };
