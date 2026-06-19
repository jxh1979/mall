import {
  BankOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  ReloadOutlined,
} from "@ant-design/icons";
import { Alert, Badge, Button, Layout, Menu, Modal, Spin } from "antd";
import React from "react";
import { apiBaseUrl } from "./apiClient";
import { ResultBox } from "./ui";
import {
  defaultOpenMenuKeys,
  findNavigationGroup,
  menuItems,
  sectionMeta,
  type SectionKey,
} from "./shell";
import type { ApiUser, Snapshot } from "./types";

const { Header, Sider, Content } = Layout;

type PendingConfirmation = {
  title: string;
  content: string;
  action: () => Promise<void>;
};

type LegacyAppShellProps = {
  activeSection: SectionKey;
  authUser: ApiUser;
  children: React.ReactNode;
  error: string | null;
  lastResult: unknown;
  loading: boolean;
  onLogout: () => void;
  onRefresh: () => void;
  onSectionChange: (section: SectionKey) => void;
  openMenuKeys: string[];
  pendingConfirmation: PendingConfirmation | null;
  setOpenMenuKeys: (keys: string[]) => void;
  setPendingConfirmation: (confirmation: PendingConfirmation | null) => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  sidebarCollapsed: boolean;
  snapshot: Snapshot | null;
};

export function LegacyAppShell({
  activeSection,
  authUser,
  children,
  error,
  lastResult,
  loading,
  onLogout,
  onRefresh,
  onSectionChange,
  openMenuKeys,
  pendingConfirmation,
  setOpenMenuKeys,
  setPendingConfirmation,
  setSidebarCollapsed,
  sidebarCollapsed,
  snapshot,
}: LegacyAppShellProps) {
  const activeMeta = sectionMeta[activeSection];
  const activeGroup = findNavigationGroup(activeSection);

  function handleSidebarToggle() {
    if (window.matchMedia("(max-width: 760px)").matches) {
      setOpenMenuKeys(defaultOpenMenuKeys);
      setSidebarCollapsed(false);
      return;
    }
    const nextValue = !sidebarCollapsed;
    if (!nextValue && openMenuKeys.length === 0) setOpenMenuKeys(defaultOpenMenuKeys);
    setSidebarCollapsed(nextValue);
  }

  async function handleConfirm() {
    const action = pendingConfirmation?.action;
    setPendingConfirmation(null);
    if (action) await action();
  }

  return (
    <Layout className="app-shell" data-testid="app-shell">
      <Sider width={248} collapsedWidth={72} collapsed={sidebarCollapsed} className={`sidebar${sidebarCollapsed ? " sidebar-collapsed" : ""}`}>
        <div className="brand">
          <BankOutlined />
          {sidebarCollapsed ? null : <span>综合体管理系统</span>}
        </div>
        <Menu
          data-testid="app-nav"
          mode="inline"
          selectedKeys={[activeSection]}
          openKeys={openMenuKeys}
          onOpenChange={(keys) => {
            if (!sidebarCollapsed) setOpenMenuKeys(keys);
          }}
          onClick={({ key }) => onSectionChange(key as SectionKey)}
          items={menuItems}
        />
      </Sider>
      <Layout className="main-layout">
        <Header className="topbar">
          <div className="topbar-left">
            <Button
              data-testid="sidebar-toggle"
              className="icon-button"
              icon={sidebarCollapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
              onClick={handleSidebarToggle}
              aria-label={sidebarCollapsed ? "展开导航" : "收起导航"}
            />
            <div className="topbar-title">
              <strong>{activeMeta.title}</strong>
              <span>{activeGroup.label} · {apiBaseUrl || "同源 API"}</span>
            </div>
          </div>
          <div className="topbar-actions">
            <span className="user-chip">{authUser.username}</span>
            <Badge status={snapshot?.health.ok ? "success" : "warning"} text={snapshot?.health.ok ? "API 正常" : "等待数据"} />
            <Button data-testid="logout-button" onClick={onLogout}>退出</Button>
            <Button icon={<ReloadOutlined />} loading={loading} onClick={onRefresh}>刷新</Button>
          </div>
        </Header>
        <Content className="content">
          <Alert
            className="status-alert"
            data-testid="legacy-retired-banner"
            type="warning"
            showIcon
            message="该控制台已退役（只读）"
            description="写动作已统一迁移至管理后台/现场端执行。请前往「管理后台」完成核销、结账、库存等操作。"
            action={(
              <Button size="small" type="primary" data-testid="legacy-go-admin" onClick={() => window.location.assign("/admin")}>
                前往管理后台
              </Button>
            )}
          />
          {error ? <Alert className="status-alert" data-testid="global-error" type="error" message="后端数据读取失败" description={error} /> : null}
          {loading && !snapshot ? <div className="loading-state"><Spin size="large" /></div> : null}
          {children}
          <ResultBox value={lastResult} />
        </Content>
      </Layout>
      <Modal
        open={Boolean(pendingConfirmation)}
        title={pendingConfirmation?.title}
        okText="确认"
        cancelText="取消"
        okButtonProps={{ "data-testid": "confirm-high-impact-ok" }}
        cancelButtonProps={{ "data-testid": "confirm-high-impact-cancel" }}
        onCancel={() => setPendingConfirmation(null)}
        onOk={handleConfirm}
      >
        <p>{pendingConfirmation?.content}</p>
      </Modal>
    </Layout>
  );
}