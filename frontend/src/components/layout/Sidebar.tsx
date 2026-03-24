import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { useArchiveRuns } from '@/hooks/useApi';
import { useAuth } from '@/contexts/AuthContext';
import { useTheme } from '@/contexts/ThemeContext';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '📊' },
  { to: '/runs', label: 'Test Runs', icon: '🧪' },
  { to: '/jobs', label: 'Jobs', icon: '💼' },
  { to: '/search', label: 'Search', icon: '🔍' },
  { to: '/flakiness', label: 'Flakiness', icon: '⚡' },
  { to: '/trends', label: 'Trends', icon: '📈' },
  { to: '/insights', label: 'Insights', icon: '💡' },
  { to: '/failures', label: 'Failures', icon: '🔥' },
  { to: '/performance', label: 'Performance', icon: '⏱️' },
  { to: '/notifications', label: 'Notifications', icon: '🔔' },
  { to: '/projects', label: 'Projects', icon: '🗂️' },
];

export default function Sidebar() {
  const [isDragOver, setIsDragOver] = useState(false);
  const archiveMutation = useArchiveRuns();
  const { username, role, isSuperAdmin, isAuthenticated, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  function handleDragOver(e: React.DragEvent) {
    e.preventDefault();
    setIsDragOver(true);
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    const runId = e.dataTransfer.getData('runId');
    if (runId) {
      archiveMutation.mutate([runId]);
    }
    setIsDragOver(false);
  }

  return (
    <aside className="w-56 bg-sidebar text-white flex flex-col min-h-screen">
      <div className="p-4 border-b border-sidebar-hover">
        <h1 className="text-lg font-bold tracking-wide">UTEM</h1>
        <p className="text-xs text-gray-400 mt-0.5">Test Reporting Engine</p>
      </div>

      <nav className="flex-1 py-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 text-sm transition-colors ${
                isActive
                  ? 'bg-sidebar-active text-white border-l-2 border-blue-400'
                  : 'text-gray-300 hover:bg-sidebar-hover hover:text-white border-l-2 border-transparent'
              }`
            }
          >
            <span>{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}

        {isSuperAdmin && (
          <NavLink
            to="/users"
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 text-sm transition-colors ${
                isActive
                  ? 'bg-sidebar-active text-white border-l-2 border-blue-400'
                  : 'text-gray-300 hover:bg-sidebar-hover hover:text-white border-l-2 border-transparent'
              }`
            }
          >
            <span>👥</span>
            <span>Users</span>
          </NavLink>
        )}

        {/* Archive — also a drag-drop target */}
        <div
          onDragOver={handleDragOver}
          onDragLeave={() => setIsDragOver(false)}
          onDrop={handleDrop}
          className={`transition-colors rounded-sm mx-1 ${isDragOver ? 'ring-2 ring-amber-400 bg-amber-900/30' : ''}`}
        >
          <NavLink
            to="/archive"
            className={({ isActive }) =>
              `flex items-center gap-3 px-4 py-2.5 text-sm transition-colors ${
                isActive
                  ? 'bg-sidebar-active text-white border-l-2 border-blue-400'
                  : 'text-gray-300 hover:bg-sidebar-hover hover:text-white border-l-2 border-transparent'
              }`
            }
          >
            <span>🗄️</span>
            <span>Archive</span>
          </NavLink>
        </div>
      </nav>

      <div className="p-4 border-t border-sidebar-hover text-xs text-gray-500">
        {isAuthenticated ? (
          <div className="space-y-1">
            <div className="text-gray-300 font-medium truncate">{username}</div>
            <div className="text-gray-500">{role === 'SUPER_ADMIN' ? 'Super Admin' : 'Member'}</div>
            <button
              type="button"
              onClick={logout}
              className="mt-1 text-gray-400 hover:text-white transition-colors"
            >
              Sign out
            </button>
          </div>
        ) : (
          <span>v0.9.1 · UTEM</span>
        )}
        <button
          type="button"
          onClick={toggleTheme}
          className="flex items-center gap-2 text-xs text-gray-400 hover:text-white transition-colors py-1 mt-2"
          title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
        >
          <span>{theme === 'dark' ? '☀️' : '🌙'}</span>
          <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>
        </button>
      </div>
    </aside>
  );
}
