import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "NBA2K Assistant",
  description: "Live matchup tool for NBA 2K sessions",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <header className="site-header">
          <div className="site-header-inner">
            <Link href="/" className="site-wordmark">
              NBA<span>2K</span> Assistant
            </Link>
            <nav className="site-nav">
              <Link href="/">Team Builder</Link>
              <Link href="/session">Live Session</Link>
            </nav>
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
