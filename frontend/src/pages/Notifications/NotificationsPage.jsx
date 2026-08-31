import React, { useState, useEffect } from 'react';
import {
  Bell,
  CheckCheck,
  Sparkles,
  CreditCard,
  AlertTriangle,
  ShieldAlert,
  Info,
  Check,
} from 'lucide-react';
import { notificationService } from '../../services/notificationService';

export const NotificationsPage = () => {
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeFilter, setActiveFilter] = useState('ALL'); // 'ALL' | 'UNREAD'

  useEffect(() => {
    loadNotifications();
  }, []);

  const loadNotifications = async () => {
    try {
      const res = await notificationService.getNotifications();
      if (res.success && res.data) {
        setNotifications(res.data);
      }
    } catch (e) {
      console.error('Failed to load notifications', e);
    } finally {
      setLoading(false);
    }
  };

  const handleMarkAsRead = async (id) => {
    try {
      await notificationService.markAsRead(id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === id ? { ...n, read: true, isRead: true } : n))
      );
    } catch (e) {
      console.error('Failed to mark read', e);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) =>
        prev.map((n) => ({ ...n, read: true, isRead: true }))
      );
    } catch (e) {
      console.error('Failed to mark all read', e);
    }
  };

  const getIcon = (type) => {
    switch (type) {
      case 'WELCOME':
        return <Sparkles className="w-4 h-4 text-indigo-400" />;
      case 'PAYMENT_SUCCESS':
      case 'INVOICE_CREATED':
      case 'SUBSCRIPTION_UPGRADED':
        return <CreditCard className="w-4 h-4 text-emerald-400" />;
      case 'USAGE_WARNING_75':
      case 'USAGE_WARNING_90':
      case 'QUOTA_EXCEEDED':
        return <AlertTriangle className="w-4 h-4 text-amber-400" />;
      case 'SECURITY_ALERT':
      case 'PAYMENT_FAILED':
        return <ShieldAlert className="w-4 h-4 text-rose-400" />;
      default:
        return <Info className="w-4 h-4 text-slate-400" />;
    }
  };

  const filteredNotifications =
    activeFilter === 'UNREAD'
      ? notifications.filter((n) => !n.read && !n.isRead)
      : notifications;

  const unreadTotal = notifications.filter((n) => !n.read && !n.isRead).length;

  return (
    <div className="space-y-8 max-w-4xl mx-auto">
      {/* Top Banner */}
      <div className="p-8 rounded-3xl bg-slate-900/80 border border-slate-800 shadow-xl flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
            <Bell className="w-5 h-5" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">Notifications Inbox</h1>
            <p className="text-xs text-slate-400">
              System alerts, billing updates, and quota warnings
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {unreadTotal > 0 && (
            <button
              onClick={handleMarkAllAsRead}
              className="px-4 py-2 rounded-xl text-xs font-bold bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 border border-indigo-500/30 transition-all flex items-center gap-1.5"
            >
              <CheckCheck className="w-3.5 h-3.5" />
              <span>Mark All as Read</span>
            </button>
          )}

          {/* Filter Pills */}
          <div className="p-1 rounded-2xl bg-slate-950 border border-slate-800 flex items-center space-x-1">
            <button
              onClick={() => setActiveFilter('ALL')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                activeFilter === 'ALL'
                  ? 'bg-slate-800 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              All ({notifications.length})
            </button>
            <button
              onClick={() => setActiveFilter('UNREAD')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition-all ${
                activeFilter === 'UNREAD'
                  ? 'bg-slate-800 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Unread ({unreadTotal})
            </button>
          </div>
        </div>
      </div>

      {/* Notifications List */}
      <div className="p-6 rounded-2xl bg-slate-900/60 border border-slate-800 space-y-3">
        {filteredNotifications.length > 0 ? (
          filteredNotifications.map((notif) => {
            const isUnread = !notif.read && !notif.isRead;
            return (
              <div
                key={notif.id}
                className={`p-4 rounded-2xl border transition-all flex items-start justify-between gap-4 ${
                  isUnread
                    ? 'bg-slate-950 border-indigo-500/40 shadow-sm'
                    : 'bg-slate-950/40 border-slate-800/60 text-slate-400'
                }`}
              >
                <div className="flex items-start space-x-3.5">
                  <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 shrink-0">
                    {getIcon(notif.type)}
                  </div>
                  <div className="space-y-1">
                    <div className="flex items-center space-x-2">
                      <h3
                        className={`text-xs font-bold ${
                          isUnread ? 'text-white' : 'text-slate-300'
                        }`}
                      >
                        {notif.title}
                      </h3>
                      {isUnread && (
                        <span className="w-2 h-2 rounded-full bg-indigo-400" />
                      )}
                    </div>
                    <p className="text-xs text-slate-300 leading-relaxed">
                      {notif.message}
                    </p>
                    <p className="text-[10px] text-slate-500 font-mono">
                      {new Date(notif.createdAt).toLocaleString()}
                    </p>
                  </div>
                </div>

                {isUnread && (
                  <button
                    onClick={() => handleMarkAsRead(notif.id)}
                    className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-300 hover:bg-slate-900 transition-colors shrink-0"
                    title="Mark as Read"
                  >
                    <Check className="w-4 h-4" />
                  </button>
                )}
              </div>
            );
          })
        ) : (
          <p className="text-xs text-slate-500 py-12 text-center">
            {activeFilter === 'UNREAD' ? 'No unread notifications.' : 'No notifications in your inbox.'}
          </p>
        )}
      </div>
    </div>
  );
};

export default NotificationsPage;
