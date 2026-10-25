// 文件版本：V0.01
// 客人引导提示页（PGM「受控的十一类终端：客人手机应用、客人小程序、Web 前端（业务人员，客人无网页端）」）：
// 网页端只给业务人员使用；客人在浏览器打开根路径或客人链接时只看到这句提示——不含任何客人功能、不调接口。
export function CustomerNoticePage(): React.ReactElement {
  return (
    <main data-testid="customer-notice" style={{ maxWidth: 480, margin: "15vh auto 0", padding: "0 16px", textAlign: "center" }}>
      <h1 style={{ fontSize: 22, margin: "0 0 12px" }}>客人请使用综合体官方 App 或微信小程序</h1>
      <p style={{ color: "#4b5563", lineHeight: 1.6, margin: 0 }}>
        购票、订台、查看订单与二维码、积分与储值等，请在综合体官方 App 或微信小程序中办理。网页端仅供工作人员使用。
      </p>
    </main>
  );
}
