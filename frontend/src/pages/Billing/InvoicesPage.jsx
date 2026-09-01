import React, { useState, useEffect } from 'react';
import { Receipt, Eye, X, Printer } from 'lucide-react';
import { billingService } from '../../services/billingService';

export const InvoicesPage = () => {
  const [invoices, setInvoices] = useState([]);
  const [paymentOrders, setPaymentOrders] = useState([]);
  const [activeTab, setActiveTab] = useState('invoices'); // 'invoices' | 'orders'
  const [selectedInvoice, setSelectedInvoice] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadBillingData();
  }, []);

  const loadBillingData = async () => {
    try {
      const [invRes, orderRes] = await Promise.allSettled([
        billingService.getInvoices(),
        billingService.getPaymentOrders(),
      ]);

      if (invRes.status === 'fulfilled' && invRes.value.success) {
        setInvoices(invRes.value.data);
      }
      if (orderRes.status === 'fulfilled' && orderRes.value.success) {
        setPaymentOrders(orderRes.value.data);
      }
    } catch (e) {
      console.error('Failed to load invoices:', e);
    } finally {
      setLoading(false);
    }
  };

  const handleViewInvoice = async (invoiceId) => {
    try {
      const res = await billingService.getInvoiceById(invoiceId);
      if (res.success && res.data) {
        setSelectedInvoice(res.data);
      }
    } catch (e) {
      console.error('Failed to load invoice detail', e);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
            <Receipt className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Billing History & Invoices</h1>
            <p className="text-xs text-slate-400">Immutable GST tax invoices & payment transactions</p>
          </div>
        </div>

        {/* Tab Switcher */}
        <div className="p-1 rounded-2xl bg-slate-950 border border-slate-800 flex items-center space-x-1">
          <button
            onClick={() => setActiveTab('invoices')}
            className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition-all ${
              activeTab === 'invoices'
                ? 'bg-indigo-600 text-white shadow'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Tax Invoices ({invoices.length})
          </button>
          <button
            onClick={() => setActiveTab('orders')}
            className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition-all ${
              activeTab === 'orders'
                ? 'bg-indigo-600 text-white shadow'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            Orders History ({paymentOrders.length})
          </button>
        </div>
      </div>

      {/* Main Content Table */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800">
        {activeTab === 'invoices' ? (
          invoices.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="text-slate-400 border-b border-slate-800">
                  <tr>
                    <th className="pb-3 font-semibold">Invoice Number</th>
                    <th className="pb-3 font-semibold">Date Generated</th>
                    <th className="pb-3 font-semibold">Plan Description</th>
                    <th className="pb-3 font-semibold">Subtotal</th>
                    <th className="pb-3 font-semibold">Tax (GST 18%)</th>
                    <th className="pb-3 font-semibold">Total Amount</th>
                    <th className="pb-3 font-semibold">Status</th>
                    <th className="pb-3 font-semibold text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/60">
                  {invoices.map((inv) => (
                    <tr key={inv.id} className="hover:bg-slate-950/40 transition-colors">
                      <td className="py-3.5 font-mono font-bold text-slate-200">{inv.invoiceNumber}</td>
                      <td className="py-3.5 text-slate-400">
                        {new Date(inv.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-3.5 text-slate-300 font-medium">{inv.planName}</td>
                      <td className="py-3.5 font-mono text-slate-400">₹{inv.subtotal}</td>
                      <td className="py-3.5 font-mono text-slate-400">₹{inv.taxAmount}</td>
                      <td className="py-3.5 font-mono font-bold text-white">₹{inv.totalAmount}</td>
                      <td className="py-3.5">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          {inv.status}
                        </span>
                      </td>
                      <td className="py-3.5 text-right">
                        <button
                          onClick={() => handleViewInvoice(inv.id)}
                          className="px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 transition-colors inline-flex items-center gap-1"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>View</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="text-xs text-slate-500 py-8 text-center">No paid invoices recorded yet.</p>
          )
        ) : paymentOrders.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="text-slate-400 border-b border-slate-800">
                <tr>
                  <th className="pb-3 font-semibold">Gateway Order ID</th>
                  <th className="pb-3 font-semibold">Timestamp</th>
                  <th className="pb-3 font-semibold">Plan & Interval</th>
                  <th className="pb-3 font-semibold">Amount Charged</th>
                  <th className="pb-3 font-semibold">Provider</th>
                  <th className="pb-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60">
                {paymentOrders.map((order) => (
                  <tr key={order.id} className="hover:bg-slate-950/40 transition-colors">
                    <td className="py-3.5 font-mono text-slate-300">{order.gatewayOrderId}</td>
                    <td className="py-3.5 text-slate-400">
                      {new Date(order.createdAt).toLocaleString()}
                    </td>
                    <td className="py-3.5 text-slate-200">
                      {order.planName} ({order.billingInterval})
                    </td>
                    <td className="py-3.5 font-mono font-bold text-white">
                      ₹{order.amount}
                    </td>
                    <td className="py-3.5 font-mono text-slate-400">{order.gatewayProvider}</td>
                    <td className="py-3.5">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold border ${
                          order.status === 'PAID'
                            ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                            : order.status === 'FAILED'
                            ? 'bg-rose-500/10 text-rose-400 border-rose-500/20'
                            : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                        }`}
                      >
                        {order.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-8 text-center">No payment orders recorded yet.</p>
        )}
      </div>

      {/* Invoice Detail Modal */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-2xl rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6 relative">
            <button
              onClick={() => setSelectedInvoice(null)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white bg-slate-950/60 hover:bg-slate-800 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            {/* Invoice Header */}
            <div className="flex items-start justify-between border-b border-slate-800 pb-6">
              <div>
                <h2 className="text-xl font-extrabold text-white">TAX INVOICE</h2>
                <p className="text-xs text-indigo-400 font-mono mt-1 font-bold">
                  {selectedInvoice.invoiceNumber}
                </p>
                <p className="text-[11px] text-slate-400 mt-1">
                  Issued: {new Date(selectedInvoice.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm font-bold text-white">Nexus AI SaaS Platform</p>
                <p className="text-[11px] text-slate-400">GSTIN: 29AABCS1429B1ZB</p>
                <p className="text-[11px] text-slate-400">Bangalore, Karnataka, India</p>
              </div>
            </div>

            {/* Billed To */}
            <div className="grid grid-cols-2 gap-4 text-xs">
              <div>
                <p className="text-slate-500 font-semibold uppercase">Billed To:</p>
                <p className="font-bold text-slate-200 mt-1">{selectedInvoice.customerName}</p>
                <p className="text-slate-400">{selectedInvoice.customerEmail}</p>
              </div>
              <div className="text-right">
                <p className="text-slate-500 font-semibold uppercase">Payment Status:</p>
                <span className="inline-block mt-1 px-2.5 py-0.5 rounded text-xs font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                  PAID IN FULL
                </span>
              </div>
            </div>

            {/* Items Table */}
            <div className="border border-slate-800 rounded-xl overflow-hidden">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-950 text-slate-400">
                  <tr>
                    <th className="p-3">Description</th>
                    <th className="p-3 text-center">Qty</th>
                    <th className="p-3 text-right">Unit Price</th>
                    <th className="p-3 text-right">Amount</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800">
                  {selectedInvoice.items?.map((item) => (
                    <tr key={item.id}>
                      <td className="p-3 font-medium text-slate-200">{item.description}</td>
                      <td className="p-3 text-center text-slate-400">{item.quantity}</td>
                      <td className="p-3 text-right font-mono text-slate-300">₹{item.unitPrice}</td>
                      <td className="p-3 text-right font-mono font-bold text-slate-200">₹{item.amount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Breakdown Totals */}
            <div className="space-y-1.5 text-xs text-slate-300 max-w-xs ml-auto border-t border-slate-800 pt-3">
              <div className="flex justify-between">
                <span className="text-slate-400">Subtotal:</span>
                <span className="font-mono">₹{selectedInvoice.subtotal}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Integrated GST (18%):</span>
                <span className="font-mono">₹{selectedInvoice.taxAmount}</span>
              </div>
              <div className="flex justify-between text-sm font-bold text-white pt-2 border-t border-slate-800">
                <span>Total Amount:</span>
                <span className="font-mono text-indigo-400">₹{selectedInvoice.totalAmount}</span>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
              <button
                onClick={() => window.print()}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-white flex items-center gap-1.5 transition-colors"
              >
                <Printer className="w-3.5 h-3.5" />
                <span>Print Invoice</span>
              </button>
              <button
                onClick={() => setSelectedInvoice(null)}
                className="px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600 hover:bg-indigo-500 text-white transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InvoicesPage;
