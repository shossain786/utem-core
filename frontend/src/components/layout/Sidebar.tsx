import { NavLink } from 'react-router-dom';

const navItems = [
  { to: '/', label: 'Dashboard', icon: '📊' },
  { to: '/runs', label: 'Test Runs', icon: '🧪' },
  { to: '/search', label: 'Search', icon: '🔍' },
  { to: '/flakiness', label: 'Flakiness', icon: '⚡' },
  { to: '/trends', label: 'Trends', icon: '📈' },
];

export default function Sidebar() {
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
      </nav>

      <div className="p-4 border-t border-sidebar-hover text-xs text-gray-500">
        v0.1.0 MVP
      </div>
    </aside>
  );
}
