import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import PublicLayout from '../layouts/PublicLayout';
import DashboardLayout from '../layouts/DashboardLayout';
import AdminLayout from '../layouts/AdminLayout';
import { ProtectedRoute } from './ProtectedRoute';
import { AdminProtectedRoute } from './AdminProtectedRoute';

// Public Pages
import LandingPage from '../pages/Landing/LandingPage';
import LoginPage from '../pages/Login/LoginPage';
import RegisterPage from '../pages/Register/RegisterPage';

// Consumer App Pages
import DashboardPage from '../pages/Dashboard/DashboardPage';
import AIStudioPage from '../pages/AIStudio/AIStudioPage';
import UsagePage from '../pages/Usage/UsagePage';
import SubscriptionPage from '../pages/Subscription/SubscriptionPage';
import InvoicesPage from '../pages/Billing/InvoicesPage';
import NotificationsPage from '../pages/Notifications/NotificationsPage';
import SettingsPage from '../pages/Profile/SettingsPage';

// Admin Portal Pages
import AdminDashboardPage from '../pages/Admin/AdminDashboardPage';
import AdminAnalyticsPage from '../pages/Admin/AdminAnalyticsPage';
import AdminUsersPage from '../pages/Admin/AdminUsersPage';
import AdminSubscriptionsPage from '../pages/Admin/AdminSubscriptionsPage';
import AdminPlansPage from '../pages/Admin/AdminPlansPage';
import AdminPaymentsPage from '../pages/Admin/AdminPaymentsPage';
import AdminInvoicesPage from '../pages/Admin/AdminInvoicesPage';
import AdminAuditLogsPage from '../pages/Admin/AdminAuditLogsPage';
import AdminHealthPage from '../pages/Admin/AdminHealthPage';

export const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Pages */}
      <Route element={<PublicLayout />}>
        <Route path="/" element={<LandingPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
      </Route>

      {/* Protected Consumer Application */}
      <Route
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/studio" element={<AIStudioPage />} />
        <Route path="/dashboard" element={<Navigate to="/studio" replace />} />
        <Route path="/ai-studio" element={<Navigate to="/studio" replace />} />
        <Route path="/usage" element={<UsagePage />} />
        <Route path="/subscription" element={<SubscriptionPage />} />
        <Route path="/invoices" element={<InvoicesPage />} />
        <Route path="/billing" element={<InvoicesPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/profile" element={<SettingsPage />} />
      </Route>

      {/* Protected Admin Console */}
      <Route
        element={
          <AdminProtectedRoute>
            <AdminLayout />
          </AdminProtectedRoute>
        }
      >
        <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
        <Route path="/admin/subscriptions" element={<AdminSubscriptionsPage />} />
        <Route path="/admin/plans" element={<AdminPlansPage />} />
        <Route path="/admin/payments" element={<AdminPaymentsPage />} />
        <Route path="/admin/invoices" element={<AdminInvoicesPage />} />
        <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
        <Route path="/admin/health" element={<AdminHealthPage />} />
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/studio" replace />} />
    </Routes>
  );
};

export default AppRoutes;
