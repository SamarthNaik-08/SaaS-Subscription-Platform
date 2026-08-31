import React, { useState, useEffect } from 'react';
import {
  Users,
  Search,
  Eye,
  Shield,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  X,
  CreditCard,
  Receipt,
  Activity,
} from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminUsersPage = () => {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [selectedUserDetail, setSelectedUserDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [updatingStatus, setUpdatingStatus] = useState(false);

  useEffect(() => {
    loadUsers();
  }, [page, search]);

  const loadUsers = async () => {
    try {
      setLoading(true);
      const res = await adminService.getUsers(search, page, 20);
      if (res.success && res.data) {
        setUsers(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      }
    } catch (e) {
      console.error('Failed to load users:', e);
    } finally {
      setLoading(false);
    }
  };

  const handleViewUser = async (userId) => {
    try {
      const res = await adminService.getUserDetail(userId);
      if (res.success && res.data) {
        setSelectedUserDetail(res.data);
      }
    } catch (e) {
      console.error('Failed to load user detail', e);
    }
  };

  const handleStatusChange = async (userId, newStatus) => {
    if (!window.confirm(`Are you sure you want to change user status to ${newStatus}?`)) return;
    setUpdatingStatus(true);
    try {
      const res = await adminService.updateUserStatus(userId, newStatus);
      if (res.success) {
        setUsers((prev) =>
          prev.map((u) => (u.id === userId ? { ...u, status: newStatus } : u))
        );
        if (selectedUserDetail?.user?.id === userId) {
          setSelectedUserDetail((prev) => ({
            ...prev,
            user: { ...prev.user, status: newStatus },
          }));
        }
      }
    } catch (e) {
      console.error('Failed to update status', e);
    } finally {
      setUpdatingStatus(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <Users className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Platform Customers Directory</h1>
            <p className="text-xs text-slate-400">Manage user accounts, subscriptions, and status controls</p>
          </div>
        </div>

        {/* Search Bar */}
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 text-slate-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            placeholder="Search by name or email..."
            className="w-full pl-10 pr-4 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:border-rose-500 transition-colors"
          />
        </div>
      </div>

      {/* Users Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        {users.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Customer Name</th>
                  <th className="pb-3">Email Address</th>
                  <th className="pb-3">Platform Role</th>
                  <th className="pb-3">Account Status</th>
                  <th className="pb-3">Created Date</th>
                  <th className="pb-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3.5 font-bold text-slate-200">
                      {u.firstName} {u.lastName}
                    </td>
                    <td className="py-3.5 font-mono text-slate-400">{u.email}</td>
                    <td className="py-3.5">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-mono">
                        {u.globalRole || 'USER'}
                      </span>
                    </td>
                    <td className="py-3.5">
                      <select
                        value={u.status || 'ACTIVE'}
                        onChange={(e) => handleStatusChange(u.id, e.target.value)}
                        disabled={updatingStatus}
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border bg-slate-950 focus:outline-none ${
                          u.status === 'ACTIVE'
                            ? 'text-emerald-400 border-emerald-500/30'
                            : u.status === 'SUSPENDED'
                            ? 'text-amber-400 border-amber-500/30'
                            : 'text-rose-400 border-rose-500/30'
                        }`}
                      >
                        <option value="ACTIVE">ACTIVE</option>
                        <option value="SUSPENDED">SUSPENDED</option>
                        <option value="DEACTIVATED">DEACTIVATED</option>
                      </select>
                    </td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 text-right">
                      <button
                        onClick={() => handleViewUser(u.id)}
                        className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-colors inline-flex items-center gap-1"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Inspect</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No users matched query.</p>
        )}
      </div>

      {/* User Detail Drawer / Modal */}
      {selectedUserDetail && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-3xl rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6 relative max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setSelectedUserDetail(null)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white bg-slate-950/60 hover:bg-slate-800 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            <div>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30 font-mono">
                CUSTOMER INSPECTION
              </span>
              <h2 className="text-xl font-bold text-white mt-1">
                {selectedUserDetail.user?.firstName} {selectedUserDetail.user?.lastName}
              </h2>
              <p className="text-xs text-slate-400 font-mono">{selectedUserDetail.user?.email}</p>
            </div>

            {/* Subscription & Usage Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
                <p className="font-semibold text-slate-400 flex items-center gap-1.5">
                  <CreditCard className="w-3.5 h-3.5 text-indigo-400" />
                  Active Subscription
                </p>
                <p className="text-sm font-bold text-white">
                  {selectedUserDetail.subscription?.plan?.name || 'Free Plan'}
                </p>
                <p className="text-slate-400">
                  Status: <span className="text-emerald-400 font-bold">{selectedUserDetail.subscription?.status || 'ACTIVE'}</span>
                </p>
              </div>

              <div className="p-4 rounded-2xl bg-slate-950 border border-slate-800 space-y-2 text-xs">
                <p className="font-semibold text-slate-400 flex items-center gap-1.5">
                  <Activity className="w-3.5 h-3.5 text-purple-400" />
                  Current AI Usage
                </p>
                <p className="text-sm font-bold text-white">
                  {selectedUserDetail.currentUsage?.AI_REQUEST?.used ?? 0} requests consumed
                </p>
                <p className="text-slate-400">
                  Remaining: {selectedUserDetail.currentUsage?.AI_REQUEST?.remaining ?? 50} requests
                </p>
              </div>
            </div>

            {/* Invoices List */}
            <div className="space-y-2 text-xs">
              <h3 className="font-bold text-slate-200">Customer Invoices ({selectedUserDetail.invoices?.length || 0})</h3>
              {selectedUserDetail.invoices?.length > 0 ? (
                <div className="space-y-1.5">
                  {selectedUserDetail.invoices.map((inv) => (
                    <div key={inv.id} className="p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 flex justify-between">
                      <span className="font-mono text-slate-300">{inv.invoiceNumber}</span>
                      <span className="font-mono font-bold text-white">₹{inv.totalAmount}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-500">No invoices recorded.</p>
              )}
            </div>

            {/* Audit Log for User */}
            <div className="space-y-2 text-xs">
              <h3 className="font-bold text-slate-200">Recent Account Activity</h3>
              {selectedUserDetail.auditLogs?.length > 0 ? (
                <div className="space-y-1.5 max-h-40 overflow-y-auto">
                  {selectedUserDetail.auditLogs.map((log) => (
                    <div key={log.id} className="p-2 rounded-lg bg-slate-950/60 border border-slate-800 flex justify-between text-[11px]">
                      <span className="font-mono text-rose-400">{log.action}</span>
                      <span className="text-slate-400">{new Date(log.createdAt).toLocaleString()}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-slate-500">No activity recorded.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminUsersPage;
