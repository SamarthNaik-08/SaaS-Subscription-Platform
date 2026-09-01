import React, { useState, useEffect } from 'react';
import { User, Lock, Smartphone, LogOut, Save } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { userService } from '../../services/userService';

export const SettingsPage = () => {
  const { user, updateUser } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [sessions, setSessions] = useState([]);
  const [profileMsg, setProfileMsg] = useState(null);
  const [passwordMsg, setPasswordMsg] = useState(null);
  const [passwordError, setPasswordError] = useState(null);
  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadingPassword, setLoadingPassword] = useState(false);
  const [loadingSessions, setLoadingSessions] = useState(false);

  useEffect(() => {
    loadSessions();
  }, []);

  const loadSessions = async () => {
    try {
      setLoadingSessions(true);
      const res = await userService.getSessions();
      if (res.success && res.data) {
        setSessions(res.data);
      }
    } catch (e) {
      console.warn('Failed to load active sessions', e);
    } finally {
      setLoadingSessions(false);
    }
  };

  const handleUpdateProfile = async (e) => {
    e.preventDefault();
    setLoadingProfile(true);
    setProfileMsg(null);

    try {
      const res = await userService.updateProfile({ firstName, lastName });
      if (res.success && res.data) {
        updateUser(res.data);
        setProfileMsg('Profile details updated successfully!');
      }
    } catch (err) {
      setProfileMsg(err.response?.data?.message || 'Failed to update profile.');
    } finally {
      setLoadingProfile(false);
    }
  };

  const handleChangePassword = async (e) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordMsg(null);

    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirm password do not match.');
      return;
    }

    if (newPassword.length < 8) {
      setPasswordError('New password must be at least 8 characters long.');
      return;
    }

    setLoadingPassword(true);

    try {
      const res = await userService.changePassword({ currentPassword, newPassword });
      if (res.success) {
        setPasswordMsg('Password changed successfully! All other active sessions have been signed out.');
        setCurrentPassword('');
        setNewPassword('');
        setConfirmPassword('');
        loadSessions();
      }
    } catch (err) {
      setPasswordError(err.response?.data?.message || 'Failed to change password.');
    } finally {
      setLoadingPassword(false);
    }
  };

  const handleRevokeAllSessions = async () => {
    if (!window.confirm('Revoke all other active sessions? You will remain signed in on this browser.')) {
      return;
    }

    try {
      const res = await userService.revokeAllSessions();
      if (res.success) {
        loadSessions();
      }
    } catch (e) {
      console.error('Failed to revoke sessions', e);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex items-center space-x-3">
        <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
          <User className="w-5 h-5" />
        </div>
        <div>
          <h1 className="text-2xl font-bold text-white">Account Settings & Security</h1>
          <p className="text-xs text-slate-400">Manage your profile, password, and active session credentials</p>
        </div>
      </div>

      {/* Grid: Profile & Password */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Profile Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800/80 pb-3">
            <User className="w-4 h-4 text-indigo-400" />
            Personal Profile
          </h2>

          {profileMsg && (
            <div className="p-3 rounded-xl bg-indigo-500/10 border border-indigo-500/30 text-indigo-300 text-xs">
              {profileMsg}
            </div>
          )}

          <form onSubmit={handleUpdateProfile} className="space-y-4 text-xs">
            <div className="space-y-1">
              <label className="text-slate-400 font-medium">First Name</label>
              <input
                type="text"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                required
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div className="space-y-1">
              <label className="text-slate-400 font-medium">Last Name</label>
              <input
                type="text"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                required
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div className="space-y-1">
              <label className="text-slate-400 font-medium">Email Address (Immutable)</label>
              <input
                type="email"
                value={user?.email || ''}
                disabled
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950/50 border border-slate-800/50 text-slate-500 cursor-not-allowed"
              />
            </div>

            <button
              type="submit"
              disabled={loadingProfile}
              className="w-full py-2.5 rounded-xl font-bold text-xs bg-indigo-600 hover:bg-indigo-500 text-white shadow transition-all flex items-center justify-center gap-2"
            >
              <Save className="w-3.5 h-3.5" />
              <span>{loadingProfile ? 'Saving...' : 'Save Profile Changes'}</span>
            </button>
          </form>
        </div>

        {/* Change Password Card */}
        <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
          <h2 className="text-sm font-bold text-white flex items-center gap-2 border-b border-slate-800/80 pb-3">
            <Lock className="w-4 h-4 text-indigo-400" />
            Change Password
          </h2>

          {passwordMsg && (
            <div className="p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs">
              {passwordMsg}
            </div>
          )}
          {passwordError && (
            <div className="p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-300 text-xs">
              {passwordError}
            </div>
          )}

          <form onSubmit={handleChangePassword} className="space-y-3.5 text-xs">
            <div className="space-y-1">
              <label className="text-slate-400 font-medium">Current Password</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                required
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div className="space-y-1">
              <label className="text-slate-400 font-medium">New Password (min. 8 characters)</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={8}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <div className="space-y-1">
              <label className="text-slate-400 font-medium">Confirm New Password</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={8}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-slate-800 text-slate-100 focus:outline-none focus:border-indigo-500 transition-colors"
              />
            </div>

            <p className="text-[11px] text-amber-400/90 leading-tight">
              Changing your password will immediately terminate and revoke all other active sessions.
            </p>

            <button
              type="submit"
              disabled={loadingPassword}
              className="w-full py-2.5 rounded-xl font-bold text-xs bg-slate-800 hover:bg-slate-700 text-white border border-slate-700 shadow transition-all"
            >
              {loadingPassword ? 'Updating Password...' : 'Update Password'}
            </button>
          </form>
        </div>
      </div>

      {/* Active Sessions & Security Revocation */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-4">
        <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
          <div>
            <h2 className="text-sm font-bold text-white flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-indigo-400" />
              Active Refresh Sessions
            </h2>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Cryptographically hashed refresh tokens stored in database
            </p>
          </div>

          <button
            onClick={handleRevokeAllSessions}
            className="px-3.5 py-1.5 rounded-xl text-xs font-bold text-rose-400 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 transition-all flex items-center gap-1.5"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Revoke All Other Sessions</span>
          </button>
        </div>

        {sessions.length > 0 ? (
          <div className="space-y-2.5">
            {sessions.map((sess, idx) => (
              <div
                key={sess.id || idx}
                className="p-3 rounded-xl bg-slate-950/70 border border-slate-800/80 flex items-center justify-between text-xs"
              >
                <div className="flex items-center space-x-3">
                  <div className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
                  <div>
                    <p className="font-mono text-slate-300">
                      Session #{sess.id ? sess.id.substring(0, 8) : idx + 1}
                    </p>
                    <p className="text-[11px] text-slate-500">
                      Issued: {new Date(sess.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>
                <div className="text-right">
                  <span
                    className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      sess.revoked
                        ? 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                    }`}
                  >
                    {sess.revoked ? 'REVOKED' : 'ACTIVE'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-xs text-slate-500 py-4 text-center">No active sessions found.</p>
        )}
      </div>
    </div>
  );
};

export default SettingsPage;
