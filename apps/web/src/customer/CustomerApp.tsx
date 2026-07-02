import {
  CalendarOutlined,
  HomeOutlined,
  ShoppingOutlined,
  StarOutlined,
  UnorderedListOutlined,
  WalletOutlined,
} from "@ant-design/icons";
import { Layout, Menu } from "antd";
import { useEffect, useMemo, useState } from "react";

import { CustomerAuthShell } from "./components/CustomerAuthShell";
import { CustomerService } from "./client/customerService";
import { customerLogout, customerPhoneLogin, customerRestoreSession } from "./client/customerClient";
import { BarInvitationPage } from "./pages/BarInvitationPage";
import { BarReservationPage } from "./pages/BarReservationPage";
import { BarScanOrderPage } from "./pages/BarScanOrderPage";
import { BuyTicketPage } from "./pages/BuyTicketPage";
import { HomePage } from "./pages/HomePage";
import { MyOrdersPage } from "./pages/MyOrdersPage";
import { PointsPage } from "./pages/PointsPage";
import { WalletPage } from "./pages/WalletPage";
import type { CustomerSession } from "./types";

const { Header, Content, Sider } = Layout;

type PageKey = "home" | "buy" | "reserve" | "invitation" | "scan" | "orders" | "points" | "wallet";

// 3.11 多页入口：/ticketing /bar /amusement 直达对应功能页，而非默认客人首页。
function initialPageFromPath(): PageKey {
  const path = typeof window !== "undefined" ? window.location.pathname : "/";
  if (path.startsWith("/ticketing") || path.startsWith("/amusement")) return "buy";
  if (path.startsWith("/bar")) return "scan";
  return "home";
}

export default function CustomerApp(): React.ReactElement {
  // 3.3 免登录（用户拍板终态）：token 不落任何可脚本读取的存储（防 XSS 窃取），
  // 登录后凭证在后端种的 httpOnly+SameSite=Strict cookie 里；刷新页面凭 cookie 恢复会话免重登。
  const [session, setSession] = useState<CustomerSession | null>(null);
  const [restoring, setRestoring] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageKey>(initialPageFromPath());

  useEffect(() => {
    let cancelled = false;
    void customerRestoreSession().then((info) => {
      if (cancelled) return;
      if (info) {
        setSession({
          customerId: info.customerId,
          maskedPhone: info.maskedPhone,
          customerToken: "",
          expiresAt: info.expiresAt ?? "",
        });
      }
      setRestoring(false);
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const service = useMemo(
    () => (session ? new CustomerService(session.customerToken) : null),
    [session],
  );

  const onLogin = async (values: { phone: string; smsCode: string }) => {
    setLoading(true);
    setError(null);
    try {
      const result = await customerPhoneLogin({ phone: values.phone, smsCode: values.smsCode, scene: "login" });
      const next: CustomerSession = {
        customerId: result.customerId,
        maskedPhone: result.maskedPhone,
        // 凭证走 httpOnly cookie，前端不留 token（内存也不留，XSS 面最小化）。
        customerToken: "",
        expiresAt: result.expiresAt,
      };
      setSession(next);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败");
    } finally {
      setLoading(false);
    }
  };

  const onLogout = () => {
    void customerLogout();
    setSession(null);
    setPage("home");
  };

  if (restoring) {
    return <CustomerAuthShell error={null} loading onLogin={onLogin} />;
  }
  if (!session || !service) {
    return <CustomerAuthShell error={error} loading={loading} onLogin={onLogin} />;
  }

  const menuItems = [
    { key: "home", icon: <HomeOutlined />, label: "首页 / 票种" },
    { key: "buy", icon: <ShoppingOutlined />, label: "购票" },
    { key: "reserve", icon: <CalendarOutlined />, label: "订台" },
    { key: "invitation", icon: <CalendarOutlined />, label: "订卡邀请函" },
    { key: "scan", icon: <ShoppingOutlined />, label: "扫码点单" },
    { key: "orders", icon: <UnorderedListOutlined />, label: "我的订单 / 票 / 二维码" },
    { key: "points", icon: <StarOutlined />, label: "积分 / 签到" },
    { key: "wallet", icon: <WalletOutlined />, label: "储值 / 会员" },
  ];

  return (
    <Layout className="app-shell" style={{ minHeight: "100vh" }} data-testid="customer-app">
      <Sider breakpoint="lg" collapsedWidth="0">
        <div className="brand" style={{ color: "#fff", padding: 16 }}>
          综合体客人端
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[page]}
          items={menuItems}
          onClick={(info) => setPage(info.key as PageKey)}
        />
      </Sider>
      <Layout>
        <Header style={{ background: "#fff", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <span data-testid="customer-phone">已登录：{session.maskedPhone}</span>
          <a onClick={onLogout} data-testid="customer-logout">
            退出登录
          </a>
        </Header>
        <Content style={{ padding: 16 }}>
          {page === "home" ? <HomePage service={service} /> : null}
          {page === "buy" ? <BuyTicketPage service={service} /> : null}
          {page === "reserve" ? <BarReservationPage service={service} /> : null}
          {page === "invitation" ? <BarInvitationPage service={service} /> : null}
          {page === "scan" ? <BarScanOrderPage service={service} /> : null}
          {page === "orders" ? <MyOrdersPage service={service} /> : null}
          {page === "points" ? <PointsPage service={service} /> : null}
          {page === "wallet" ? <WalletPage service={service} /> : null}
        </Content>
      </Layout>
    </Layout>
  );
}
