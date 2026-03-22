import { useState } from 'react';
import { useProjects, useCreateProject, useRegenerateApiKey, useDeleteProject } from '@/hooks/useApi';
import type { Project } from '@/api/types';

export default function ProjectsPage() {
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
        <button
          onClick={() => setShowCreate(true)}
          className="px-4 py-2 bg-blue-600 text-white text-sm rounded hover:bg-blue-700"
        >
          + New Project
        </button>
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

function ProjectCard({ project, keyVisible, copied, onToggleKey, onCopy, onRegenerate, onDelete }: {
  project: Project;
  keyVisible: boolean;
  copied: boolean;
  onToggleKey: () => void;
  onCopy: () => void;
  onRegenerate: () => void;
  onDelete: () => void;
}) {
  return (
    <div className={`bg-white border rounded-lg p-4 ${!project.active ? 'opacity-50' : ''}`}>
      <div className="flex items-start justify-between">
        <div>
          <h3 className="font-semibold text-gray-900">{project.name}</h3>
          {project.description && <p className="text-sm text-gray-500 mt-0.5">{project.description}</p>}
          <p className="text-xs text-gray-400 mt-1">Created {new Date(project.createdAt).toLocaleDateString()}</p>
        </div>
        <div className="flex gap-2">
          <button onClick={onRegenerate}
            className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded hover:bg-gray-200"
            title="Regenerate API key">
            ↻ Regen Key
          </button>
          <button onClick={onDelete}
            className="text-xs px-2 py-1 bg-red-50 text-red-600 rounded hover:bg-red-100">
            Delete
          </button>
        </div>
      </div>

      <div className="mt-3 flex items-center gap-2">
        <code className="flex-1 bg-gray-50 border rounded px-3 py-1.5 text-xs font-mono text-gray-700 truncate">
          {keyVisible ? project.apiKey : '•'.repeat(32)}
        </code>
        <button onClick={onToggleKey}
          className="text-xs px-2 py-1.5 bg-gray-100 text-gray-600 rounded hover:bg-gray-200">
          {keyVisible ? 'Hide' : 'Show'}
        </button>
        <button onClick={onCopy}
          className="text-xs px-2 py-1.5 bg-blue-50 text-blue-600 rounded hover:bg-blue-100">
          {copied ? '✓ Copied' : 'Copy'}
        </button>
      </div>

      <p className="text-xs text-gray-400 mt-2">
        Add <code className="bg-gray-100 px-1 rounded">X-API-Key: {keyVisible ? project.apiKey : '***'}</code> to your reporter config
      </p>
    </div>
  );
}
