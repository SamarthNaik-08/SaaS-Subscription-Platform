import React, { useState, useEffect } from 'react';
import { Check, Sparkles } from 'lucide-react';
import { billingService } from '../../services/billingService';
import { useAuth } from '../../context/AuthContext';

export const SubscriptionPage = () => {
  const { user } = useAuth();
  const [plans, setPlans] = useState([]);
  const [subscription, setSubscription] = useState(null);
  const [billingInterval, setBillingInterval] = useState('MONTHLY');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [statusMsg, setStatusMsg] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [plansRes, subRes] = await Promise.allSettled([
        billingService.getPlans(),
        billingService.getCurrentSubscription(),
      ]);

      if (plansRes.status === 'fulfilled' && plansRes.value.success) {
        setPlans(plansRes.value.data);
      }
      if (subRes.status === 'fulfilled' && subRes.value.success) {
        setSubscription(subRes.value.data);
      }
    } catch (e) {
      console.error('Failed to load subscription data', e);
    } finally {
      setLoading(false);
    }
  };

  const handleUpgrade = async (planCode) => {
    if (planCode === 'FREE') return;
    setActionLoading(true);
    setErrorMsg(null);
    setStatusMsg(null);

    try {
      // 1. Create Payment Order on backend (server calculates tax + amount)
      const orderRes = await billingService.createPaymentOrder(planCode, billingInterval);
      if (!orderRes.success || !orderRes.data) {
        throw new Error(orderRes.message || 'Failed to create payment order');
      }

      const order = orderRes.data;

      // 2. Check if Razorpay script is loaded or running in Sandbox
      if (window.Razorpay && order.gatewayProvider === 'RAZORPAY') {
        const options = {
          key: order.keyId,
          amount: Math.round(order.amount * 100),
          currency: order.currency || 'INR',
          name: 'Nexus AI SaaS',
          description: `${order.planName} (${order.billingInterval})`,
          order_id: order.gatewayOrderId,
          handler: async (response) => {
            try {
              const verifyRes = await billingService.verifyPayment({
                gatewayOrderId: response.razorpay_order_id,
                gatewayPaymentId: response.razorpay_payment_id,
                gatewaySignature: response.razorpay_signature,
              });

              if (verifyRes.success) {
                setStatusMsg(`Successfully upgraded to ${order.planName}! Invoice #${verifyRes.data.invoiceNumber} generated.`);
                loadData();
              }
            } catch (err) {
              setErrorMsg(err.response?.data?.message || 'Payment verification failed.');
            }
          },
          prefill: {
            name: `${user?.firstName} ${user?.lastName}`,
            email: user?.email,
          },
          theme: { color: '#6366f1' },
        };

        const rzp = new window.Razorpay(options);
        rzp.open();
      } else {
        // Fallback / Sandbox automatic simulation flow
        const verifyRes = await billingService.verifyPayment({
          gatewayOrderId: order.gatewayOrderId,
          gatewayPaymentId: 'pay_sandbox_' + Date.now(),
          gatewaySignature: 'sig_sandbox_' + Date.now(),
        });

        if (verifyRes.success) {
          setStatusMsg(`Payment verified in Sandbox mode! Upgraded to ${order.planName}.`);
          loadData();
        }
      }
    } catch (err) {
      console.error('Upgrade error:', err);
      setErrorMsg(err.response?.data?.message || err.message || 'Payment initiation failed.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm('Are you sure you want to cancel your paid subscription? You will retain access until the end of your billing cycle.')) {
      return;
    }

    setActionLoading(true);
    setErrorMsg(null);
    try {
      const res = await billingService.cancelSubscription();
      if (res.success) {
        setStatusMsg('Subscription set to cancel at the end of the current billing period.');
        loadData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to cancel subscription.');
    } finally {
      setActionLoading(false);
    }
  };

  const handleResume = async () => {
    setActionLoading(true);
    setErrorMsg(null);
    try {
      const res = await billingService.resumeSubscription();
      if (res.success) {
        setStatusMsg('Subscription successfully resumed!');
        loadData();
      }
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to resume subscription.');
    } finally {
      setActionLoading(false);
    }
  };

  const currentPlanCode = subscription?.plan?.code || 'FREE';

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="text-center max-w-3xl mx-auto space-y-4 pt-4">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/10 text-indigo-400 border border-indigo-500/30">
          <Sparkles className="w-3.5 h-3.5" />
          <span>Flexible Plans & Transparent Pricing</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
          Scale your AI capabilities without limits
        </h1>
        <p className="text-sm text-slate-400 leading-relaxed">
          Upgrade your personal account tier for higher monthly request limits, larger storage quotas, and multi-model access.
        </p>

        {/* Monthly / Yearly Toggle */}
        <div className="flex items-center justify-center pt-2">
          <div className="p-1 rounded-2xl bg-slate-900 border border-slate-800 flex items-center space-x-1">
            <button
              onClick={() => setBillingInterval('MONTHLY')}
              className={`px-4 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                billingInterval === 'MONTHLY'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Monthly Billing
            </button>
            <button
              onClick={() => setBillingInterval('YEARLY')}
              className={`flex items-center space-x-1.5 px-4 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                billingInterval === 'YEARLY'
                  ? 'bg-indigo-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <span>Yearly Billing</span>
              <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-emerald-500 text-slate-950 uppercase">
                Save 17%
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Status Notifications */}
      {statusMsg && (
        <div className="max-w-xl mx-auto p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs font-semibold text-center animate-in fade-in">
          {statusMsg}
        </div>
      )}
      {errorMsg && (
        <div className="max-w-xl mx-auto p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs font-semibold text-center animate-in fade-in">
          {errorMsg}
        </div>
      )}

      {/* Pricing Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 max-w-6xl mx-auto">
        {/* FREE PLAN */}
        <div className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-white">Free</h2>
              {currentPlanCode === 'FREE' && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700">
                  Current Plan
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400">Essential quota for casual testing and basic AI tasks.</p>
            <div className="flex items-baseline space-x-1">
              <span className="text-3xl font-black text-white">₹0</span>
              <span className="text-xs text-slate-400">/ month</span>
            </div>

            <ul className="space-y-2.5 text-xs text-slate-300 pt-2 border-t border-slate-800/80">
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>50 AI generation requests / mo</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>100 MB context storage</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-emerald-400 shrink-0" />
                <span>Gemini 1.5 Flash Model</span>
              </li>
              <li className="flex items-center gap-2 text-slate-500">
                <Check className="w-4 h-4 text-slate-600 shrink-0" />
                <span>Standard generation latency</span>
              </li>
            </ul>
          </div>

          <button
            disabled={true}
            className="w-full py-2.5 rounded-xl font-bold text-xs bg-slate-800 text-slate-500 cursor-not-allowed"
          >
            {currentPlanCode === 'FREE' ? 'Active Tier' : 'Downgrade'}
          </button>
        </div>

        {/* PRO PLAN (Featured) */}
        <div className="relative p-6 rounded-3xl bg-gradient-to-b from-indigo-950/60 to-slate-900 border-2 border-indigo-500/50 shadow-2xl flex flex-col justify-between space-y-6">
          <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full text-[10px] font-extrabold uppercase tracking-wider bg-gradient-to-r from-indigo-500 to-purple-500 text-white shadow-md">
            Most Popular
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <span>Pro</span>
                <Sparkles className="w-4 h-4 text-indigo-400" />
              </h2>
              {currentPlanCode === 'PRO' && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/40">
                  Current Plan
                </span>
              )}
            </div>
            <p className="text-xs text-slate-300">For creators, developers, and power users who need scale.</p>
            <div className="flex items-baseline space-x-1">
              <span className="text-3xl font-black text-white">
                {billingInterval === 'YEARLY' ? '₹4,990' : '₹499'}
              </span>
              <span className="text-xs text-slate-400">
                / {billingInterval === 'YEARLY' ? 'year' : 'month'} + GST
              </span>
            </div>

            <ul className="space-y-2.5 text-xs text-slate-200 pt-2 border-t border-slate-800/80">
              <li className="flex items-center gap-2 font-medium">
                <Check className="w-4 h-4 text-indigo-400 shrink-0" />
                <span>1,000 AI generation requests / mo</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-indigo-400 shrink-0" />
                <span>5 GB context storage</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-indigo-400 shrink-0" />
                <span>Gemini 1.5 Pro, GPT-4o & Claude</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-indigo-400 shrink-0" />
                <span>Priority inference speed & low latency</span>
              </li>
            </ul>
          </div>

          <div>
            {currentPlanCode === 'PRO' ? (
              <div className="space-y-2">
                {subscription?.cancelAtPeriodEnd ? (
                  <button
                    onClick={handleResume}
                    disabled={actionLoading}
                    className="w-full py-2.5 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-500 text-white shadow transition-all"
                  >
                    Resume Pro Subscription
                  </button>
                ) : (
                  <button
                    onClick={handleCancel}
                    disabled={actionLoading}
                    className="w-full py-2.5 rounded-xl font-bold text-xs bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-all"
                  >
                    Cancel Subscription
                  </button>
                )}
              </div>
            ) : (
              <button
                onClick={() => handleUpgrade('PRO')}
                disabled={actionLoading}
                className="w-full py-2.5 rounded-xl font-bold text-xs bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white shadow-lg shadow-indigo-500/25 transition-all hover:scale-[1.02]"
              >
                {actionLoading ? 'Processing Checkout...' : 'Upgrade to Pro'}
              </button>
            )}
          </div>
        </div>

        {/* BUSINESS PLAN */}
        <div className="p-6 rounded-3xl bg-slate-900/60 border border-slate-800 flex flex-col justify-between space-y-6">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-white">Business</h2>
              {currentPlanCode === 'BUSINESS' && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/40">
                  Current Plan
                </span>
              )}
            </div>
            <p className="text-xs text-slate-400">Maximum inference volume for heavy automated workflows.</p>
            <div className="flex items-baseline space-x-1">
              <span className="text-3xl font-black text-white">
                {billingInterval === 'YEARLY' ? '₹14,990' : '₹1,499'}
              </span>
              <span className="text-xs text-slate-400">
                / {billingInterval === 'YEARLY' ? 'year' : 'month'} + GST
              </span>
            </div>

            <ul className="space-y-2.5 text-xs text-slate-300 pt-2 border-t border-slate-800/80">
              <li className="flex items-center gap-2 font-medium">
                <Check className="w-4 h-4 text-purple-400 shrink-0" />
                <span>5,000 AI generation requests / mo</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-purple-400 shrink-0" />
                <span>50 GB dedicated storage</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-purple-400 shrink-0" />
                <span>Unlimited all-models access</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-4 h-4 text-purple-400 shrink-0" />
                <span>Dedicated burst rate limits</span>
              </li>
            </ul>
          </div>

          <div>
            {currentPlanCode === 'BUSINESS' ? (
              <div className="space-y-2">
                {subscription?.cancelAtPeriodEnd ? (
                  <button
                    onClick={handleResume}
                    disabled={actionLoading}
                    className="w-full py-2.5 rounded-xl font-bold text-xs bg-emerald-600 hover:bg-emerald-500 text-white shadow transition-all"
                  >
                    Resume Business Subscription
                  </button>
                ) : (
                  <button
                    onClick={handleCancel}
                    disabled={actionLoading}
                    className="w-full py-2.5 rounded-xl font-bold text-xs bg-rose-500/10 hover:bg-rose-500/20 text-rose-400 border border-rose-500/30 transition-all"
                  >
                    Cancel Subscription
                  </button>
                )}
              </div>
            ) : (
              <button
                onClick={() => handleUpgrade('BUSINESS')}
                disabled={actionLoading}
                className="w-full py-2.5 rounded-xl font-bold text-xs bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 transition-all"
              >
                {actionLoading ? 'Processing Checkout...' : 'Upgrade to Business'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SubscriptionPage;
