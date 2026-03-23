import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { useUsers, useCreateUser, useDeactivateUser, useReactivateUser, useResetPassword } from '@/hooks/useApi';

export default function UsersPage() {
  const { isSuperAdmin } = useAuth();
  const { data: users, isLoading } = useUsers(isSuperAdmin);
  const createUser = useCreateUser();
  const deactivateUser = useDeactivateUser();
  const reactivateUser = useReactivateUser();
  const resetPassword = useResetPassword();

  const [showCreate, setShowCreate] = useState(false);
  const [form, setForm] = useState({ username: '', email: '', password: '', role: 'MEMBER' });
  const [resetTarget, setResetTarget] = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [createError, setCreateError] = useState<string | null>(null);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setCreateError(null);
    try {
      await createUser.mutateAsync(form);
      setShowCreate(false);
      setForm({ username: '', email: '', password: '', role: 'MEMBER' });
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      setCreateError(msg || 'Failed to create user');
    }
  }

  async function handleResetPassword(e: React.FormEvent) {
    e.preventDefault();
    if (!resetTarget) return;
    await resetPassword.mutateAsync({ userId: resetTarget, newPassword });
    setResetTarget(null);
    setNewPassword('');
  }

  if (!isSuperAdmin) return <Navigate to="/" replace />;
  if (isLoading) return <div className="p-6 text-gray-400">Loading…</div>;

  return (
    <div className="p-6 max-w-5xl">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-white">Users</h1>
        <button
          onClick={() => setShowCreate(true)}
          className="bg-blue-600 hover:bg-blue-700 text-white text-sm px-4 py-2 rounded"
        >
          + New User
        </button>
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="bg-gray-800 rounded-lg p-4 mb-6 space-y-3">
          <h2 className="text-sm font-medium text-white">Create User</h2>
          <div className="grid grid-cols-2 gap-3">
            <input
              placeholder="Username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white text-sm"
              required
            />
            <input
              placeholder="Email (optional)"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white text-sm"
            />
            <input
              type="password"
              placeholder="Password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white text-sm"
              required
            />
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value })}
              className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white text-sm"
            >
              <option value="MEMBER">Member</option>
              <option value="SUPER_ADMIN">Super Admin</option>
            </select>
          </div>
          {createError && <p className="text-red-400 text-xs">{createError}</p>}
          <div className="flex gap-2">
            <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white text-sm px-4 py-1.5 rounded">
              Create
            </button>
            <button type="button" onClick={() => setShowCreate(false)}
              className="text-gray-400 hover:text-white text-sm px-4 py-1.5">
              Cancel
            </button>
          </div>
        </form>
      )}

      {resetTarget && (
        <form onSubmit={handleResetPassword} className="bg-gray-800 rounded-lg p-4 mb-6 space-y-3">
          <h2 className="text-sm font-medium text-white">Reset Password</h2>
          <input
            type="password"
            placeholder="New password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            className="bg-gray-700 border border-gray-600 rounded px-3 py-2 text-white text-sm w-full"
            required
          />
          <div className="flex gap-2">
            <button type="submit" className="bg-amber-600 hover:bg-amber-700 text-white text-sm px-4 py-1.5 rounded">
              Reset
            </button>
            <button type="button" onClick={() => setResetTarget(null)}
              className="text-gray-400 hover:text-white text-sm px-4 py-1.5">
              Cancel
            </button>
          </div>
        </form>
      )}

      <div className="bg-gray-800 rounded-lg overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-700 text-gray-300">
            <tr>
              <th className="px-4 py-3 text-left font-medium">Username</th>
              <th className="px-4 py-3 text-left font-medium">Email</th>
              <th className="px-4 py-3 text-left font-medium">Role</th>
              <th className="px-4 py-3 text-left font-medium">Status</th>
              <th className="px-4 py-3 text-left font-medium">Projects</th>
              <th className="px-4 py-3 text-left font-medium">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-700">
            {users?.map((user) => (
              <tr key={user.id} className="text-gray-300">
                <td className="px-4 py-3 font-medium text-white">{user.username}</td>
                <td className="px-4 py-3 text-gray-400">{user.email || '—'}</td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${
                    user.role === 'SUPER_ADMIN' ? 'bg-purple-900 text-purple-300' : 'bg-blue-900 text-blue-300'
                  }`}>
                    {user.role === 'SUPER_ADMIN' ? 'Super Admin' : 'Member'}
                  </span>
                </td>
                <td className="px-4 py-3">
                  <span className={`text-xs px-2 py-0.5 rounded-full ${
                    user.active ? 'bg-green-900 text-green-300' : 'bg-gray-700 text-gray-400'
                  }`}>
                    {user.active ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-4 py-3 text-gray-400">
                  {user.projectIds == null ? 'All' : user.projectIds.length}
                </td>
                <td className="px-4 py-3">
                  <div className="flex gap-2">
                    <button
                      onClick={() => setResetTarget(user.id)}
                      className="text-xs text-amber-400 hover:text-amber-300"
                    >
                      Reset pwd
                    </button>
                    {user.active ? (
                      <button
                        onClick={() => deactivateUser.mutate(user.id)}
                        className="text-xs text-red-400 hover:text-red-300"
                      >
                        Deactivate
                      </button>
                    ) : (
                      <button
                        onClick={() => reactivateUser.mutate(user.id)}
                        className="text-xs text-green-400 hover:text-green-300"
                      >
                        Reactivate
                      </button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
            {!users?.length && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-500">No users yet</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
