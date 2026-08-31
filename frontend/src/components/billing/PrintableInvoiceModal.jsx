import React from 'react';
import { Printer, X, Download, ShieldCheck, CheckCircle2 } from 'lucide-react';

export const PrintableInvoiceModal = ({ invoice, isOpen, onClose }) => {
  if (!isOpen || !invoice) return null;

  const handlePrint = () => {
    window.print();
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm print:p-0 print:bg-white">
      <div className="bg-slate-900 border border-slate-800 w-full max-w-2xl rounded-2xl p-6 sm:p-8 text-white space-y-6 max-h-[90vh] overflow-y-auto print:max-h-none print:border-none print:shadow-none print:text-black print:bg-white">
        {/* Actions (Hidden on Print) */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-4 print:hidden">
          <div className="flex items-center space-x-2">
            <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              Tax Invoice
            </span>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={handlePrint}
              className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-semibold transition"
            >
              <Printer className="w-3.5 h-3.5" />
              <span>Print / Save PDF</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Invoice Header */}
        <div className="flex justify-between items-start pt-2">
          <div>
            <h2 className="text-xl font-bold text-white print:text-black">TAX INVOICE</h2>
            <p className="font-mono text-xs text-indigo-400 print:text-slate-700 font-semibold mt-1">
              Invoice #{invoice.invoiceNumber}
            </p>
            <p className="text-xs text-slate-400 print:text-slate-500 mt-1">
              Issue Date: {formatDate(invoice.createdAt)}
            </p>
          </div>
          <div className="text-right text-xs">
            <h3 className="font-bold text-white print:text-black text-sm">Nexus AI SaaS Platform</h3>
            <p className="text-slate-400 print:text-slate-600">GSTIN: 29AABCS1429B1ZB</p>
            <p className="text-slate-400 print:text-slate-600">Bangalore, Karnataka, India</p>
          </div>
        </div>

        {/* Bill To */}
        <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 text-xs grid grid-cols-2 gap-4 print:bg-slate-50 print:border-slate-200">
          <div>
            <p className="text-slate-400 print:text-slate-500 font-semibold uppercase text-[10px]">Billed To</p>
            <p className="font-bold text-white text-sm print:text-black mt-0.5">{invoice.customerName || 'Customer'}</p>
            <p className="text-slate-400 print:text-slate-600">{invoice.customerEmail || ''}</p>
          </div>
          <div className="text-right">
            <p className="text-slate-400 print:text-slate-500 font-semibold uppercase text-[10px]">Payment Status</p>
            <span className="inline-flex items-center space-x-1 px-2.5 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-bold mt-1">
              <CheckCircle2 className="w-3 h-3" />
              <span>{invoice.status}</span>
            </span>
          </div>
        </div>

        {/* Invoice Items Table */}
        <div className="border border-slate-800 rounded-xl overflow-hidden print:border-slate-300">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-950 text-slate-400 print:bg-slate-100 print:text-slate-700">
              <tr className="border-b border-slate-800 print:border-slate-300">
                <th className="p-3">Description</th>
                <th className="p-3 text-center">Qty</th>
                <th className="p-3 text-right">Unit Price</th>
                <th className="p-3 text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 print:divide-slate-200 text-slate-300 print:text-slate-800">
              {invoice.items?.map((item) => (
                <tr key={item.id}>
                  <td className="p-3 font-medium">{item.description}</td>
                  <td className="p-3 text-center">{item.quantity}</td>
                  <td className="p-3 text-right font-mono">₹{item.unitPrice?.toFixed(2)}</td>
                  <td className="p-3 text-right font-mono font-semibold">₹{item.amount?.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Totals */}
        <div className="flex justify-end pt-2 text-xs">
          <div className="w-64 space-y-2 text-right">
            <div className="flex justify-between text-slate-400 print:text-slate-600">
              <span>Subtotal:</span>
              <span className="font-mono text-white print:text-black">₹{invoice.subtotal?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-400 print:text-slate-600">
              <span>Integrated GST (18%):</span>
              <span className="font-mono text-white print:text-black">₹{invoice.taxAmount?.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm font-bold border-t border-slate-800 print:border-slate-300 pt-2 text-white print:text-black">
              <span>Total Amount:</span>
              <span className="font-mono text-indigo-400 print:text-black">₹{invoice.totalAmount?.toFixed(2)}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PrintableInvoiceModal;
