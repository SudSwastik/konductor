export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <main style={{ minHeight: '100vh', maxWidth: 1280, margin: '0 auto', padding: '34px 40px 72px' }}>
      {children}
    </main>
  );
}
