import React, { useState, useEffect } from 'react';
import {
  Activity,
  Zap,
  HardDrive,
  AlertTriangle,
  Clock,
  Sparkles,
  Calendar,
  Layers,
  ArrowUpRight,
  TrendingUp,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { usageService } from '../../services/usageService';
import { billingService } from '../../services/billingService';

export const UsagePage = () => {
  const [usage, setUsage] = useState(null);
  const [history, setHistory] = useState([]);
  const [subscription, setSubscription] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUsageData();
  }, []);

  const loadUsageData = async () => {
    try {
      const [usageData, historyData, subRes] = await Promise.allSettled([
        usageService.getCurrentUsage(),
        usageService.getUsageHistory('AI_REQUEST'),
        billingService.getCurrentSubscription(),
      ]);

      if (usageData.status === 'fulfilled') {
        setUsage(usageData.value);
      }
      if (historyData.status === 'fulfilled') {
        setHistory(historyData.value || []);
      }
      if (subRes.status === 'fulfilled' && subRes.value.success) {
        setSubscription(subRes.value.data);
      }
    } catch (e) {
      console.error('Failed to load usage data:', e);
    } finally {
      setLoading(false);
    }
  };

  const aiUsage = usage?.metrics?.AI_REQUEST;
  const storageUsage = usage?.metrics?.STORAGE;
  const plan = subscription?.plan;

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Activity className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-white">Quota & Consumption Metering</h1>
              <p className="text-xs text-slate-400">Authoritative server-side quota tracking and consumption log</p>
            </div>
          </div>
        </div>

        <Link
          to="/subscription"
          className="flex items-center space-x-2 px-5 py-2.5 rounded-xl font-bold text-xs bg-indigo-600 hover:bg-indigo-500 text-white shadow-md transition-all self-start md:self-auto"
        >
          <span>Upgrade Tier Limits</span>
          <ArrowUpRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Quota Threshold Warnings */}
      {aiUsage && aiUsage.percentage >= 75 && (
        <div
          className={`p-4 rounded-2xl border flex items-center justify-between gap-4 ${
            aiUsage.percentage >= 100
              ? 'bg-rose-500/10 border-rose-500/30 text-rose-300'
              : aiUsage.percentage >= 90
              ? 'bg-amber-500/10 border-amber-500/30 text-amber-300'
              : 'bg-indigo-500/10 border-indigo-500/30 text-indigo-300'
          }`}
        >
          <div className="flex items-center space-x-3 text-xs font-semibold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            <span>
              {aiUsage.percentage >= 100
                ? 'Monthly AI quota is completely exhausted (100%). Upgrade your plan to resume AI generation.'
                : aiUsage.percentage >= 90
                ? 'Warning: You have reached 90% of your monthly AI quota.'
                : 'Notice: 75% of monthly AI quota has been consumed.'}
            </span>
          </div>
          <Link
            to="/subscription"
            className="px-3.5 py-1.5 rounded-lg text-xs font-bold bg-white text-slate-950 shadow hover:bg-slate-100 transition-colors shrink-0"
          >
            Upgrade Now
          </Link>
        </div>
      )}

      {/* Primary Metric Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* AI Request Quota Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-xl bg-indigo-500/15 text-indigo-400 flex items-center justify-center">
                <Sparkles className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-200">Monthly AI Requests</h3>
                <p className="text-[11px] text-slate-400">Pessimistic-lock concurrency protected</p>
              </div>
            </div>
            <span
              className={`text-xs font-mono font-bold px-2.5 py-1 rounded-lg border ${
                (aiUsage?.percentage ?? 0) >= 90
                  ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                  : 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20'
              }`}
            >
              {aiUsage?.percentage ?? 0}% USED
            </span>
          </div>

          <div>
            <div className="flex items-baseline justify-between mb-2">
              <span className="text-3xl font-black text-white">{aiUsage?.used ?? 0}</span>
              <span className="text-sm font-mono text-slate-400">/ {aiUsage?.limit ?? 50} requests</span>
            </div>

            <div className="w-full h-3 bg-slate-950 rounded-full overflow-hidden p-0.5 border border-slate-800">
              <div
                className={`h-full rounded-full transition-all duration-700 ${
                  (aiUsage?.percentage ?? 0) >= 90
                    ? 'bg-rose-500'
                    : (aiUsage?.percentage ?? 0) >= 75
                    ? 'bg-amber-500'
                    : 'bg-gradient-to-r from-indigo-500 via-purple-500 to-pink-500'
                }`}
                style={{ width: `${Math.min(100, aiUsage?.percentage ?? 0)}%` }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800/80 text-xs">
            <div>
              <p className="text-slate-500">Remaining</p>
              <p className="text-sm font-bold text-slate-200 mt-0.5">{aiUsage?.remaining ?? 50} requests</p>
            </div>
            <div>
              <p className="text-slate-500">Reset Cycle</p>
              <p className="text-sm font-bold text-slate-200 mt-0.5">End of billing period</p>
            </div>
          </div>
        </div>

        {/* Storage Limit Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-9 h-9 rounded-xl bg-purple-500/15 text-purple-400 flex items-center justify-center">
                <HardDrive className="w-4 h-4" />
              </div>
              <div>
                <h3 className="text-sm font-bold text-slate-200">Storage Capacity</h3>
                <p className="text-[11px] text-slate-400">Context cache & prompt history</p>
              </div>
            </div>
            <span className="text-xs font-mono font-bold px-2.5 py-1 rounded-lg bg-purple-500/10 text-purple-400 border border-purple-500/20">
              {storageUsage?.percentage ?? 0}% USED
            </span>
          </div>

          <div>
            <div className="flex items-baseline justify-between mb-2">
              <span className="text-3xl font-black text-white">
                {storageUsage?.used ? Math.round(storageUsage.used / (1024 * 1024)) : 0} MB
              </span>
              <span className="text-sm font-mono text-slate-400">
                / {storageUsage?.limit ? Math.round(storageUsage.limit / (1024 * 1024)) : 100} MB
              </span>
            </div>

            <div className="w-full h-3 bg-slate-950 rounded-full overflow-hidden p-0.5 border border-slate-800">
              <div
                className="h-full rounded-full bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-700"
                style={{ width: `${Math.min(100, storageUsage?.percentage ?? 0)}%` }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-slate-800/80 text-xs">
            <div>
              <p className="text-slate-500">Active Tier</p>
              <p className="text-sm font-bold text-slate-200 mt-0.5">{plan?.name || 'Free Plan'}</p>
            </div>
            <div>
              <p className="text-slate-500">Tier Capacity</p>
              <p className="text-sm font-bold text-slate-200 mt-0.5">{plan?.storageLimitMb || 100} MB</p>
            </div>
          </div>
        </div>
      </div>

      {/* Consumption Audit Log */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <h3 className="text-sm font-bold text-slate-200 flex items-center gap-2 border-b border-slate-800/80 pb-3">
          <Clock className="w-4 h-4 text-indigo-400" />
          Recent Consumption Ledger
        </h3>

        {history.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3 font-semibold">Timestamp</th>
                  <th className="pb-3 font-semibold">Metric</th>
                  <th className="pb-3 font-semibold">Consumed Units</th>
                  <th className="pb-3 font-semibold">Metadata / Context</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {history.map((rec) => (
                  <tr key={rec.id} className="hover:bg-slate-950/40">
                    <td className="py-3 font-mono text-slate-400">
                      {new Date(rec.recordedAt || rec.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3">
                      <span className="px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 font-mono font-semibold">
                        {rec.metric}
                      </span>
                    </td>
                    <td className="py-3 font-bold text-slate-200">+{rec.quantity}</td>
                    <td className="py-3 text-slate-400 max-w-xs truncate">{rec.metadata || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-6 text-center">No usage records yet in this billing cycle.</p>
        )}
      </div>
    </div>
  );
};

export default UsagePage;
