import {
  FileText,
  LayoutDashboard,
  ScrollText,
  Settings,
  ShieldAlert,
  type LucideIcon,
} from "lucide-react";

export type NavItem = {
  href: string;
  label: string;
  icon: LucideIcon;
};

export const NAV_ITEMS: NavItem[] = [
  { href: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { href: "/dashboard/claims", label: "Claims", icon: FileText },
  { href: "/dashboard/review", label: "Review queue", icon: ShieldAlert },
  { href: "/dashboard/audit", label: "Audit trail", icon: ScrollText },
  { href: "/dashboard/settings", label: "Settings", icon: Settings },
];

export function isActive(pathname: string, href: string): boolean {
  if (href === "/dashboard") {
    return pathname === href;
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function activeItem(pathname: string): NavItem | undefined {
  return [...NAV_ITEMS]
    .sort((a, b) => b.href.length - a.href.length)
    .find((item) => isActive(pathname, item.href));
}
