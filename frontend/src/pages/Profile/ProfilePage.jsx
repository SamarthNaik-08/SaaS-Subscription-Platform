import React from 'react';
import { User, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export const ProfilePage = () => {
  const { user } = useAuth();

  return (
    <div className="space-y-8 pb-12">
      <div>
        <h1 className="text-2xl font-bold text-white">User Profile</h1>
        <p className="text-xs text-slate-400">Account identity and credentials</p>
      </div>

      <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-6 space-y-6">
        <div className="flex items-center space-x-4">
          <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-purple-600 to-indigo-600 flex items-center justify-center text-white font-bold text-xl shadow-lg">
            {user?.firstName ? user.firstName.charAt(0) : 'U'}
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">{user?.firstName} {user?.lastName}</h2>
            <p className="text-xs text-slate-400">{user?.email}</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-4 border-t border-slate-800">
          <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-1">
            <span className="text-xs text-slate-400">User ID (UUID)</span>
            <p className="text-xs font-mono text-slate-200 truncate">{user?.id}</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-1">
            <span className="text-xs text-slate-400">Global Role</span>
            <p className="text-sm font-semibold text-indigo-400">{user?.role || 'USER'}</p>
          </div>

          <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-1">
            <span className="text-xs text-slate-400">Account Status</span>
            <p className="text-sm font-semibold text-emerald-400 flex items-center space-x-1">
              <CheckCircle2 className="w-4 h-4" />
              <span>{user?.status || 'ACTIVE'}</span>
            </p>
          </div>

          <div className="p-4 rounded-xl bg-slate-800/40 border border-slate-800 space-y-1">
            <span className="text-xs text-slate-400">Last Login</span>
            <p className="text-sm font-semibold text-slate-200">
              {user?.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString() : 'Current Session'}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
