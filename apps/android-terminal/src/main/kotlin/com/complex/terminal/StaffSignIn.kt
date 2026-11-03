package com.complex.terminal

// 文件版本：V0.01

/**
 * 第十八批 N 段第 120 条 120-R1（BSM:986「启用售票能力的一体机可由授权服务人员登录后辅助客人购票和查询订单……」；
 * BSM:990「……必须由授权员工记录到订单、账单、日结和对账里。」；BSM:992「工程师可在授权后填写游乐设施定期检修记录……」）：
 * 一体机常驻员工会话不动，售票、现金/POS 登记、检修、主管确认由授权员工当场登录，这一段请求带授权员工的令牌，做完（含出错）即不再带。
 * 120-R1 复核：令牌只设在调用线程上（[TerminalApiClient.staffToken]，[block] 在本线程同步执行），常驻令牌 [TerminalApiClient.accessToken] 不动——
 * 别的线程同时发的请求仍带常驻令牌，两段后台请求重叠也不会把常驻令牌换成授权员工令牌。
 */
fun <T> TerminalApiClient.asStaff(token: String, block: () -> T): T {
  val outer = staffToken.get()
  staffToken.set(token)
  try {
    return block()
  } finally {
    staffToken.set(outer)
  }
}
