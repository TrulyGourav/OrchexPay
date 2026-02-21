import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import PublicLayout from './layouts/PublicLayout';
import DashboardLayout from './layouts/DashboardLayout';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Unauthorized from './pages/Unauthorized';
import AdminDashboard from './pages/admin/Dashboard';
import MerchantList from './pages/admin/MerchantList';
import WalletSearch from './pages/admin/WalletSearch';
import TransactionExplorer from './pages/admin/TransactionExplorer';
import FreezeWallet from './pages/admin/FreezeWallet';
import SettlementReport from './pages/admin/SettlementReport';
import MerchantDashboard from './pages/merchant/MerchantDashboard';
import VendorManagement from './pages/merchant/VendorManagement';
import VendorWallets from './pages/merchant/VendorWallets';
import OrderSettlement from './pages/merchant/OrderSettlement';
import CommissionEarnings from './pages/merchant/CommissionEarnings';
import EscrowBalance from './pages/merchant/EscrowBalance';
import MerchantTransactions from './pages/merchant/MerchantTransactions';
import OrderCompletion from './pages/merchant/OrderCompletion';
import PaymentRecords from './pages/merchant/PaymentRecords';
import VendorDashboard from './pages/vendor/VendorDashboard';
import VendorTransactions from './pages/vendor/VendorTransactions';
import RequestPayout from './pages/vendor/RequestPayout';
import PayoutStatus from './pages/vendor/PayoutStatus';
import BankDetails from './pages/vendor/BankDetails';
function PrivateRoute({ children, allowedRoles }) {
  const { token, user } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  if (!user) return null;
  const hasRole = allowedRoles.some((r) => user.roles && user.roles.includes(r));
  if (!hasRole) return <Navigate to="/unauthorized" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route element={<PublicLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
      </Route>
      <Route path="/unauthorized" element={<Unauthorized />} />

      <Route
        path="/admin"
        element={
          <PrivateRoute allowedRoles={['ADMIN']}>
            <DashboardLayout role="ADMIN" />
          </PrivateRoute>
        }
      >
        <Route index element={<AdminDashboard />} />
        <Route path="merchants" element={<MerchantList />} />
        <Route path="wallets" element={<WalletSearch />} />
        <Route path="transactions" element={<TransactionExplorer />} />
        <Route path="freeze" element={<FreezeWallet />} />
        <Route path="settlement" element={<SettlementReport />} />
      </Route>

      <Route
        path="/merchant"
        element={
          <PrivateRoute allowedRoles={['MERCHANT']}>
            <DashboardLayout role="MERCHANT" />
          </PrivateRoute>
        }
      >
        <Route index element={<MerchantDashboard />} />
        <Route path="vendors" element={<VendorManagement />} />
        <Route path="vendor-wallets" element={<VendorWallets />} />
        <Route path="order-settlement" element={<OrderSettlement />} />
        <Route path="commission" element={<CommissionEarnings />} />
        <Route path="escrow" element={<EscrowBalance />} />
        <Route path="transactions" element={<MerchantTransactions />} />
        <Route path="order-complete" element={<OrderCompletion />} />
        <Route path="payments" element={<PaymentRecords />} />
      </Route>

      <Route
        path="/vendor"
        element={
          <PrivateRoute allowedRoles={['VENDOR']}>
            <DashboardLayout role="VENDOR" />
          </PrivateRoute>
        }
      >
        <Route index element={<VendorDashboard />} />
        <Route path="transactions" element={<VendorTransactions />} />
        <Route path="payout" element={<RequestPayout />} />
        <Route path="payout-status" element={<PayoutStatus />} />
        <Route path="bank" element={<BankDetails />} />
      </Route>

      <Route path="/" element={<Navigate to="/login" replace />} />
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
