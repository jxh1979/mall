# M006 治理门禁与 CI 基线／文档与接口定义／第1段

版本：V0.01

状态：PASS（当前471模块计划）。锁 SHA256：b012328f72f6df21a1a393c243fcaa276f00aeccf9818038fb9317d910c6695a；LOCK-AMEND-COUNT：1。

范围：docs/delivery-readiness-gates-v0.1.md（V0.12）及 docs/deploy-and-change-flow-v0.1.md（V0.07）；206行、79项冻结判据。

039–043、047六项缺陷经2次B3修复，B4均获双PASS；037/038/044–046登记但非缺陷。B5-r2全模块双PASS，0待修、0新增。原B5-r1两席分歧和历史报告完整保留。

最终完整报告：M006-b5-r2-self-raw.md V0.02（73975 bytes；SHA256 ebc3de35581d664fb8543b784cd887da84b8c89e3e7f13ead8133103bcbc4171）；M006-b5-r2-grok-raw.md V0.02（68180 bytes；SHA256 f65d01f05ba1056c988f747534806da09fb6506cfcacbcc352f74290e8607bef）。Grok真实exit0→本方独立审计完整落盘→完整读取Grok→四值相等→ledger V1.83登记PASS→本收口文件。

验证：本方6项相关校验通过；67个npm引用、17个路径/通配引用存在；表格结构、61段HTML原文摘录、部署15项门禁数组、合并同SHA、实装hook及workflow镜像检查通过。历史首次workflow成功和运行/测试非秘密账号配置为本方额外核验，Grok未独立核验这两项。三份业务HTML未改；无提交、推送、生产发布；不代表全仓或生产准入已通过。

继续 M007：治理门禁与 CI 基线／运行和发布脚本／第1段；不等待程序性确认。


## 现锁重新结项
版本：V0.02
状态：PASS。当前锁V0.14 SHA b0863b95a64b030bcd6e0b120a21b1436cd7bb9738b6ec95640e46cb0407f458，COUNT1。上文V0.01是旧锁历史，原文保留。
唯一两文档206行、79判据完整三线复审，源码本轮未改。B1 selfV0.20/GrokV0.12，B2 selfV0.21/GrokV0.13全差集通过，findingsV3.42十一项037—047当前队列否，旧001—036历史范围保留。空队列跳新增B3/B4，累计B3仍2。
最终B5-r2：selfV0.03 98069bytes SHA f8ef454ae2dbb5f24b59db84bc5fa3b0c4335f7a1dfb29ab48d20111ccca893f；GrokV0.03 101047bytes SHA fb454bcb73947c87bfc8d2c49a2983edd657f8131c7b151e1a3586de059fb4b0。Grok真实exit0，本方先独立封存、后读外席全文，四值全等、双PASS，先ledgerV7.78登记PASS再本收口。
验证：docs/外部证据追踪检查0；67脚本名存在、47表行两列、49父链、部署15项与predeploy相等、workflow镜像一致。root本次未真实PUT/部署/合并/DB；沿用同字节B1loopback类型和main拒绝证据。Grok本次hook0/1/0、workflow active/保护403。
runtime陈旧exit1→M409/M415，PGM台账同步红→M426/M008，入口快照漂移→M435/M436/M008，均作为所属模块B1既有输入，不宣称全仓或上线绿。Grok新增范围外观察：M414生成器L330旧说明未涵盖disabled分支，后续B1判定，不在本模块立缺陷或修复。
无新HTML/业务规则修改、无提交推送发布。继续M007。
