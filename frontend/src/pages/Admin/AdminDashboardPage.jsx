import React, { useState, useEffect } from 'react';
import { TrendingUp, Users, CreditCard, Sparkles, ShieldCheck, DollarSign } from 'lucide-react';
import { Link } from 'react-router-dom';
import { adminService } from '../../services/adminService';

export const AdminDashboardPage = () => {
  const [dashboard, setDashboard] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      const res = await adminService.getDashboard();
      if (res.success && res.data) {
        setDashboard(res.data);
      }
    } catch (e) {
      console.error('Failed to load admin dashboard', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-gradient-to-r from-rose-950/40 via-slate-900 to-slate-900 border border-rose-900/30 shadow-2xl flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-1.5">
          <div className="flex items-center space-x-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30 font-mono">
              OPERATOR SUITE
            </span>
            <span className="text-xs text-slate-400">• Real-Time Analytics</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
            Platform Operator Command Center
          </h1>
          <p className="text-xs text-slate-300">
            Authoritative financial telemetry, user subscriptions, and security infrastructure oversight
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/admin/analytics"
            className="flex items-center space-x-2 px-4 py-2.5 rounded-xl font-bold text-xs bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-600/25 transition-all"
          >
            <TrendingUp className="w-4 h-4" />
            <span>Deep Financials</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* MRR */}
        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold uppercase tracking-wider">Monthly Recurring (MRR)</span>
            <DollarSign className="w-4 h-4 text-emerald-400" />
          </div>
          <p className="text-2xl font-black text-white">
            ₹{dashboard?.mrr ? Number(dashboard.mrr).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-emerald-400 font-semibold">Authoritative Monthly Baseline</p>
        </div>

        {/* ARR */}
        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold uppercase tracking-wider">Annual Run Rate (ARR)</span>
            <TrendingUp className="w-4 h-4 text-indigo-400" />
          </div>
          <p className="text-2xl font-black text-white">
            ₹{dashboard?.arr ? Number(dashboard.arr).toLocaleString() : '0.00'}
          </p>
          <p className="text-[11px] text-indigo-400 font-semibold">MRR × 12 Annualized</p>
        </div>

        {/* Total Registered Users */}
        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold uppercase tracking-wider">Total Customers</span>
            <Users className="w-4 h-4 text-purple-400" />
          </div>
          <p className="text-2xl font-black text-white">{dashboard?.totalUsers ?? 0}</p>
          <p className="text-[11px] text-slate-400">
            {dashboard?.activePaidSubscriptions ?? 0} active paying tiers
          </p>
        </div>

        {/* Total AI Requests */}
        <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 space-y-2">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span className="font-semibold uppercase tracking-wider">Platform AI Inferences</span>
            <Sparkles className="w-4 h-4 text-pink-400" />
          </div>
          <p className="text-2xl font-black text-white">{dashboard?.totalAiRequests ?? 0}</p>
          <p className="text-[11px] text-pink-400 font-semibold">
            {dashboard?.paymentSuccessRate ?? 100}% payment capture rate
          </p>
        </div>
      </div>

      {/* Two Column Section: Recent Customers & Recent Payments */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Customers */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <Users className="w-4 h-4 text-rose-400" />
              Recent Customer Signups
            </h2>
            <Link to="/admin/users" className="text-xs font-semibold text-rose-400 hover:text-rose-300">
              Manage Users
            </Link>
          </div>

          {dashboard?.recentUsers?.length > 0 ? (
            <div className="space-y-3">
              {dashboard.recentUsers.map((u) => (
                <div
                  key={u.id}
                  className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 flex items-center justify-between text-xs"
                >
                  <div>
                    <p className="font-bold text-slate-200">{u.firstName} {u.lastName}</p>
                    <p className="text-slate-400 mt-0.5">{u.email}</p>
                  </div>
                  <div className="text-right">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                      {u.globalRole || 'USER'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-6 text-center">No signups found.</p>
          )}
        </div>

        {/* Recent Payment Orders */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <CreditCard className="w-4 h-4 text-rose-400" />
              Recent Payment Transactions
            </h2>
            <Link to="/admin/payments" className="text-xs font-semibold text-rose-400 hover:text-rose-300">
              View All Orders
            </Link>
          </div>

          {dashboard?.recentPayments?.length > 0 ? (
            <div className="space-y-3">
              {dashboard.recentPayments.map((p) => (
                <div
                  key={p.id}
                  className="p-3.5 rounded-xl bg-slate-950/70 border border-slate-800 flex items-center justify-between text-xs"
                >
                  <div>
                    <p className="font-bold text-slate-200">{p.planName} ({p.billingInterval})</p>
                    <p className="text-slate-400 font-mono text-[11px] mt-0.5">{p.gatewayOrderId}</p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold font-mono text-white">₹{p.amount}</p>
                    <span
                      className={`inline-block mt-0.5 px-2 py-0.5 rounded text-[10px] font-bold border ${
                        p.status === 'PAID'
                          ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                          : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                      }`}
                    >
                      {p.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-6 text-center">No payment transactions recorded.</p>
          )}
        </div>
      </div>

      {/* Security Audit Feed */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
          <h2 className="text-sm font-bold text-white flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-rose-400" />
            Security & Lifecycle Audit Trail
          </h2>
          <Link to="/admin/audit-logs" className="text-xs font-semibold text-rose-400 hover:text-rose-300">
            View Complete Audit Log
          </Link>
        </div>

        {dashboard?.recentAuditLogs?.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Timestamp</th>
                  <th className="pb-3">Action</th>
                  <th className="pb-3">Actor / User</th>
                  <th className="pb-3">Target</th>
                  <th className="pb-3">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {dashboard.recentAuditLogs.slice(0, 8).map((log) => (
                  <tr key={log.id} className="hover:bg-slate-950/40">
                    <td className="py-2.5 font-mono text-slate-400">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td className="py-2.5">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-2.5 text-slate-300">{log.userEmail || 'SYSTEM'}</td>
                    <td className="py-2.5 text-slate-400 font-mono">{log.targetEntity || '—'}</td>
                    <td className="py-2.5 text-slate-400 max-w-sm truncate">{log.details || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-6 text-center">No audit logs recorded yet.</p>
        )}
      </div>
    </div>
  );
};

export default AdminDashboardPage;
