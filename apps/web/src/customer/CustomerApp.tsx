import {
  CalendarOutlined,
  HomeOutlined,
  ShoppingOutlined,
  StarOutlined,
  UnorderedListOutlined,
  WalletOutlined,
} from "@ant-design/icons";
import { ConfigProvider, Layout, Menu } from "antd";
import zhCN from "antd/es/locale/zh_CN";
import dayjs from "dayjs";
import "dayjs/locale/zh-cn";
import { useEffect, useMemo, useState } from "react";

import { stripBase, withBase } from "../basePath";
import { CustomerAuthShell } from "./components/CustomerAuthShell";
import { CustomerService } from "./client/customerService";
import {
  customerLogout,
  customerPhoneLogin,
  customerRestoreSession,
  resetCustomerSessionExpiredNotice,
  setCustomerSessionExpiredHandler,
} from "./client/customerClient";
import { formatApiErrorDetail } from "../shared/apiError";
import { BarInvitationPage } from "./pages/BarInvitationPage";
import { BarReservationPage } from "./pages/BarReservationPage";
import { BarScanOrderPage } from "./pages/BarScanOrderPage";
import { BuyTicketPage } from "./pages/BuyTicketPage";
import { HomePage } from "./pages/HomePage";
import { MyOrdersPage } from "./pages/MyOrdersPage";
import { PointsPage } from "./pages/PointsPage";
import { WalletPage } from "./pages/WalletPage";
import type { CustomerSession } from "./types";

// antd 组件（日期选择器等）中文化；dayjs 同步切中文，避免 "Select date" 类英文文案。
dayjs.locale("zh-cn");

const { Header, Content, Sider } = Layout;

type PageKey = "home" | "buy" | "reserve" | "invitation" | "scan" | "orders" | "points" | "wallet";

const PAGE_KEYS: readonly PageKey[] = ["home", "buy", "reserve", "invitation", "scan", "orders", "points", "wallet"];

// 页面标题单一来源：侧栏菜单与每页 h1（语义地标）共用。
const PAGE_TITLES: Record<PageKey, string> = {
  home: "首页 / 票种",
  buy: "购票",
  reserve: "订台",
  invitation: "订卡邀请函",
  scan: "扫码点单",
  orders: "我的订单 / 票 / 二维码",
  points: "积分 / 签到",
  wallet: "储值 / 会员",
};

// 3.11 多页入口 + 深链修复：
// - /ticketing /bar /amusement 入口页重定向到 /?page=xxx，这里必须解析 ?page=（此前只看 pathname，入口全落首页）；
// - ?page= 支持入口别名（ticketing/amusement→buy，bar→scan）与全部 tab key，刷新/后退凭 URL 恢复当前 tab。
function pageFromLocation(): PageKey {
  if (typeof window === "undefined") return "home";
  const query = new URLSearchParams(window.location.search).get("page");
  if (query) {
    const mapped = query === "ticketing" || query === "amusement" ? "buy" : query === "bar" ? "scan" : query;
    if ((PAGE_KEYS as readonly string[]).includes(mapped)) return mapped as PageKey;
  }
  const path = stripBase(window.location.pathname);
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
  const [errorDetail, setErrorDetail] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState<PageKey>(pageFromLocation());

  // 切 tab 写入历史（/?page=xxx），后退/前进凭 popstate 恢复上一个 tab，后退不再直接退出站点。
  const goTo = (next: PageKey) => {
    setPage(next);
    if (pageFromLocation() !== next) {
      window.history.pushState({ page: next }, "", withBase(next === "home" ? "/" : `/?page=${next}`));
    }
  };

  useEffect(() => {
    const onPopState = () => setPage(pageFromLocation());
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

  // 会话过期全局回登录页：任何页面任何请求 401 → 清会话（回登录视图）+ 顶部提示；真 403 权限不足不登出。
  useEffect(() => {
    setCustomerSessionExpiredHandler(() => {
      setSession(null);
      setError("登录已过期，请重新登录");
      setErrorDetail(null);
    });
    return () => setCustomerSessionExpiredHandler(null);
  }, []);

  // 重新登录成功后复位一次性防抖标记，下次会话再过期仍能触发回登录。
  useEffect(() => {
    if (session) resetCustomerSessionExpiredNotice();
  }, [session]);

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
    setErrorDetail(null);
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
      setErrorDetail(formatApiErrorDetail(err));
    } finally {
      setLoading(false);
    }
  };

  const onLogout = () => {
    void customerLogout();
    setSession(null);
    goTo("home");
  };

  const body =
    restoring ? (
      <CustomerAuthShell error={null} loading onLogin={onLogin} />
    ) : !session || !service ? (
      <CustomerAuthShell error={error} errorDetail={errorDetail} loading={loading} onLogin={onLogin} />
    ) : (
      <Layout className="app-shell" style={{ minHeight: "100vh" }} data-testid="customer-app">
        <a className="skip-link" href="#main-content">跳到主内容</a>
        <Sider breakpoint="lg" collapsedWidth="0">
          <div className="brand" style={{ color: "#fff", padding: 16 }}>
            综合体客人端
          </div>
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[page]}
            items={[
              { key: "home", icon: <HomeOutlined />, label: PAGE_TITLES.home },
              { key: "buy", icon: <ShoppingOutlined />, label: PAGE_TITLES.buy },
              { key: "reserve", icon: <CalendarOutlined />, label: PAGE_TITLES.reserve },
              { key: "invitation", icon: <CalendarOutlined />, label: PAGE_TITLES.invitation },
              { key: "scan", icon: <ShoppingOutlined />, label: PAGE_TITLES.scan },
              { key: "orders", icon: <UnorderedListOutlined />, label: PAGE_TITLES.orders },
              { key: "points", icon: <StarOutlined />, label: PAGE_TITLES.points },
              { key: "wallet", icon: <WalletOutlined />, label: PAGE_TITLES.wallet },
            ]}
            onClick={(info) => goTo(info.key as PageKey)}
          />
        </Sider>
        <Layout>
          <Header style={{ background: "#fff", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <span data-testid="customer-phone">已登录：{session.maskedPhone}</span>
            <a className="customer-logout-link" onClick={onLogout} data-testid="customer-logout">
              退出登录
            </a>
          </Header>
          <Content id="main-content" tabIndex={-1} style={{ padding: 16 }}>
            {/* 每页 h1 语义地标（此前全站无 h1）；标题与侧栏菜单同源。 */}
            <h1 className="customer-page-title">{PAGE_TITLES[page]}</h1>
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

  return (
    <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: "#0958d9", colorPrimaryHover: "#1c64d4", colorPrimaryActive: "#0a4fb8" } }}>
      {body}
    </ConfigProvider>
  );
}
