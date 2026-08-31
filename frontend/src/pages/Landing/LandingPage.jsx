import React from 'react';
import { Link } from 'react-router-dom';
import {
  Sparkles,
  Shield,
  Zap,
  Bot,
  Database,
  ArrowRight,
  CheckCircle2,
  Lock,
  Layers,
  Activity,
} from 'lucide-react';
import { PLANS } from '../../utils/constants';

export const LandingPage = () => {
  return (
    <div className="space-y-24 pb-20">
      {/* Hero Section */}
      <section className="relative pt-20 pb-16 overflow-hidden">
        {/* Glow effect */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-indigo-500/15 rounded-full blur-3xl pointer-events-none -z-10" />

        <div className="max-w-5xl mx-auto px-4 text-center space-y-8">
          <div className="inline-flex items-center space-x-2 px-3 py-1.5 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs font-semibold tracking-wide uppercase shadow-sm">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            <span>Next-Generation Consumer AI Studio & Inference Platform</span>
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white leading-tight sm:leading-none">
            Next-Gen AI Capabilities with <br className="hidden sm:block" />
            <span className="bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
              Direct Account Ownership & Metering
            </span>
          </h1>

          <p className="max-w-2xl mx-auto text-lg text-slate-300">
            A production-ready Consumer AI platform built with Spring Boot 3, PostgreSQL, Spring Security JWT, multi-model AI routing, and modern React studio architecture.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4">
            <Link
              to="/register"
              className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-8 py-3.5 rounded-xl bg-gradient-to-r from-indigo-600 via-indigo-500 to-purple-600 hover:from-indigo-500 hover:to-purple-500 text-white font-semibold text-base transition-all shadow-xl shadow-indigo-600/30 hover:scale-[1.02]"
            >
              <span>Get Started Free</span>
              <ArrowRight className="w-5 h-5" />
            </Link>
            <Link
              to="/login"
              className="w-full sm:w-auto inline-flex items-center justify-center space-x-2 px-8 py-3.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-slate-700 text-slate-200 font-semibold text-base transition-all"
            >
              <span>Sign In</span>
            </Link>
          </div>

          {/* Quick Metrics Badge */}
          <div className="pt-8 grid grid-cols-2 md:grid-cols-4 gap-4 max-w-3xl mx-auto text-left">
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <p className="text-2xl font-bold text-white">Direct</p>
              <p className="text-xs text-slate-400">Account Ownership</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <p className="text-2xl font-bold text-indigo-400">JWT + BCrypt</p>
              <p className="text-xs text-slate-400">Zero Trust Auth</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <p className="text-2xl font-bold text-purple-400">Multi-Model</p>
              <p className="text-xs text-slate-400">Gemini, GPT-4o, Claude</p>
            </div>
            <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
              <p className="text-2xl font-bold text-emerald-400">Pessimistic Lock</p>
              <p className="text-xs text-slate-400">Atomic Metering</p>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <h2 className="text-xs font-bold text-indigo-400 tracking-wider uppercase">Core Architecture</h2>
          <p className="text-3xl font-bold text-white">Built for True Consumer Scale</p>
          <p className="text-slate-400 text-sm">
            Everything your personal AI workflow requires, from multi-model prompt generation to real-time quota guardrails.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 hover:border-slate-700 transition-colors space-y-4">
            <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <Bot className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Multi-Model AI Studio</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Generate text, chat with intelligent assistants, and route inference across state-of-the-art LLMs.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 hover:border-slate-700 transition-colors space-y-4">
            <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Lock className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Stateless JWT + Refresh Rotation</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Short-lived 15-minute access tokens paired with SHA-256 hashed refresh token rotation and multi-session revocation.
            </p>
          </div>

          <div className="p-6 rounded-2xl bg-slate-900/50 border border-slate-800 hover:border-slate-700 transition-colors space-y-4">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <Layers className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white">Tiered Subscription Engine</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Authoritative FREE, PRO, and BUSINESS plans with database-driven quota limits on monthly requests and storage.
            </p>
          </div>
        </div>
      </section>

      {/* Pricing Section */}
      <section id="pricing" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="text-center max-w-3xl mx-auto mb-16 space-y-3">
          <h2 className="text-xs font-bold text-indigo-400 tracking-wider uppercase">Transparent Tiers</h2>
          <p className="text-3xl font-bold text-white">Choose the Plan That Fits Your Vision</p>
          <p className="text-slate-400 text-sm">
            Every newly registered user starts automatically with the Free tier.
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8">
          {Object.values(PLANS).map((plan) => {
            const isPopular = plan.code === 'PRO';
            return (
              <div
                key={plan.code}
                className={`relative rounded-2xl p-8 flex flex-col justify-between transition-all ${
                  isPopular
                    ? 'bg-gradient-to-b from-indigo-950/60 to-slate-900 border-2 border-indigo-500/80 shadow-2xl shadow-indigo-500/15'
                    : 'bg-slate-900/60 border border-slate-800'
                }`}
              >
                {isPopular && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-0.5 rounded-full bg-indigo-500 text-white text-[11px] font-bold uppercase tracking-wider shadow">
                    Most Popular
                  </div>
                )}

                <div className="space-y-6">
                  <div>
                    <h3 className="text-xl font-bold text-white">{plan.name}</h3>
                    <p className="text-xs text-slate-400 mt-1">Tier Code: {plan.code}</p>
                  </div>

                  <div className="flex items-baseline space-x-1">
                    <span className="text-4xl font-extrabold text-white">{plan.currency}{plan.price}</span>
                    <span className="text-slate-400 text-sm">/{plan.interval}</span>
                  </div>

                  <ul className="space-y-3 text-sm text-slate-300">
                    <li className="flex items-center space-x-3">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span><strong>{plan.aiLimit}</strong> AI Requests/mo</span>
                    </li>
                    <li className="flex items-center space-x-3">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span><strong>{plan.storageMb >= 1024 ? `${plan.storageMb / 1024} GB` : `${plan.storageMb} MB`}</strong> Storage Limit</span>
                    </li>
                    <li className="flex items-center space-x-3">
                      <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span>Inference Speed & Low Latency</span>
                    </li>
                  </ul>
                </div>

                <div className="pt-8">
                  <Link
                    to="/register"
                    className={`w-full py-3 rounded-xl font-semibold text-sm flex items-center justify-center transition-all ${
                      isPopular
                        ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30'
                        : 'bg-slate-800 hover:bg-slate-700 text-white border border-slate-700'
                    }`}
                  >
                    {plan.price === 0 ? 'Start Free' : 'Get Started'}
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      </section>
    </div>
  );
};

export default LandingPage;
