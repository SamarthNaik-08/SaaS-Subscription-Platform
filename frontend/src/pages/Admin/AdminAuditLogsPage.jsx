import React, { useState, useEffect } from 'react';
import { ShieldCheck, Filter } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminAuditLogsPage = () => {
  const [logs, setLogs] = useState([]);
  const [actionFilter, setActionFilter] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAuditLogs();
  }, [page, actionFilter]);

  const loadAuditLogs = async () => {
    try {
      setLoading(true);
      const res = await adminService.getAuditLogs({
        action: actionFilter || undefined,
        page,
        size: 30,
      });
      if (res.success && res.data) {
        setLogs(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      }
    } catch (e) {
      console.error('Failed to load audit logs', e);
    } finally {
      setLoading(false);
    }
  };

  const actionTypes = [
    '',
    'USER_REGISTERED',
    'USER_LOGGED_IN',
    'USER_LOGGED_OUT',
    'PASSWORD_CHANGED',
    'PAYMENT_ORDER_CREATED',
    'PAYMENT_VERIFIED',
    'PAYMENT_FAILED',
    'SUBSCRIPTION_UPGRADED',
    'SUBSCRIPTION_CANCEL_SCHEDULED',
    'SUBSCRIPTION_RESUMED',
    'INVOICE_CREATED',
    'ADMIN_USER_STATUS_CHANGED',
    'ADMIN_PLAN_UPDATED',
  ];

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Security & Lifecycle Audit Log</h1>
            <p className="text-xs text-slate-400">
              Immutable audit ledger capturing all authentication, billing, and operator events
            </p>
          </div>
        </div>

        {/* Action Filter Selector */}
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-500" />
          <select
            value={actionFilter}
            onChange={(e) => {
              setActionFilter(e.target.value);
              setPage(0);
            }}
            className="px-3.5 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-semibold text-slate-200 focus:outline-none focus:border-rose-500"
          >
            <option value="">All Audit Actions</option>
            {actionTypes.filter(Boolean).map((act) => (
              <option key={act} value={act}>
                {act}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Audit Logs Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
        {logs.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Timestamp</th>
                  <th className="pb-3">Event Action</th>
                  <th className="pb-3">Actor / Customer Email</th>
                  <th className="pb-3">Target Entity</th>
                  <th className="pb-3">Event Summary</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3 font-mono text-slate-400">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono">
                        {log.action}
                      </span>
                    </td>
                    <td className="py-3 font-medium text-slate-200">{log.userEmail || 'SYSTEM'}</td>
                    <td className="py-3 font-mono text-slate-400">
                      {log.targetEntity ? `${log.targetEntity}:${log.targetId ? log.targetId.substring(0, 8) : ''}` : '—'}
                    </td>
                    <td className="py-3 text-slate-300 max-w-md truncate">{log.details}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No audit log records found.</p>
        )}
      </div>
    </div>
  );
};

export default AdminAuditLogsPage;
