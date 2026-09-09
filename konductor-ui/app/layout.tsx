import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Subscriptions | Konductor",
  description: "Create and manage event subscriptions in Konductor.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
