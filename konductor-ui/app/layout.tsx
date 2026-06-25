import type { Metadata } from 'next';
import Sidebar from '@/components/Sidebar/Sidebar';
import '@/styles/globals.scss';

export const metadata: Metadata = {
  title: 'Konductor Admin',
  description: 'Publisher-side event routing admin',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Sidebar />
        <main style={{ marginLeft: '220px', padding: '32px', minHeight: '100vh' }}>
          {children}
        </main>
      </body>
    </html>
  );
}
