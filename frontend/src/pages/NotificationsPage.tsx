import { useState } from 'react';
import {
  useNotificationChannels,
  useCreateNotificationChannel,
  useUpdateNotificationChannel,
  useDeleteNotificationChannel,
  useTestNotificationChannel,
} from '@/hooks/useApi';
import type { NotificationChannel, ChannelType } from '@/api/types';

const CHANNEL_META: Record<ChannelType, { icon: string; label: string; urlLabel: string; needsUrl: boolean; needsEmail: boolean }> = {
  SLACK:   { icon: '💬', label: 'Slack',    urlLabel: 'Webhook URL',  needsUrl: true,  needsEmail: false },
  TEAMS:   { icon: '🔔', label: 'Teams',    urlLabel: 'Webhook URL',  needsUrl: true,  needsEmail: false },
  WEBHOOK: { icon: '🔗', label: 'Webhook',  urlLabel: 'Endpoint URL', needsUrl: true,  needsEmail: false },
  EMAIL:   { icon: '📧', label: 'Email',    urlLabel: '',             needsUrl: false, needsEmail: true  },
};

const EMPTY_FORM = {
  name: '',
  type: 'SLACK' as ChannelType,
  webhookUrl: '',
  emailTo: '',
  enabled: true,
  notifyOnFailureOnly: false,
};

