import type { Metadata } from "next";
import { Geist, Geist_Mono, Inter } from "next/font/google";
import { AuthProvider } from "@/lib/auth";
import "./globals.css";
import { cn } from "@/lib/utils";

const inter = Inter({subsets:['latin'],variable:'--font-sans'});

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "lupo cloud",
  description: "Cloud management dashboard",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
      <html lang="en" className={cn("h-full", "dark","antialiased", geistSans.variable, geistMono.variable, "font-sans", inter.variable)}>
        <body className="relative min-h-full flex flex-col bg-neutral-950 text-neutral-100">

        <div className="pointer-events-none absolute inset-0 h-full w-full mask-[radial-gradient(ellipse_50%_50%_at_50%_50%,#000_60%,transparent_100%)] bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px]"></div>

          <div className="pointer-events-none absolute inset-0 flex items-center justify-center overflow-hidden z-0">
              <div className="absolute -top-40 -left-20 h-96 w-96 rounded-full bg-purple-600/20 blur-3xl opacity-30"></div>
              <div className="absolute -bottom-40 -right-40 h-96 w-96 rounded-full bg-indigo-600/20 blur-3xl"></div>
              <div className="absolute h-125 w-125 rounded-full bg-fuchsia-900/10 blur-[120px]"></div>
          </div>

          <div className="relative z-10 flex flex-col flex-1">
            <AuthProvider>
                { children }
            </AuthProvider>
          </div>
      </body>
      </html>
  );
}
