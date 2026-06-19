import {
  CalendarOutlined,
  HomeOutlined,
  ShoppingOutlined,
  StarOutlined,
  UnorderedListOutlined,
  WalletOutlined,
} from "@ant-design/icons";
import { Layout, Menu } from "antd";
import { useMemo, useState } from "react";

import { CustomerAuthShell } from "./components/CustomerAuthShell";
import { CustomerService } from "./client/customerService";
import { customerPhoneLogin } from "./client/customerClient";
import { BarReservationPage } from "./pages/BarReservationPage";
import { BuyTicketPage } from "./pages/BuyTicketPage";
import { HomePage } from "./pages/HomePage";
import { MyOrdersPage } from "./pages/MyOrdersPage";
import { PointsPage } from "./pages/PointsPage";
import { WalletPage } from "./pages/WalletPage";
import type { CustomerSession } from "./types";

const { Header, Content, Sider } = Layout;

const STORAGE_KEY = "customer-session";

type PageKey = "home" | "buy" | "reserve" | "orders" | "points" | "wallet";

function loadSession(): CustomerSession | null {
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as CustomerSession;
    if (parsed.expiresAt && Date.parse(parsed.expiresAt) < Date.now()) return null;
    return parsed.customerToken ? parsed : null;
  } catch {
    return null;
  }
}

export default function CustomerApp(): React.ReactElement {
  const [session, setSession] = useState<CustomerSession | null>(() => loadSession());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageKey>("home");

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
        customerToken: result.customerToken,
        expiresAt: result.expiresAt,
      };
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      setSession(next);
    } catch (err) {
      setError(err instanceof Error ? err.message : "登录失败");
    } finally {
      setLoading(false);
    }
  };

  const onLogout = () => {
    window.localStorage.removeItem(STORAGE_KEY);
    setSession(null);
    setPage("home");
  };

  if (!session || !service) {
    return <CustomerAuthShell error={error} loading={loading} onLogin={onLogin} />;
  }

  const menuItems = [
    { key: "home", icon: <HomeOutlined />, label: "首页 / 票种" },
    { key: "buy", icon: <ShoppingOutlined />, label: "购票" },
    { key: "reserve", icon: <CalendarOutlined />, label: "订台" },
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
          {page === "orders" ? <MyOrdersPage service={service} /> : null}
          {page === "points" ? <PointsPage service={service} /> : null}
          {page === "wallet" ? <WalletPage service={service} /> : null}
        </Content>
      </Layout>
    </Layout>
  );
}
