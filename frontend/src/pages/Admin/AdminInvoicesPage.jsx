import React, { useState, useEffect } from 'react';
import { Receipt } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminInvoicesPage = () => {
  const [invoices, setInvoices] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadInvoices();
  }, [page]);

  const loadInvoices = async () => {
    try {
      setLoading(true);
      const res = await adminService.getInvoices(page, 20);
      if (res.success && res.data) {
        setInvoices(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      }
    } catch (e) {
      console.error('Failed to load invoices', e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
          <Receipt className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Platform Invoices Master Register</h1>
          <p className="text-xs text-slate-400">GST-compliant tax invoices generated across all accounts</p>
        </div>
      </div>

      {/* Invoices Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
        {invoices.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3">Invoice Number</th>
                  <th className="pb-3">Customer Email</th>
                  <th className="pb-3">Date</th>
                  <th className="pb-3">Subtotal</th>
                  <th className="pb-3">GST Tax</th>
                  <th className="pb-3">Total Amount</th>
                  <th className="pb-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {invoices.map((inv) => (
                  <tr key={inv.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3.5 font-mono font-bold text-slate-200">{inv.invoiceNumber}</td>
                    <td className="py-3.5 text-slate-400 font-mono">{inv.customerEmail}</td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(inv.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-3.5 font-mono text-slate-400">₹{inv.subtotal}</td>
                    <td className="py-3.5 font-mono text-slate-400">₹{inv.taxAmount}</td>
                    <td className="py-3.5 font-mono font-bold text-white">₹{inv.totalAmount}</td>
                    <td className="py-3.5">
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        {inv.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No invoices recorded.</p>
        )}
      </div>
    </div>
  );
};

export default AdminInvoicesPage;
