# M005 — 根运行与测试配置审计收口
版本：V0.01

审计结论：PASS。M005/322，4个模块文件232行、127根scripts、9个基础编译选项，冻结GEN-001—GEN-018全部完成三线双审。B3执行1次，方案复核已修2次沿用，运行失败重跑0。控制V0.17；源工作区160:/home/james/complex-management/main。先完成B5后四值全等并写ledger PASS，再创建本文件。

## 修复及证据

本批M005-001—006：Node引擎支持范围与既定依赖一致并同步lock根；真实端口先数值化并限制合法整数范围；mock/real选择器覆盖已有32/18条；删除smoke:p0-ops死别名；缺真实标志立即失败；默认E2E服务只监听loopback。其余007—012为技术登记，全部12ID当前队列否，无添加/HTML申请或待答。

修复前同一回归检查8个失败，修复后0；当前B5重新验证12断言全通过。133脚本节点/107npm边无悬空无环、75node入口语法过；TypeScript正反例0 vs2322/7006；实际npm/Vite参数解析确认127.0.0.1:5173；原生引擎矩阵与root-lock深一致。UI仅验证列表收集，未宣称50条业务断言执行通过。未在B5安装依赖、激活corepack或启动生产/数据库。

| 环节 | 原始证据 |
|---|---|
| B1全量/B2整批归并 | M005-b1-self-raw.md；M005-b1-grok-raw.md（首份外席B1因索引泄漏不采票，保留；新独立B1从byte36359起，整批B2最终双PASS） |
| B4-r1 | M005-b4-r1-self-raw.md；M005-b4-r1-grok-raw.md；两席审计/越界PASS |
| B5-r1 | M005-b5-r1-self-raw.md；M005-b5-r1-grok-raw.md；两席全模块PASS |

B3 before=4e82fa54f5fcf4589bf2c8ad526ab64c9d2c2a4e，after=214490504e59405a1c6ef5f83f172a6f8be373fe；完整未过滤binary diff3529字节/8hunk/4路径/SHA42a88e82d010599b68b2e30414b2e35d2731cc5e6c6b81931ea55f4312779c26，原文及逐块ID归因在两席B4raw。

Grok B5真实exit0（15:39:23 UTC接收），39052字节/416行/SHAcf0fdfa8f179392f93161e64afed533cea92fd84da79a5d1eda478a4451652a3；本方读取前独立含补核60471字节/SHA2dab6ab2974fa7dba20ee0e9246a3dfedd6376f69e4ca34dfdac5312cba21ec7，追加接收说明不改原前缀、不作新盲审票。

## 四值与源快照

actual=sidecar=ledger=当前B5 Grok最后哈希：
23a86b8fabc3f14001def51f17a13af53be3384b7cea5ab5b3c8d57b699072de
LOCK-AMEND-COUNT: 0

- package.json：350999a90ddf85b2d3dcbee090dd593fb7faff18d0f467f0a608d8b3664d473f
- package-lock.json：d3676e896b885b6bbb979440bb51b9dfe60728735cee000e528ab5d1791bcfef
- playwright.config.ts：c1c72998765d8aa42b83f11c557a1e8ea886eb1c557207aab1ded8d5d5bd4700
- playwright.real-backend.config.ts：956e9484517c7620833962e3ec45d63e940e8de452a201ab9a9fd148e5355132

另tsconfig.base.json为13d49b947d918bf064b5f98ceaa049e1c954f88eafe736c438cd4372b44696db。package-lock只做本ID必要根镜像及文件版本，不重开M004；三业务HTML及35既有scratch保持。

## 后续执行

按操作员已授权的逐模块发布顺序，基于本次已审字节创建release worktree，提交、走propose-change.sh实际predeploy后推送/PR、走merge-pr.sh等待对应HEAD的CI通过后合并，再ff同步160。发布以实际PR/CI/merge回执证明，未完成前不开始M006。禁止借本收口文件声称发布已经完成。
