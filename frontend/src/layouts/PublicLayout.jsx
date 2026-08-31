import React from 'react';
import { Outlet } from 'react-router-dom';
import Navbar from '../components/navbar/Navbar';

export const PublicLayout = () => {
  return (
    <div className="min-h-screen flex flex-col bg-[#0b0f19] text-slate-100">
      <Navbar />
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-slate-800/80 bg-[#090d16] py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4 text-xs text-slate-400">
          <p>© {new Date().getFullYear()} Nexus AI Platform. Consumer AI SaaS Architecture.</p>
          <div className="flex space-x-6">
            <a href="#privacy" className="hover:text-slate-200 transition-colors">Privacy Policy</a>
            <a href="#terms" className="hover:text-slate-200 transition-colors">Terms of Service</a>
            <a href="#security" className="hover:text-slate-200 transition-colors">Security Architecture</a>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default PublicLayout;
