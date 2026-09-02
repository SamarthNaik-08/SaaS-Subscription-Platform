import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  Sparkles,
  Activity,
  CreditCard,
  Receipt,
  Bell,
  Settings,
  ShieldAlert,
  LogOut,
  Pin,
  Clock,
  MessageSquare,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const Sidebar = ({ isOpen, onClose }) => {
  const { user, isAdmin, logout } = useAuth();

  const navItems = [
    { name: 'AI Studio', path: '/studio', icon: Sparkles, badge: 'Active' },
    { name: 'Quota & Usage', path: '/usage', icon: Activity, badge: null },
    { name: 'Plan & Billing', path: '/subscription', icon: CreditCard, badge: null },
    { name: 'Invoices', path: '/invoices', icon: Receipt, badge: null },
    { name: 'Notifications', path: '/notifications', icon: Bell, badge: null },
    { name: 'Settings & Security', path: '/settings', icon: Settings, badge: null },
  ];

  const pinnedChats = [
    { id: 'p1', title: 'Java Architecture Guide', time: 'Pinned' },
    { id: 'p2', title: 'React Best Practices', time: 'Pinned' },
  ];

  const recentChats = [
    { id: 'r1', title: 'What is Java?', time: 'Just now' },
    { id: 'r2', title: 'Python ML Setup', time: '2h ago' },
    { id: 'r3', title: 'Spring Boot REST API', time: '5h ago' },
    { id: 'r4', title: 'Docker Deployment', time: 'Yesterday' },
  ];

  return (
    <>
      {/* Mobile overlay */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/70 backdrop-blur-sm lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-72 bg-slate-950/95 backdrop-blur-xl border-r border-slate-800/80 flex flex-col transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand */}
        <div className="p-6 border-b border-slate-800/80 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 via-purple-500 to-pink-500 flex items-center justify-center shadow-lg shadow-indigo-500/25">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="font-bold text-base bg-gradient-to-r from-white via-slate-200 to-indigo-300 bg-clip-text text-transparent">
                Nexus AI
              </h1>
              <p className="text-xs text-indigo-400 font-medium">Consumer SaaS Platform</p>
            </div>
          </div>
        </div>

        {/* User Account Capsule */}
        <div className="px-4 py-3 mx-4 mt-4 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between">
          <div className="flex items-center space-x-3 min-w-0">
            <div className="w-8 h-8 rounded-lg bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300 font-bold text-xs shrink-0">
              {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
            </div>
            <div className="truncate">
              <p className="text-xs font-semibold text-slate-200 truncate">
                {user?.firstName} {user?.lastName}
              </p>
              <div className="flex items-center space-x-1.5 mt-0.5">
                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                  {user?.globalRole === 'ADMIN' ? 'Platform Admin' : 'Customer'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Scrollable Content Area */}
        <div className="flex-1 overflow-y-auto">
          {/* Main Navigation */}
          <nav className="px-4 pt-5 pb-2 space-y-1.5">
            {navItems.map((item) => {
              const Icon = item.icon;
              return (
                <NavLink
                  key={item.path}
                  to={item.path}
                  onClick={onClose}
                  className={({ isActive }) =>
                    `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all ${
                      isActive
                        ? 'bg-gradient-to-r from-indigo-600/20 to-purple-600/10 text-indigo-300 border border-indigo-500/30 shadow-sm'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/60'
                    }`
                  }
                >
                  <div className="flex items-center space-x-3">
                    <Icon className="w-4 h-4" />
                    <span>{item.name}</span>
                  </div>
                  {item.badge && (
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 font-semibold border border-indigo-500/30">
                      {item.badge}
                    </span>
                  )}
                </NavLink>
              );
            })}
          </nav>

          {/* Pinned Section */}
          <div className="px-4 pt-4 pb-2">
            <div className="flex items-center space-x-2 px-2 mb-2">
              <Pin className="w-3 h-3 text-amber-400/70" />
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Pinned</span>
            </div>
            <div className="space-y-0.5">
              {pinnedChats.map((chat) => (
                <NavLink
                  key={chat.id}
                  to="/studio"
                  onClick={onClose}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-xs transition-all text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 group"
                >
                  <div className="flex items-center space-x-2.5 min-w-0">
                    <MessageSquare className="w-3.5 h-3.5 text-amber-400/50 shrink-0" />
                    <span className="truncate">{chat.title}</span>
                  </div>
                  <span className="text-[10px] text-slate-600 shrink-0 ml-2">{chat.time}</span>
                </NavLink>
              ))}
            </div>
          </div>

          {/* Recents Section */}
          <div className="px-4 pt-3 pb-4">
            <div className="flex items-center space-x-2 px-2 mb-2">
              <Clock className="w-3 h-3 text-slate-500" />
              <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Recents</span>
            </div>
            <div className="space-y-0.5">
              {recentChats.map((chat) => (
                <NavLink
                  key={chat.id}
                  to="/studio"
                  onClick={onClose}
                  className="flex items-center justify-between px-3 py-2 rounded-lg text-xs transition-all text-slate-400 hover:text-slate-200 hover:bg-slate-900/60 group"
                >
                  <div className="flex items-center space-x-2.5 min-w-0">
                    <MessageSquare className="w-3.5 h-3.5 text-slate-600 shrink-0" />
                    <span className="truncate">{chat.title}</span>
                  </div>
                  <span className="text-[10px] text-slate-600 shrink-0 ml-2">{chat.time}</span>
                </NavLink>
              ))}
            </div>
          </div>
        </div>

        {/* Admin Link (if ADMIN) */}
        {isAdmin && (
          <div className="px-4 pb-2">
            <NavLink
              to="/admin/dashboard"
              onClick={onClose}
              className="flex items-center space-x-3 px-3.5 py-2.5 rounded-xl text-sm font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-all shadow-sm"
            >
              <ShieldAlert className="w-4 h-4 text-rose-400" />
              <span>Admin Operator Portal</span>
            </NavLink>
          </div>
        )}

        {/* Sign Out Button */}
        <div className="p-4 border-t border-slate-800/80 bg-slate-950/60">
          <button
            onClick={logout}
            className="w-full flex items-center justify-center space-x-2 px-3.5 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 border border-slate-800 hover:border-rose-500/20 transition-all"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>
    </>
  );
};

export default Sidebar;
