import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { WebSocketProvider } from '@/contexts/WebSocketContext';
import AppLayout from './components/layout/AppLayout';
import DashboardPage from './pages/DashboardPage';
import RunsPage from './pages/RunsPage';
import RunDetailPage from './pages/RunDetailPage';
import ComparisonPage from './pages/ComparisonPage';
import SearchPage from './pages/SearchPage';
import FlakinessPage from './pages/FlakinessPage';
import TrendsPage from './pages/TrendsPage';

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
      <WebSocketProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/runs" element={<RunsPage />} />
              <Route path="/runs/:runId" element={<RunDetailPage />} />
              <Route path="/runs/:runId/compare/:compareRunId" element={<ComparisonPage />} />
              <Route path="/search" element={<SearchPage />} />
              <Route path="/flakiness" element={<FlakinessPage />} />
              <Route path="/trends" element={<TrendsPage />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </WebSocketProvider>
    </QueryClientProvider>
  );
}

export default App;
