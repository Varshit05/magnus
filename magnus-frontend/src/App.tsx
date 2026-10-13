
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MainLayout } from '@/components/layout/MainLayout';
import { Dashboard } from '@/pages/Dashboard';
import { StudentsPage } from '@/pages/StudentsPage';
import { UploadPage } from '@/pages/UploadPage';
import { VerifyPage } from '@/pages/VerifyPage';
import { LoginPage } from '@/pages/LoginPage';
import { ForbiddenPage } from '@/pages/ForbiddenPage';
import { AuthProvider } from '@/context/AuthContext';
import { ProtectedRoute } from '@/components/ProtectedRoute';

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
    <AuthProvider>
      <QueryClientProvider client={queryClient}>
        <Router>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/403" element={<ForbiddenPage />} />
            <Route element={<ProtectedRoute />}>
              <Route
                path="/"
                element={
                  <MainLayout>
                    <Dashboard />
                  </MainLayout>
                }
              />
              <Route
                path="/students"
                element={
                  <MainLayout>
                    <StudentsPage />
                  </MainLayout>
                }
              />
              <Route
                path="/upload"
                element={
                  <MainLayout>
                    <UploadPage />
                  </MainLayout>
                }
              />
              <Route
                path="/verify"
                element={
                  <MainLayout>
                    <VerifyPage />
                  </MainLayout>
                }
              />
            </Route>
          </Routes>
        </Router>
      </QueryClientProvider>
    </AuthProvider>
  );
}

export default App;
