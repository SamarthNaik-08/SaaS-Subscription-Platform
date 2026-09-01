import React, { useState, useEffect } from 'react';
import { Layers, Edit2, X, Save } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminPlansPage = () => {
  const [plans, setPlans] = useState([]);
  const [editingPlan, setEditingPlan] = useState(null);
  const [formData, setFormData] = useState({});
  const [loading, setLoading] = useState(true);
  const [saveLoading, setSaveLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState(null);

  useEffect(() => {
    loadPlans();
  }, []);

  const loadPlans = async () => {
    try {
      const res = await adminService.getAllPlans();
      if (res.success && res.data) {
        setPlans(res.data);
      }
    } catch (e) {
      console.error('Failed to load plans:', e);
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (plan) => {
    setEditingPlan(plan);
    setFormData({
      name: plan.name,
      description: plan.description || '',
      priceMonthly: plan.priceMonthly,
      priceYearly: plan.priceYearly,
      monthlyAiLimit: plan.monthlyAiLimit,
      storageLimitMb: plan.storageLimitMb,
      isActive: plan.isActive ?? true,
    });
    setStatusMsg(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaveLoading(true);
    try {
      const res = await adminService.updatePlan(editingPlan.id, formData);
      if (res.success && res.data) {
        setStatusMsg(`Plan ${res.data.name} updated successfully!`);
        setEditingPlan(null);
        loadPlans();
      }
    } catch (err) {
      console.error('Failed to update plan', err);
    } finally {
      setSaveLoading(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
          <Layers className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Platform Plan & Pricing Configuration</h1>
          <p className="text-xs text-slate-400">
            Configure future pricing, inference limits, and active tiers (historical invoices remain untouched)
          </p>
        </div>
      </div>

      {statusMsg && (
        <div className="p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-semibold text-center">
          {statusMsg}
        </div>
      )}

      {/* Plans Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {plans.map((plan) => (
          <div
            key={plan.id}
            className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800 flex flex-col justify-between space-y-6"
          >
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-rose-500/10 text-rose-400 border border-rose-500/20 font-mono">
                  {plan.code}
                </span>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded border ${
                    plan.isActive
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-slate-800 text-slate-400 border-slate-700'
                  }`}
                >
                  {plan.isActive ? 'ACTIVE' : 'INACTIVE'}
                </span>
              </div>

              <div>
                <h2 className="text-xl font-bold text-white">{plan.name}</h2>
                <p className="text-xs text-slate-400 mt-1">{plan.description || 'Tier definition'}</p>
              </div>

              <div className="space-y-2 pt-2 border-t border-slate-800 text-xs">
                <div className="flex justify-between">
                  <span className="text-slate-400">Monthly Price:</span>
                  <span className="font-mono font-bold text-white">₹{plan.priceMonthly}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Yearly Price:</span>
                  <span className="font-mono font-bold text-white">₹{plan.priceYearly}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Monthly AI Limit:</span>
                  <span className="font-mono font-bold text-indigo-400">
                    {plan.monthlyAiLimit} requests
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Storage Quota:</span>
                  <span className="font-mono font-bold text-purple-400">
                    {plan.storageLimitMb} MB
                  </span>
                </div>
              </div>
            </div>

            <button
              onClick={() => handleEdit(plan)}
              className="w-full py-2 rounded-xl text-xs font-bold bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 transition-all flex items-center justify-center gap-2"
            >
              <Edit2 className="w-3.5 h-3.5" />
              <span>Edit Plan Parameters</span>
            </button>
          </div>
        ))}
      </div>

      {/* Edit Plan Modal */}
      {editingPlan && (
        <div className="fixed inset-0 z-50 bg-black/75 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 w-full max-w-lg rounded-3xl p-6 sm:p-8 shadow-2xl space-y-6 relative">
            <button
              onClick={() => setEditingPlan(null)}
              className="absolute top-6 right-6 p-2 rounded-xl text-slate-400 hover:text-white bg-slate-950/60 hover:bg-slate-800 transition-colors"
            >
              <X className="w-4 h-4" />
            </button>

            <div>
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-400 border border-rose-500/30 font-mono">
                PLAN CONFIGURATION
              </span>
              <h2 className="text-xl font-bold text-white mt-1">
                Edit {editingPlan.name} ({editingPlan.code})
              </h2>
            </div>

            <form onSubmit={handleSave} className="space-y-4 text-xs">
              <div className="space-y-1">
                <label className="text-slate-400 font-medium">Plan Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  required
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-rose-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-slate-400 font-medium">Monthly Price (INR)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.priceMonthly}
                    onChange={(e) => setFormData({ ...formData, priceMonthly: parseFloat(e.target.value) })}
                    required
                    className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-rose-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-slate-400 font-medium">Yearly Price (INR)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={formData.priceYearly}
                    onChange={(e) => setFormData({ ...formData, priceYearly: parseFloat(e.target.value) })}
                    required
                    className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-rose-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <label className="text-slate-400 font-medium">Monthly AI Limit</label>
                  <input
                    type="number"
                    value={formData.monthlyAiLimit}
                    onChange={(e) => setFormData({ ...formData, monthlyAiLimit: parseInt(e.target.value) })}
                    required
                    className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-rose-500"
                  />
                </div>

                <div className="space-y-1">
                  <label className="text-slate-400 font-medium">Storage Limit (MB)</label>
                  <input
                    type="number"
                    value={formData.storageLimitMb}
                    onChange={(e) => setFormData({ ...formData, storageLimitMb: parseInt(e.target.value) })}
                    required
                    className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-rose-500"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setEditingPlan(null)}
                  className="px-4 py-2 rounded-xl text-xs font-bold text-slate-400 hover:text-white bg-slate-950 border border-slate-800"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={saveLoading}
                  className="px-5 py-2 rounded-xl text-xs font-bold bg-rose-600 hover:bg-rose-500 text-white shadow-lg shadow-rose-600/20"
                >
                  {saveLoading ? 'Saving...' : 'Save Plan Changes'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminPlansPage;