export default function NotificationsPage() {
  const { data: channels = [], isLoading } = useNotificationChannels();
  const createMutation = useCreateNotificationChannel();
  const updateMutation = useUpdateNotificationChannel();
  const deleteMutation = useDeleteNotificationChannel();
  const testMutation   = useTestNotificationChannel();

  const [showForm, setShowForm]       = useState(false);
  const [editTarget, setEditTarget]   = useState<NotificationChannel | null>(null);
  const [form, setForm]               = useState(EMPTY_FORM);
  const [testStatus, setTestStatus]   = useState<Record<number, 'idle' | 'sending' | 'ok' | 'err'>>({});

  function openCreate() {
    setEditTarget(null);
    setForm(EMPTY_FORM);
    setShowForm(true);
  }

  function openEdit(ch: NotificationChannel) {
    setEditTarget(ch);
    setForm({
      name:               ch.name,
      type:               ch.type,
      webhookUrl:         ch.webhookUrl ?? '',
      emailTo:            ch.emailTo ?? '',
      enabled:            ch.enabled,
      notifyOnFailureOnly: ch.notifyOnFailureOnly,
    });
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditTarget(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const dto = {
      ...form,
      webhookUrl: form.webhookUrl || null,
      emailTo:    form.emailTo || null,
    } as Omit<NotificationChannel, 'id' | 'createdAt'>;

    if (editTarget) {
      await updateMutation.mutateAsync({ id: editTarget.id, ...dto });
    } else {
      await createMutation.mutateAsync(dto);
    }
    closeForm();
  }

  async function handleTest(id: number) {
    setTestStatus(s => ({ ...s, [id]: 'sending' }));
    try {
      await testMutation.mutateAsync(id);
      setTestStatus(s => ({ ...s, [id]: 'ok' }));
    } catch {
      setTestStatus(s => ({ ...s, [id]: 'err' }));
    }
    setTimeout(() => setTestStatus(s => ({ ...s, [id]: 'idle' })), 3000);
  }

  async function toggleEnabled(ch: NotificationChannel) {
    await updateMutation.mutateAsync({ ...ch, enabled: !ch.enabled });
  }

  const meta = CHANNEL_META[form.type];
  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <div className="p-6 max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-xl font-semibold text-gray-900">Notifications</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            Get notified on Slack, Teams, Email, or any webhook when a run completes.
          </p>
        </div>
        <button
          onClick={openCreate}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
        >
          + Add Channel
        </button>
      </div>

      {/* Channel list */}
      {isLoading ? (
        <p className="text-sm text-gray-500">Loading…</p>
      ) : channels.length === 0 ? (
        <div className="border-2 border-dashed border-gray-200 rounded-xl p-12 text-center">
          <p className="text-3xl mb-2">🔕</p>
          <p className="text-gray-500 text-sm">No notification channels configured yet.</p>
          <button
            onClick={openCreate}
            className="mt-4 px-4 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700"
          >
            Add your first channel
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {channels.map(ch => {
            const m = CHANNEL_META[ch.type];
            const ts = testStatus[ch.id] ?? 'idle';
            return (
              <div
                key={ch.id}
                className="flex items-center gap-4 bg-white border border-gray-200 rounded-xl px-5 py-4 shadow-sm"
              >
                <span className="text-2xl">{m.icon}</span>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="font-medium text-gray-900 text-sm">{ch.name}</span>
                    <span className="text-xs text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">{m.label}</span>
                    {ch.notifyOnFailureOnly && (
                      <span className="text-xs text-red-600 bg-red-50 px-2 py-0.5 rounded-full">Failures only</span>
                    )}
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5 truncate">
                    {ch.webhookUrl ?? ch.emailTo ?? '—'}
                  </p>
                </div>

                {/* Toggle */}
                <button
                  onClick={() => toggleEnabled(ch)}
                  className={`relative inline-flex h-5 w-9 items-center rounded-full transition-colors ${
                    ch.enabled ? 'bg-blue-600' : 'bg-gray-300'
                  }`}
                  title={ch.enabled ? 'Disable' : 'Enable'}
                >
                  <span
                    className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform ${
                      ch.enabled ? 'translate-x-4' : 'translate-x-0.5'
                    }`}
                  />
                </button>

                {/* Test */}
                <button
                  onClick={() => handleTest(ch.id)}
                  disabled={ts === 'sending'}
                  className={`text-xs px-3 py-1.5 rounded-lg border transition-colors ${
                    ts === 'ok'  ? 'border-green-400 text-green-600 bg-green-50' :
                    ts === 'err' ? 'border-red-400 text-red-600 bg-red-50' :
                    'border-gray-300 text-gray-600 hover:bg-gray-50'
                  }`}
                >
                  {ts === 'sending' ? '…' : ts === 'ok' ? '✓ Sent' : ts === 'err' ? '✗ Failed' : 'Test'}
                </button>

                {/* Edit */}
                <button
                  onClick={() => openEdit(ch)}
                  className="text-xs px-3 py-1.5 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50"
                >
                  Edit
                </button>

                {/* Delete */}
                <button
                  onClick={() => deleteMutation.mutate(ch.id)}
                  className="text-xs px-3 py-1.5 rounded-lg border border-red-200 text-red-500 hover:bg-red-50"
                >
                  Delete
                </button>
              </div>
            );
          })}
        </div>
      )}

      {/* Inline info for property-based config */}
      <div className="mt-8 rounded-xl bg-gray-50 border border-gray-200 p-4 text-xs text-gray-500">
        <p className="font-medium text-gray-700 mb-1">Property-based config</p>
        <p>
          Channels added here are stored in the database. You can also configure Slack, Teams, Email,
          and Jenkins webhooks statically via <code className="bg-gray-200 px-1 rounded">application.properties</code> —
          those fire alongside the channels above.
        </p>
      </div>

      {/* Add / Edit modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6">
            <h2 className="text-base font-semibold text-gray-900 mb-4">
              {editTarget ? 'Edit Channel' : 'Add Notification Channel'}
            </h2>

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Name */}
              <div>
                <label className="block text-sm text-gray-700 mb-1">Name</label>
                <input
                  required
                  value={form.name}
                  onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
                  placeholder="e.g. QA Slack Channel"
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>

              {/* Type */}
              <div>
                <label className="block text-sm text-gray-700 mb-1">Type</label>
                <select
                  value={form.type}
                  onChange={e => setForm(f => ({ ...f, type: e.target.value as ChannelType }))}
                  className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {(Object.keys(CHANNEL_META) as ChannelType[]).map(t => (
                    <option key={t} value={t}>
                      {CHANNEL_META[t].icon} {CHANNEL_META[t].label}
                    </option>
                  ))}
                </select>
              </div>

              {/* URL or Email */}
              {meta.needsUrl && (
                <div>
                  <label className="block text-sm text-gray-700 mb-1">{meta.urlLabel}</label>
                  <input
                    required
                    value={form.webhookUrl}
                    onChange={e => setForm(f => ({ ...f, webhookUrl: e.target.value }))}
                    placeholder="https://hooks.slack.com/services/..."
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              )}
              {meta.needsEmail && (
                <div>
                  <label className="block text-sm text-gray-700 mb-1">Recipients</label>
                  <input
                    required
                    value={form.emailTo}
                    onChange={e => setForm(f => ({ ...f, emailTo: e.target.value }))}
                    placeholder="team@example.com, qa@example.com"
                    className="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                  <p className="text-xs text-gray-400 mt-1">Comma-separated. Requires SMTP configured in application.properties.</p>
                </div>
              )}

              {/* Options */}
              <div className="flex items-center justify-between">
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.notifyOnFailureOnly}
                    onChange={e => setForm(f => ({ ...f, notifyOnFailureOnly: e.target.checked }))}
                    className="rounded"
                  />
                  Notify on failures only
                </label>
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.enabled}
                    onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))}
                    className="rounded"
                  />
                  Enabled
                </label>
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={closeForm}
                  className="px-4 py-2 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isPending}
                  className="px-4 py-2 text-sm bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                >
                  {isPending ? 'Saving…' : editTarget ? 'Save Changes' : 'Add Channel'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
