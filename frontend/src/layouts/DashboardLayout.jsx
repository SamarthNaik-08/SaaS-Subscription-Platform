import React, { useState, useEffect } from 'react';
import { Outlet, Link, useNavigate } from 'react-router-dom';
import { Menu, Bell, Sparkles, Zap, ShieldAlert, ArrowUpRight } from 'lucide-react';
import Sidebar from '../components/sidebar/Sidebar';
import { useAuth } from '../context/AuthContext';
import { notificationService } from '../services/notificationService';
import { usageService } from '../services/usageService';
import { billingService } from '../services/billingService';

export const DashboardLayout = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [currentUsage, setCurrentUsage] = useState(null);
  const [subscription, setSubscription] = useState(null);
  const { user, isAdmin } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    loadHeaderData();
    const interval = setInterval(loadHeaderData, 30000);
    return () => clearInterval(interval);
  }, []);

  const loadHeaderData = async () => {
    try {
      const [unreadRes, usageRes, subRes] = await Promise.allSettled([
        notificationService.getUnreadCount(),
        usageService.getCurrentUsage(),
        billingService.getCurrentSubscription(),
      ]);

      if (unreadRes.status === 'fulfilled' && unreadRes.value.success) {
        setUnreadCount(unreadRes.value.data);
      }
      if (usageRes.status === 'fulfilled') {
        setCurrentUsage(usageRes.value);
      }
      if (subRes.status === 'fulfilled' && subRes.value.success) {
        setSubscription(subRes.value.data);
      }
    } catch (e) {
      console.warn('Failed to load layout header data', e);
    }
  };

  const aiUsage = currentUsage?.metrics?.AI_REQUEST;
  const isFreePlan = subscription?.plan?.code === 'FREE' || !subscription;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex selection:bg-indigo-500/30 selection:text-indigo-200">
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0 lg:pl-72">
        {/* Top Header */}
        <header className="sticky top-0 z-30 h-16 bg-slate-950/80 backdrop-blur-xl border-b border-slate-800/80 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          <div className="flex items-center space-x-3 sm:space-x-4">
            <button
              onClick={() => setSidebarOpen(true)}
              className="lg:hidden p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-slate-800"
            >
              <Menu className="w-5 h-5" />
            </button>

            {/* Plan Badge */}
            <div className="flex items-center space-x-2">
              <span className="hidden sm:inline-flex items-center px-2.5 py-1 rounded-lg text-xs font-bold tracking-wide uppercase bg-gradient-to-r from-indigo-500/20 to-purple-500/20 text-indigo-300 border border-indigo-500/30 shadow-sm">
                <Sparkles className="w-3 h-3 mr-1.5 text-indigo-400" />
                {subscription?.plan?.name || 'Free Plan'}
              </span>

              {/* Quota indicator capsule */}
              {aiUsage && (
                <div className="hidden md:flex items-center space-x-2 px-3 py-1 rounded-lg bg-slate-900/90 border border-slate-800 text-xs">
                  <span className="text-slate-400">AI Quota:</span>
                  <span className="font-semibold text-slate-200">
                    {aiUsage.remaining} / {aiUsage.limit} left
                  </span>
                  <div className="w-12 h-1.5 bg-slate-800 rounded-full overflow-hidden">
                    <div
                      className={`h-full rounded-full ${
                        aiUsage.percentage >= 90
                          ? 'bg-rose-500'
                          : aiUsage.percentage >= 75
                          ? 'bg-amber-500'
                          : 'bg-indigo-500'
                      }`}
                      style={{ width: `${Math.min(100, aiUsage.percentage)}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="flex items-center space-x-3 sm:space-x-4">
            {/* Upgrade CTA if on FREE plan */}
            {isFreePlan && (
              <button
                onClick={() => navigate('/subscription')}
                className="hidden sm:inline-flex items-center space-x-1.5 px-3.5 py-1.5 rounded-xl text-xs font-semibold bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 hover:from-indigo-500 hover:to-pink-500 text-white shadow-md shadow-indigo-500/20 transition-all hover:scale-[1.02]"
              >
                <Zap className="w-3.5 h-3.5" />
                <span>Upgrade to Pro</span>
                <ArrowUpRight className="w-3.5 h-3.5 ml-0.5 opacity-80" />
              </button>
            )}

            {/* Notification Bell */}
            <Link
              to="/notifications"
              className="relative p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-900 border border-slate-800/80 transition-colors"
              title="Notifications"
            >
              <Bell className="w-4 h-4" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center animate-pulse">
                  {unreadCount > 9 ? '9+' : unreadCount}
                </span>
              )}
            </Link>

            {/* User Profile Avatar */}
            <Link to="/settings" className="flex items-center space-x-2.5 group">
              <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-xs font-bold text-white shadow-md group-hover:ring-2 ring-indigo-500/50 transition-all">
                {user?.firstName ? user.firstName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div className="hidden xl:block text-left leading-none">
                <p className="text-xs font-semibold text-slate-200 group-hover:text-indigo-300 transition-colors">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-[10px] text-slate-400 mt-0.5 truncate max-w-[120px]">
                  {user?.email}
                </p>
              </div>
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

export default DashboardLayout;
