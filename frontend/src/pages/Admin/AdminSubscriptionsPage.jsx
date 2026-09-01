import React, { useState, useEffect } from 'react';
import { PackageCheck } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminSubscriptionsPage = () => {
  const [subscriptions, setSubscriptions] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadSubscriptions();
  }, [page]);

  const loadSubscriptions = async () => {
    try {
      setLoading(true);
      const res = await adminService.getSubscriptions(page, 20);
      if (res.success && res.data) {
        setSubscriptions(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      }
    } catch (e) {
      console.error('Failed to load subscriptions', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
          <PackageCheck className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Platform Subscriptions Registry</h1>
          <p className="text-xs text-slate-400">All customer subscription lifecycles, plans, and renewal states</p>
        </div>
      </div>

      {/* Subscriptions Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
        {subscriptions.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Subscription ID</th>
                  <th className="pb-3">Customer User ID</th>
                  <th className="pb-3">Plan Tier</th>
                  <th className="pb-3">Period Start</th>
                  <th className="pb-3">Period End</th>
                  <th className="pb-3">Provider</th>
                  <th className="pb-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {subscriptions.map((sub) => (
                  <tr key={sub.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3.5 font-mono text-slate-300">{sub.id}</td>
                    <td className="py-3.5 font-mono text-slate-400">{sub.userId}</td>
                    <td className="py-3.5 font-bold text-slate-200">
                      {sub.plan?.name || 'SaaS Plan'}
                    </td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(sub.currentPeriodStart).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(sub.currentPeriodEnd).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 font-mono text-slate-400">{sub.paymentProvider || 'SANDBOX'}</td>
                    <td className="py-3.5">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                          sub.status === 'ACTIVE'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : 'bg-slate-800 text-slate-400 border-slate-700'
                        }`}
                      >
                        {sub.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No subscriptions found.</p>
        )}
      </div>
    </div>
  );
};

export default AdminSubscriptionsPage;
