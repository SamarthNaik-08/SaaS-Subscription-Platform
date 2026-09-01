import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { Sparkles, Zap, Activity, CreditCard, Receipt, ArrowRight, ShieldCheck, Clock, Send } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { usageService } from '../../services/usageService';
import { billingService } from '../../services/billingService';
import { notificationService } from '../../services/notificationService';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [subscription, setSubscription] = useState(null);
  const [usage, setUsage] = useState(null);
  const [invoices, setInvoices] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [quickPrompt, setQuickPrompt] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      const [subRes, usageRes, invRes, notifRes] = await Promise.allSettled([
        billingService.getCurrentSubscription(),
        usageService.getCurrentUsage(),
        billingService.getInvoices(),
        notificationService.getNotifications(),
      ]);

      if (subRes.status === 'fulfilled' && subRes.value.success) {
        setSubscription(subRes.value.data);
      }
      if (usageRes.status === 'fulfilled') {
        setUsage(usageRes.value);
      }
      if (invRes.status === 'fulfilled' && invRes.value.success) {
        setInvoices(invRes.value.data.slice(0, 3));
      }
      if (notifRes.status === 'fulfilled' && notifRes.value.success) {
        setNotifications(notifRes.value.data.slice(0, 3));
      }
    } catch (e) {
      console.error('Failed to load dashboard data', e);
    } finally {
      setLoading(false);
    }
  };

  const handleQuickInference = (e) => {
    e.preventDefault();
    if (!quickPrompt.trim()) return;
    navigate('/studio', { state: { initialPrompt: quickPrompt } });
  };

  const aiMetric = usage?.metrics?.AI_REQUEST;
  const plan = subscription?.plan;
  const isFree = plan?.code === 'FREE' || !plan;

  return (
    <div className="space-y-8">
      {/* Welcome Hero Banner */}
      <div className="relative p-8 rounded-3xl bg-gradient-to-r from-slate-900 via-indigo-950/60 to-purple-950/40 border border-slate-800 shadow-2xl overflow-hidden">
        <div className="absolute top-0 right-0 -mt-8 -mr-8 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />
        
        <div className="relative z-10 flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="flex items-center space-x-2">
              <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-indigo-500/15 text-indigo-300 border border-indigo-500/30">
                Direct Consumer AI Platform
              </span>
              <span className="text-xs text-slate-400">• Active Session</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Welcome back, {user?.firstName || 'Creator'}!
            </h1>
            <p className="text-sm text-slate-300 max-w-xl leading-relaxed">
              Your account is configured with real-time quota metering and direct access to state-of-the-art AI models.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3">
            <Link
              to="/studio"
              className="flex items-center space-x-2 px-5 py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white shadow-lg shadow-indigo-500/25 transition-all hover:scale-105"
            >
              <Sparkles className="w-4 h-4" />
              <span>Open AI Studio</span>
            </Link>

            {isFree && (
              <Link
                to="/subscription"
                className="flex items-center space-x-2 px-5 py-2.5 rounded-xl font-bold text-xs bg-slate-900/90 hover:bg-slate-800 text-indigo-300 border border-indigo-500/30 shadow-sm transition-all"
              >
                <Zap className="w-4 h-4 text-amber-400" />
                <span>Upgrade Plan</span>
              </Link>
            )}
          </div>
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Active Plan Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Current Tier</span>
            <div className="w-8 h-8 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <CreditCard className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="flex items-baseline space-x-2">
              <h2 className="text-2xl font-black text-white">{plan?.name || 'Free Tier'}</h2>
              <span className="text-xs text-emerald-400 font-semibold">Active</span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              {isFree ? '50 AI requests / month' : `${plan?.monthlyAiLimit} requests / month`}
            </p>
          </div>
          <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
            <Link to="/subscription" className="text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-1">
              <span>Manage Plan</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
            {subscription?.cancelAtPeriodEnd && (
              <span className="text-amber-400 font-medium">Cancels at period end</span>
            )}
          </div>
        </div>

        {/* AI Requests Meter */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Monthly AI Requests</span>
            <div className="w-8 h-8 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="flex items-baseline space-x-2">
              <span className="text-2xl font-black text-white">{aiMetric?.used ?? 0}</span>
              <span className="text-xs text-slate-400 font-mono">/ {aiMetric?.limit ?? 50}</span>
            </div>
            <div className="w-full h-2 bg-slate-800 rounded-full mt-3 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  (aiMetric?.percentage ?? 0) >= 90
                    ? 'bg-rose-500'
                    : (aiMetric?.percentage ?? 0) >= 75
                    ? 'bg-amber-500'
                    : 'bg-gradient-to-r from-indigo-500 to-purple-500'
                }`}
                style={{ width: `${Math.min(100, aiMetric?.percentage ?? 0)}%` }}
              />
            </div>
          </div>
          <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
            <span className="text-slate-400">{aiMetric?.remaining ?? 50} requests remaining</span>
            <Link to="/usage" className="text-indigo-400 hover:text-indigo-300 font-semibold">
              View Analytics
            </Link>
          </div>
        </div>

        {/* Account Security Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4 hover:border-slate-700 transition-colors">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Account Security</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <ShieldCheck className="w-4 h-4" />
            </div>
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-lg font-bold text-white">Protected</span>
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                JWT Auth
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">Multi-session token revocation enabled</p>
          </div>
          <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs">
            <Link to="/settings" className="text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-1">
              <span>Security Settings</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>
        </div>
      </div>

      {/* Fast Prompt Launcher */}
      <div className="p-6 rounded-2xl bg-slate-900/40 border border-slate-800 space-y-3">
        <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-indigo-400" />
          Quick AI Generation Launchpad
        </h3>
        <form onSubmit={handleQuickInference} className="flex gap-3">
          <input
            type="text"
            value={quickPrompt}
            onChange={(e) => setQuickPrompt(e.target.value)}
            placeholder="Type any prompt to jump straight into AI Studio (e.g. Generate a Python script to parse logs)..."
            className="flex-1 px-4 py-3 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-indigo-500 transition-colors"
          />
          <button
            type="submit"
            className="px-5 py-3 rounded-xl font-bold text-xs bg-indigo-600 hover:bg-indigo-500 text-white shadow-md transition-all flex items-center space-x-2 shrink-0"
          >
            <span>Launch Studio</span>
            <Send className="w-3.5 h-3.5" />
          </button>
        </form>
      </div>

      {/* Two Column Layout: Recent Invoices & Recent Notifications */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Invoices */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <Receipt className="w-4 h-4 text-indigo-400" />
              Recent Invoices
            </h3>
            <Link to="/invoices" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300">
              View All
            </Link>
          </div>

          {invoices.length > 0 ? (
            <div className="space-y-3">
              {invoices.map((inv) => (
                <div
                  key={inv.id}
                  className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800/80 flex items-center justify-between"
                >
                  <div>
                    <p className="text-xs font-bold text-slate-200">{inv.invoiceNumber}</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">{inv.planName}</p>
                  </div>
                  <div className="text-right">
                    <p className="text-xs font-bold text-slate-200">
                      ₹{inv.totalAmount}
                    </p>
                    <span className="inline-block mt-0.5 px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      PAID
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-4 text-center">No paid invoices recorded yet.</p>
          )}
        </div>

        {/* Recent Notifications */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-400" />
              Notifications & Alerts
            </h3>
            <Link to="/notifications" className="text-xs font-semibold text-indigo-400 hover:text-indigo-300">
              View Inbox
            </Link>
          </div>

          {notifications.length > 0 ? (
            <div className="space-y-3">
              {notifications.map((notif) => (
                <div
                  key={notif.id}
                  className={`p-3.5 rounded-xl border transition-colors ${
                    notif.read
                      ? 'bg-slate-950/40 border-slate-800/60 text-slate-400'
                      : 'bg-slate-950/80 border-indigo-500/30 text-slate-200'
                  }`}
                >
                  <p className="text-xs font-bold">{notif.title}</p>
                  <p className="text-[11px] text-slate-400 mt-0.5 line-clamp-1">{notif.message}</p>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-4 text-center">No unread notifications.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default DashboardPage;
