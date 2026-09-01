import React, { useState, useEffect } from 'react';
import { CreditCard } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminPaymentsPage = () => {
  const [payments, setPayments] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPayments();
  }, [page]);

  const loadPayments = async () => {
    try {
      setLoading(true);
      const res = await adminService.getPayments(page, 20);
      if (res.success && res.data) {
        setPayments(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      }
    } catch (e) {
      console.error('Failed to load payments', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
          <CreditCard className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Platform Payment Transactions Ledger</h1>
          <p className="text-xs text-slate-400">Audit trail of all gateway payment orders and settlements</p>
        </div>
      </div>

      {/* Payments Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
        {payments.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Gateway Order ID</th>
                  <th className="pb-3">Timestamp</th>
                  <th className="pb-3">Plan Code</th>
                  <th className="pb-3">Interval</th>
                  <th className="pb-3">Amount</th>
                  <th className="pb-3">Provider</th>
                  <th className="pb-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {payments.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3.5 font-mono font-bold text-slate-200">{p.gatewayOrderId}</td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(p.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3.5 font-semibold text-slate-200">{p.planName}</td>
                    <td className="py-3.5 text-slate-400">{p.billingInterval}</td>
                    <td className="py-3.5 font-mono font-bold text-white">₹{p.amount}</td>
                    <td className="py-3.5 font-mono text-slate-400">{p.gatewayProvider}</td>
                    <td className="py-3.5">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                          p.status === 'PAID'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                        }`}
                      >
                        {p.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No payment transactions found.</p>
        )}
      </div>
    </div>
  );
};

export default AdminPaymentsPage;
