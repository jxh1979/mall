package com.complex.platform.access;

// 文件版本：V0.01

/**
 * M736：角色「可查看项目」逐个勾选的候选条目。操作员 2026-10-03 所选选项原文：「项目、城市项目公司、在用的域主管公司各列为独立条目，
 * 勾哪条只管哪条，互不连带。」kind 取 project（项目）、project_company（城市项目公司）、management_company（域主管公司）。
 */
public record RoleProjectOption(String id, String name, String kind) {
}
