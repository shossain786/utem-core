import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { useProjects, useCreateProject, useRegenerateApiKey, useDeleteProject,
         useProjectMembers, useAddProjectMember, useRemoveProjectMember, useUsers } from '@/hooks/useApi';
import type { Project, ProjectMemberDTO } from '@/api/types';
import type { MemberRole } from '@/api/types';

export default function ProjectsPage() {
  const { isSuperAdmin } = useAuth();
  const { data: projects, isLoading } = useProjects();
  const createProject = useCreateProject();
  const regenerateKey = useRegenerateApiKey();
  const deleteProject = useDeleteProject();

  const [showCreate, setShowCreate] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [visibleKeys, setVisibleKeys] = useState<Set<string>>(new Set());
  const [copied, setCopied] = useState<string | null>(null);

  function toggleKeyVisibility(id: string) {
    setVisibleKeys(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  async function copyKey(key: string, id: string) {
    await navigator.clipboard.writeText(key);
    setCopied(id);
    setTimeout(() => setCopied(null), 2000);
  }

  function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    createProject.mutate({ name: name.trim(), description: description.trim() || undefined }, {
      onSuccess: () => { setName(''); setDescription(''); setShowCreate(false); }
    });
  }

  return (
    <div className="p-6 max-w-4xl">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Projects</h1>
          <p className="text-sm text-gray-500 mt-1">
            Each project gets its own API key. Enable security via <code className="bg-gray-100 px-1 rounded">utem.security.enabled=true</code>.
          </p>
        </div>
        {isSuperAdmin && (
          <button
            onClick={() => setShowCreate(true)}
            className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
          >
            + New Project
          </button>
        )}
      </div>

      {showCreate && (
        <form onSubmit={handleCreate} className="mb-6 bg-white border rounded-lg p-4 space-y-3">
          <h2 className="font-semibold text-gray-800">Create Project</h2>
          <input
            type="text"
            placeholder="Project name *"
            value={name}
            onChange={e => setName(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
            required
          />
          <input
            type="text"
            placeholder="Description (optional)"
            value={description}
            onChange={e => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <div className="flex gap-2">
            <button type="submit" disabled={createProject.isPending}
              className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700 disabled:opacity-50">
              Create
            </button>
            <button type="button" onClick={() => setShowCreate(false)}
              className="px-4 py-2 bg-gray-100 text-gray-700 text-sm rounded hover:bg-gray-200">
              Cancel
            </button>
          </div>

        </form>
      )}

      {isLoading && <p className="text-gray-500">Loading…</p>}

      {!isLoading && projects?.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <p className="text-lg">No projects yet</p>
          <p className="text-sm mt-1">Create a project to get an API key for your reporters.</p>
        </div>
      )}

      <div className="space-y-3">
        {projects?.map(project => (
          <ProjectCard
            key={project.id}
            project={project}
            isSuperAdmin={isSuperAdmin}
            keyVisible={visibleKeys.has(project.id)}
            copied={copied === project.id}
            onToggleKey={() => toggleKeyVisibility(project.id)}
            onCopy={() => copyKey(project.apiKey, project.id)}
            onRegenerate={() => regenerateKey.mutate(project.id)}
            onDelete={() => { if (confirm(`Delete project "${project.name}"?`)) deleteProject.mutate(project.id); }}
          />
        ))}
      </div>
    </div>
  );
}

function ProjectCard({ project, isSuperAdmin, keyVisible, copied, onToggleKey, onCopy, onRegenerate, onDelete }: {
  project: Project;
  isSuperAdmin: boolean;
  keyVisible: boolean;
  copied: boolean;
  onToggleKey: () => void;
  onCopy: () => void;
  onRegenerate: () => void;
  onDelete: () => void;
}) {
  const [membersOpen, setMembersOpen] = useState(false);

  return (
    <div className={`bg-white border rounded-lg p-4 ${!project.active ? 'opacity-50' : ''}`}>
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900">{project.name}</h3>
          {project.description && <p className="text-sm text-gray-500 mt-0.5">{project.description}</p>}
          <p className="text-xs text-gray-400 mt-1">Created {new Date(project.createdAt).toLocaleDateString()}</p>
        </div>
        {isSuperAdmin && (
          <div className="flex gap-2">
            <button type="button" onClick={() => setMembersOpen(o => !o)}
              className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded hover:bg-gray-200">
              Members {membersOpen ? '▲' : '▼'}
            </button>
            <button type="button" onClick={onRegenerate}
              className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
              title="Regenerate API key">
              ↻ Regen Key
            </button>
            <button type="button" onClick={onDelete}
              className="text-xs px-2 py-1 bg-red-50 text-red-600 rounded hover:bg-red-100">
              Delete
            </button>
          </div>
        )}
      </div>

      <div className="mt-3 flex items-center gap-2">
        <code className="flex-1 bg-gray-50 border rounded px-3 py-1.5 text-xs font-mono text-gray-700 truncate">
          {keyVisible ? project.apiKey : '•'.repeat(32)}
        </code>
        <button type="button" onClick={onToggleKey}
          className="text-xs px-2 py-1.5 bg-gray-100 text-gray-600 rounded hover:bg-gray-200">
          {keyVisible ? 'Hide' : 'Show'}
        </button>
        <button type="button" onClick={onCopy}
          className="text-xs px-2 py-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100">
          {copied ? '✓ Copied' : 'Copy'}
        </button>
      </div>
      <p className="text-xs text-gray-400 mt-2">
        Add <code className="bg-gray-100 px-1 rounded">X-API-Key: {keyVisible ? project.apiKey : '***'}</code> to your reporter config
      </p>

      {isSuperAdmin && membersOpen && <MembersSection projectId={project.id} />}
    </div>
  );
}

function MembersSection({ projectId }: { projectId: string }) {
  const { data: members, isLoading } = useProjectMembers(projectId);
  const { data: allUsers } = useUsers();
  const addMember = useAddProjectMember();
  const removeMember = useRemoveProjectMember();

  const [selectedUserId, setSelectedUserId] = useState('');
  const [selectedRole, setSelectedRole] = useState<MemberRole>('VIEWER');

  const memberUserIds = new Set(members?.map(m => m.userId) ?? []);
  const availableUsers = allUsers?.filter(u => u.active && !memberUserIds.has(u.id)) ?? [];

  function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedUserId) return;
    addMember.mutate({ projectId, userId: selectedUserId, role: selectedRole }, {
      onSuccess: () => setSelectedUserId(''),
    });
  }

  return (
    <div className="mt-4 border-t pt-3">
      <h4 className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Members</h4>

      {isLoading && <p className="text-xs text-gray-400">Loading…</p>}

      {!isLoading && members?.length === 0 && (
        <p className="text-xs text-gray-400 mb-2">No members yet. Add users below.</p>
      )}

      {members && members.length > 0 && (
        <div className="space-y-1 mb-3">
          {members.map(member => (
            <MemberRow
              key={member.userId}
              member={member}
              onRemove={() => removeMember.mutate({ projectId, userId: member.userId })}
            />
          ))}
        </div>
      )}

      <form onSubmit={handleAdd} className="flex gap-2 items-center">
        <select
          value={selectedUserId}
          onChange={e => setSelectedUserId(e.target.value)}
          title="Select user to add"
          className="flex-1 border rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-400"
        >
          <option value="">Add user…</option>
          {availableUsers.map(u => (
            <option key={u.id} value={u.id}>{u.username}</option>
          ))}
        </select>
        <select
          value={selectedRole}
          onChange={e => setSelectedRole(e.target.value as MemberRole)}
          title="Select member role"
          className="border rounded px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-400"
        >
          <option value="VIEWER">Viewer</option>
          <option value="ADMIN">Admin</option>
        </select>
        <button type="submit" disabled={!selectedUserId || addMember.isPending}
          className="text-xs px-2 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
          Add
        </button>
      </form>
    </div>
  );
}

function MemberRow({ member, onRemove }: { member: ProjectMemberDTO; onRemove: () => void }) {
  return (
    <div className="flex items-center justify-between bg-gray-50 rounded px-2 py-1">
      <div className="flex items-center gap-2">
        <span className="text-xs text-gray-700">{member.username}</span>
        <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${
          member.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-gray-200 text-gray-600'
        }`}>
          {member.role}
        </span>
      </div>
      <button type="button" onClick={onRemove}
        className="text-xs text-red-500 hover:text-red-700">
        Remove
      </button>
    </div>
  );
}
