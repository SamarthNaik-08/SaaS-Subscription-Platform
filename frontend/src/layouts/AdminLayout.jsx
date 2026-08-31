import React, { useState, useEffect } from 'react';
import { Outlet, NavLink, Link } from 'react-router-dom';
import {
  ShieldAlert,
  LayoutDashboard,
  TrendingUp,
  Users,
  PackageCheck,
  Layers,
  CreditCard,
  Receipt,
  ShieldCheck,
  Cpu,
  ArrowLeft,
  Menu,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { adminService } from '../services/adminService';

export const AdminLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [healthStatus, setHealthStatus] = useState('UP');
  const { user } = useAuth();

  useEffect(() => {
    adminService.getHealth()
      .then((res) => {
        if (res.success && res.data) {
          setHealthStatus(res.data.status);
        }
      })
      .catch(() => setHealthStatus('DEGRADED'));
  }, []);

  const adminNavItems = [
    { name: 'Operator Overview', path: '/admin/dashboard', icon: LayoutDashboard },
    { name: 'Financial & Metrics', path: '/admin/analytics', icon: TrendingUp },
    { name: 'User Management', path: '/admin/users', icon: Users },
    { name: 'Subscriptions', path: '/admin/subscriptions', icon: PackageCheck },
    { name: 'Plans & Pricing', path: '/admin/plans', icon: Layers },
    { name: 'Payment Orders', path: '/admin/payments', icon: CreditCard },
    { name: 'Tax Invoices', path: '/admin/invoices', icon: Receipt },
    { name: 'Security Audit Logs', path: '/admin/audit-logs', icon: ShieldCheck },
    { name: 'System Health', path: '/admin/health', icon: Cpu },
  ];

  return (
    <div className="min-h-screen bg-[#07090e] text-slate-100 flex selection:bg-rose-500/30 selection:text-rose-200">
      {/* Mobile overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Admin Sidebar */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-72 bg-[#090d16] border-r border-rose-900/30 flex flex-col transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand */}
        <div className="p-6 border-b border-rose-900/30 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-rose-600 via-red-500 to-orange-500 flex items-center justify-center shadow-lg shadow-rose-500/25">
              <ShieldAlert className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-bold text-base bg-gradient-to-r from-white via-rose-200 to-rose-400 bg-clip-text text-transparent">
                Admin Console
              </h1>
              <p className="text-[11px] text-rose-400 font-semibold tracking-wider uppercase">
                Platform Operator
              </p>
            </div>
          </div>
        </div>

        {/* Operational Health Badge */}
        <div className="px-4 py-3 mx-4 mt-4 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <span
              className={`w-2.5 h-2.5 rounded-full ${
                healthStatus === 'UP' ? 'bg-emerald-400 animate-pulse' : 'bg-rose-500'
              }`}
            />
            <span className="text-xs text-slate-300 font-medium">Cluster Status:</span>
          </div>
          <span
            className={`text-xs font-bold px-2 py-0.5 rounded border ${
              healthStatus === 'UP'
                ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                : 'bg-rose-500/10 text-rose-400 border-rose-500/30'
            }`}
          >
            {healthStatus}
          </span>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-4 py-5 space-y-1 overflow-y-auto">
          {adminNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  `flex items-center space-x-3 px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-rose-500/15 text-rose-300 border border-rose-500/30 shadow-sm'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                  }`
                }
              >
                <Icon className="w-4 h-4" />
                <span>{item.name}</span>
              </NavLink>
            );
          })}
        </nav>

        {/* Return to Consumer Portal */}
        <div className="p-4 border-t border-rose-900/30 bg-[#06080d]">
          <Link
            to="/dashboard"
            className="w-full flex items-center justify-center space-x-2 px-3.5 py-2.5 rounded-xl text-xs font-bold text-indigo-400 bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30 transition-all shadow-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Return to Consumer App</span>
          </Link>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 lg:pl-72">
        {/* Top Header */}
        <header className="sticky top-0 z-30 h-16 bg-[#07090e]/80 backdrop-blur-xl border-b border-rose-900/30 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-slate-800"
            >
              <Menu className="w-5 h-5" />
            </button>
            <div className="flex items-center space-x-2 text-xs">
              <span className="px-2.5 py-1 rounded-md bg-rose-500/15 text-rose-400 border border-rose-500/30 font-mono font-semibold">
                ADMIN PRIVILEGES
              </span>
              <span className="hidden sm:inline text-slate-400 font-mono">
                Operator: {user?.email}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <Link
              to="/studio"
              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 transition-all"
            >
              <Sparkles className="w-3.5 h-3.5" />
              <span>AI Studio</span>
            </Link>
          </div>
        </header>

        {/* Page Body */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
