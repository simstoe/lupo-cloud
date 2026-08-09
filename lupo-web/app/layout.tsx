import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import localFont from "next/font/local";
import { AuthProvider } from "@/lib/auth";
import "./globals.css";
import SideRays from "@/components/SideRays";

const geistSans = Geist({
    variable: "--font-geist-sans",
    subsets: ["latin"],
});

const geistMono = Geist_Mono({
    variable: "--font-geist-mono",
    subsets: ["latin"],
});

const minecraft = localFont({
    variable: "--font-minecraft-local",
    display: "swap",
    src: [
        { path: "../public/fonts/minecraft.ttf", weight: "400", style: "normal" },
    ],
});

export const metadata: Metadata = {
    title: "lupo cloud",
    description: "Dashboard für dein lupo-cloud Netzwerk",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
    return (
        <html
            lang="de"
            className={`${geistSans.variable} ${geistMono.variable} ${minecraft.variable} h-full antialiased`}
        >
        <body className="h-full overflow-hidden flex flex-col bg-[#050507] text-white relative">

        <div className="absolute inset-0 pointer-events-none z-0 overflow-hidden">
            <SideRays
                speed={2.5}
                rayColor1="#EAB308"
                rayColor2="#96c8ff"
                intensity={2}
                spread={2}
                origin="top-right"
                tilt={0}
                saturation={1.5}
                blend={0.75}
                falloff={1.6}
                opacity={1}
            />
        </div>

        <div className="relative z-10 flex flex-col h-full">
            <AuthProvider>{children}</AuthProvider>
        </div>

        </body>
        </html>
    );
}