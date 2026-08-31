import React, { useState, useEffect } from 'react';
import {
  TrendingUp,
  DollarSign,
  Users,
  Sparkles,
  PieChart,
  Percent,
  Layers,
  ArrowUpRight,
} from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminAnalyticsPage = () => {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, []);

  const loadAnalytics = async () => {
    try {
      const res = await adminService.getAnalytics();
      if (res.success && res.data) {
        setAnalytics(res.data);
      }
    } catch (e) {
      console.error('Failed to load analytics', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
          <TrendingUp className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Authoritative SaaS Financials & Metrics</h1>
          <p className="text-xs text-slate-400">
            Calculated server-side from active subscriptions and settled payment orders
          </p>
        </div>
      </div>

      {/* Primary Financial Metric Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* MRR */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Monthly Recurring (MRR)</span>
          <p className="text-3xl font-black text-white">
            ₹{analytics?.mrr ? Number(analytics.mrr).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-emerald-400 font-semibold">Active monthly + yearly / 12</p>
        </div>

        {/* ARR */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Annual Run Rate (ARR)</span>
          <p className="text-3xl font-black text-white">
            ₹{analytics?.arr ? Number(analytics.arr).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-indigo-400 font-semibold">MRR × 12 Annualized</p>
        </div>

        {/* Cumulative Revenue */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Total Settled Revenue</span>
          <p className="text-3xl font-black text-white">
            ₹{analytics?.totalRevenue ? Number(analytics.totalRevenue).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-slate-400">Sum of all PAID payment orders</p>
        </div>

        {/* ARPPU */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Average Revenue / User (ARPPU)</span>
          <p className="text-3xl font-black text-white">
            ₹{analytics?.arppu ? Number(analytics.arppu).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-purple-400 font-semibold">MRR / Active Paid Subscribers</p>
        </div>
      </div>

      {/* Secondary Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-5">
        {/* Conversion Rate */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Conversion Rate</span>
          <p className="text-2xl font-black text-white">{analytics?.conversionRate ?? 0}%</p>
          <p className="text-[11px] text-slate-400">
            {analytics?.activePaidSubscribers ?? 0} paid / {analytics?.totalUsers ?? 0} total users
          </p>
        </div>

        {/* Churn Rate */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Churn Rate</span>
          <p className="text-2xl font-black text-white">{analytics?.churnRate ?? 0}%</p>
          <p className="text-[11px] text-slate-400">Subscriptions pending cancellation</p>
        </div>

        {/* Payment Capture Rate */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs font-semibold uppercase text-slate-400">Payment Success Rate</span>
          <p className="text-2xl font-black text-white">{analytics?.paymentSuccessRate ?? 100}%</p>
          <p className="text-[11px] text-slate-400">
            {analytics?.paymentSuccessCount ?? 0} succeeded, {analytics?.paymentFailureCount ?? 0} failed
          </p>
        </div>
      </div>

      {/* Tier Distribution Breakdown */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800 pb-3">
          <Layers className="w-4 h-4 text-rose-400" />
          Active Customer Tiers Breakdown
        </h2>

        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-4 rounded-xl bg-slate-950/70 border border-slate-800 space-y-1">
            <span className="text-xs text-slate-400 font-semibold">Free Tier</span>
            <p className="text-xl font-bold text-white">{analytics?.freeUsers ?? 0} users</p>
            <p className="text-[11px] text-slate-500">50 requests / month</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950/70 border border-indigo-500/30 space-y-1">
            <span className="text-xs text-indigo-400 font-semibold">Pro Tier</span>
            <p className="text-xl font-bold text-white">{analytics?.proUsers ?? 0} subscribers</p>
            <p className="text-[11px] text-indigo-400/80">₹499 / mo or ₹4,990 / yr</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-950/70 border border-purple-500/30 space-y-1">
            <span className="text-xs text-purple-400 font-semibold">Business Tier</span>
            <p className="text-xl font-bold text-white">{analytics?.businessUsers ?? 0} subscribers</p>
            <p className="text-[11px] text-purple-400/80">₹1,499 / mo or ₹14,990 / yr</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminAnalyticsPage;
