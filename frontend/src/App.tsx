import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WebSocketProvider } from '@/contexts/WebSocketContext';
import { AuthProvider } from '@/contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import AppLayout from './components/layout/AppLayout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import RunsPage from './pages/RunsPage';
import RunDetailPage from './pages/RunDetailPage';
import ComparisonPage from './pages/ComparisonPage';
import SearchPage from './pages/SearchPage';
import FlakinessPage from './pages/FlakinessPage';
import TrendsPage from './pages/TrendsPage';
import InsightsPage from './pages/InsightsPage';
import FailureClustersPage from './pages/FailureClustersPage';
import PerformancePage from './pages/PerformancePage';
import ArchivedPage from './pages/ArchivedPage';
import JobsPage from './pages/JobsPage';
import JobRunsPage from './pages/JobRunsPage';
import NotificationsPage from './pages/NotificationsPage';
import ProjectsPage from './pages/ProjectsPage';
import UsersPage from './pages/UsersPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <WebSocketProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<ProtectedRoute />}>
                <Route element={<AppLayout />}>
                  <Route path="/" element={<DashboardPage />} />
                  <Route path="/runs" element={<RunsPage />} />
                  <Route path="/runs/:runId" element={<RunDetailPage />} />
                  <Route path="/runs/:runId/compare/:compareRunId" element={<ComparisonPage />} />
                  <Route path="/search" element={<SearchPage />} />
                  <Route path="/flakiness" element={<FlakinessPage />} />
                  <Route path="/trends" element={<TrendsPage />} />
                  <Route path="/insights" element={<InsightsPage />} />
                  <Route path="/failures" element={<FailureClustersPage />} />
                  <Route path="/performance" element={<PerformancePage />} />
                  <Route path="/archive" element={<ArchivedPage />} />
                  <Route path="/jobs" element={<JobsPage />} />
                  <Route path="/jobs/:jobName" element={<JobRunsPage />} />
                  <Route path="/notifications" element={<NotificationsPage />} />
                  <Route path="/projects" element={<ProjectsPage />} />
                  <Route path="/users" element={<UsersPage />} />
                </Route>
              </Route>
            </Routes>
          </BrowserRouter>
        </WebSocketProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

export default App;
