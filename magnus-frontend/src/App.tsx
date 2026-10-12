import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/MainLayout';
import { Dashboard } from '@/pages/Dashboard';
import { StudentsPage } from '@/pages/StudentsPage';
import { UploadPage } from '@/pages/UploadPage';
import { VerifyPage } from '@/pages/VerifyPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <MainLayout>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/students" element={<StudentsPage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="/verify" element={<VerifyPage />} />
          </Routes>
        </MainLayout>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
