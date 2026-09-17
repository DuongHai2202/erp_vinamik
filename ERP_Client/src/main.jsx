import { lazy, StrictMode, Suspense } from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ConfigProvider, theme as antd_theme } from 'antd';
import viVN from 'antd/locale/vi_VN';
import { SystemStatusProvider } from './platform/common/system_status_context';
import { AuthProvider } from './platform/identity/auth_context';
import AppShell from './platform/layout/app_shell';
import NotFoundPage from './platform/layout/not_found_page';
import ProtectedRoute from './platform/layout/protected_route';
import OverviewPage from './platform/layout/overview_page';
import { ThemeProvider, use_theme } from './platform/layout/theme_context';
const LoginPage = lazy(() => import('./platform/identity/login_page'));
const RegistrationPage = lazy(() => import('./platform/identity/registration_page'));
const RegistrationRequestsPage = lazy(() => import('./platform/identity/registration_requests_page'));
const UsersPage = lazy(() => import('./platform/identity/users_page'));
const HumanResourcesPage = lazy(() => import('./modules/human_resources/human_resources_page'));
const ContractsPage = lazy(() => import('./modules/human_resources/contracts_page'));
const InventoryPage = lazy(() => import('./modules/inventory/inventory_page'));
const ProductionPage = lazy(() => import('./modules/production/production_page'));
const ModuleDataPage = lazy(() => import('./platform/layout/module_data_page'));
import './styles/global.css';

function ApplicationRoutes() {
  return <SystemStatusProvider><AuthProvider>
    <Suspense fallback={<div className="full_page_center"><span className="loading_orbit" />Đang tải giao diện...</div>}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegistrationPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route index element={<OverviewPage />} />
            <Route path="human_resources" element={<HumanResourcesPage />} />
            <Route path="human_resources/contracts" element={<ContractsPage />} />
            <Route path="human_resources/absences" element={<ModuleDataPage feature_key="absences" />} />
            <Route path="human_resources/rewards_discipline" element={<ModuleDataPage feature_key="rewards_discipline" />} />
            <Route path="human_resources/payroll" element={<ModuleDataPage feature_key="payroll" />} />
            <Route path="inventory" element={<InventoryPage />} />
            <Route path="inventory/receipts" element={<ModuleDataPage feature_key="receipts" />} />
            <Route path="inventory/issues" element={<ModuleDataPage feature_key="issues" />} />
            <Route path="inventory/stocktakes" element={<ModuleDataPage feature_key="stocktakes" />} />
            <Route path="inventory/transfers" element={<ModuleDataPage feature_key="transfers" />} />
            <Route path="production" element={<ProductionPage />} />
            <Route path="production/materials" element={<ModuleDataPage feature_key="materials" />} />
            <Route path="production/orders" element={<ModuleDataPage feature_key="orders" />} />
            <Route path="production/assignments" element={<ModuleDataPage feature_key="assignments" />} />
            <Route path="production/finished_products" element={<ModuleDataPage feature_key="finished_products" />} />
            <Route path="settings/users" element={<UsersPage />} />
            <Route path="settings/registration_requests" element={<RegistrationRequestsPage />} />
            <Route path="*" element={<NotFoundPage />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  </AuthProvider></SystemStatusProvider>;
}

function ThemedApplication() {
  const { preferences, theme_tokens } = use_theme();
  const config_theme = {
    ...theme_tokens,
    algorithm: preferences.appearance === 'dark' ? antd_theme.darkAlgorithm : antd_theme.defaultAlgorithm,
  };

  return <ConfigProvider locale={viVN} theme={config_theme}>
    <ApplicationRoutes />
  </ConfigProvider>;
}

function App() {
  return <ThemeProvider><ThemedApplication /></ThemeProvider>;
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <StrictMode><BrowserRouter><App /></BrowserRouter></StrictMode>,
);
