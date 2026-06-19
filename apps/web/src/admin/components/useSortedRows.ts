import React from "react";

/**
 * 通用的客户端列表排序 hook，与 rowSearch / usePagedRows 搭配使用：
 * 调用方传入排序列的取值器映射，hook 维护当前排序列与方向，并返回已排序的
 * 行集合与列头点击处理。数字按数值比较，其它按本地化字符串比较；空值排在末尾。
 *
 * 用法：
 *   const sort = useSortedRows(rows, { amount: (r) => r.amount, date: (r) => r.createdAt });
 *   sort.sorted // 已排序行
 *   <th onClick={() => sort.toggle("amount")}>金额{sort.indicator("amount")}</th>
 */
export type SortDirection = "asc" | "desc";

type Accessor<T> = (row: T) => unknown;

function compareValues(a: unknown, b: unknown): number {
  const aEmpty = a === null || a === undefined || a === "";
  const bEmpty = b === null || b === undefined || b === "";
  if (aEmpty && bEmpty) return 0;
  if (aEmpty) return 1;
  if (bEmpty) return -1;
  const aNum = Number(a);
  const bNum = Number(b);
  if (Number.isFinite(aNum) && Number.isFinite(bNum) && typeof a !== "boolean" && typeof b !== "boolean") {
    return aNum - bNum;
  }
  return String(a).localeCompare(String(b), "zh-CN");
}

export function useSortedRows<T>(rows: T[], accessors: Record<string, Accessor<T>>) {
  const [key, setKey] = React.useState<string | null>(null);
  const [direction, setDirection] = React.useState<SortDirection>("asc");

  const sorted = React.useMemo(() => {
    if (!key || !accessors[key]) return rows;
    const accessor = accessors[key];
    const copy = [...rows];
    copy.sort((a, b) => {
      const result = compareValues(accessor(a), accessor(b));
      return direction === "asc" ? result : -result;
    });
    return copy;
  }, [rows, key, direction, accessors]);

  function toggle(nextKey: string) {
    if (key === nextKey) {
      setDirection((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setKey(nextKey);
      setDirection("asc");
    }
  }

  function indicator(forKey: string): string {
    if (key !== forKey) return " ↕";
    return direction === "asc" ? " ↑" : " ↓";
  }

  return { sorted, key, direction, toggle, indicator };
}
