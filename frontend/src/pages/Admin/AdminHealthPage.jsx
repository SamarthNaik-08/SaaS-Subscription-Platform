import React, { useState, useEffect } from 'react';
import { Cpu, Server, Database, HardDrive, RefreshCw, CheckCircle2, Clock } from 'lucide-react';
import { adminService } from '../../services/adminService';

export const AdminHealthPage = () => {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadHealth();
    const interval = setInterval(loadHealth, 15000);
    return () => clearInterval(interval);
  }, []);

  const loadHealth = async () => {
    try {
      const res = await adminService.getHealth();
      if (res.success && res.data) {
        setHealth(res.data);
      }
    } catch (e) {
      console.error('Failed to load health metrics', e);
    } finally {
      setLoading(false);
    }
  };

  const formatBytes = (bytes) => {
    if (!bytes || bytes === 0) return '0 MB';
    const mb = bytes / (1024 * 1024);
    return mb.toFixed(1) + ' MB';
  };

  const heapUsed = health?.details?.heapMemoryUsedBytes;
  const heapMax = health?.details?.heapMemoryMaxBytes;
  const heapPct = heapUsed && heapMax ? Math.round((heapUsed / heapMax) * 100) : 0;

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400">
            <Cpu className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Platform System Telemetry & Health</h1>
            <p className="text-xs text-slate-400">JVM memory pools, database connectivity, and runtime metrics</p>
          </div>
        </div>

        <button
          onClick={loadHealth}
          className="p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-400 hover:text-white transition-colors"
          title="Refresh telemetry"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>

      {/* Grid: Health Metric Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
        {/* Status */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs text-slate-400 font-semibold uppercase">Cluster Status</span>
          <div className="flex items-center space-x-2">
            <span
              className={`w-3 h-3 rounded-full ${
                health?.status === 'UP' ? 'bg-emerald-400 animate-pulse' : 'bg-rose-500'
              }`}
            />
            <p className="text-2xl font-black text-white">{health?.status || 'UNKNOWN'}</p>
          </div>
          <p className="text-[11px] text-emerald-400 font-semibold">PostgreSQL Connected</p>
        </div>

        {/* Uptime */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs text-slate-400 font-semibold uppercase">Application Uptime</span>
          <p className="text-2xl font-black text-white">
            {health?.uptimeSeconds ? `${Math.floor(health.uptimeSeconds / 60)} mins` : '0 mins'}
          </p>
          <p className="text-[11px] text-slate-400 font-mono">Java {health?.javaVersion || '17'}</p>
        </div>

        {/* Processors & Threads */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs text-slate-400 font-semibold uppercase">CPU & Thread Pool</span>
          <p className="text-2xl font-black text-white">{health?.availableProcessors || 4} Cores</p>
          <p className="text-[11px] text-slate-400 font-mono">{health?.activeThreads || 0} active threads</p>
        </div>

        {/* Heap Memory */}
        <div className="p-5 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-2">
          <span className="text-xs text-slate-400 font-semibold uppercase">Heap Memory Pool</span>
          <p className="text-2xl font-black text-white">{formatBytes(heapUsed)}</p>
          <p className="text-[11px] text-indigo-400 font-semibold">{heapPct}% allocated</p>
        </div>
      </div>

      {/* Heap Memory Detailed Progress */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800 pb-3">
          <HardDrive className="w-4 h-4 text-rose-400" />
          JVM Memory Telemetry
        </h2>

        <div className="space-y-2">
          <div className="flex justify-between text-xs">
            <span className="text-slate-400">Heap Used: {formatBytes(heapUsed)}</span>
            <span className="text-slate-400 font-mono">Max Capacity: {formatBytes(heapMax)}</span>
          </div>
          <div className="w-full h-3 bg-slate-950 rounded-full overflow-hidden p-0.5 border border-slate-800">
            <div
              className="h-full rounded-full bg-gradient-to-r from-emerald-500 via-indigo-500 to-rose-500 transition-all duration-500"
              style={{ width: `${Math.min(100, heapPct)}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminHealthPage;
