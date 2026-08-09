import type { SVGProps } from "react";

type P = SVGProps<SVGSVGElement>;

export const IconBolt = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M13 2 3 14h7l-1 8 10-12h-7l1-8z" />
    </svg>
);

export const IconDiscord = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M20.3 5.4A18.3 18.3 0 0 0 15.9 4a12.9 12.9 0 0 0-.6 1.2 17 17 0 0 0-4.9 0A12.9 12.9 0 0 0 9.8 4a18.2 18.2 0 0 0-4.4 1.4C2.6 9 1.9 12.5 2.2 16a18.4 18.4 0 0 0 5.5 2.7c.4-.6.8-1.3 1.1-2a12 12 0 0 1-1.8-.8l.4-.3a13 13 0 0 0 10.9 0l.4.3c-.6.3-1.2.6-1.8.8.3.7.7 1.4 1.1 2a18.4 18.4 0 0 0 5.5-2.7c.4-4-.6-7.5-3.2-10.6zM9.7 13.9c-.8 0-1.5-.8-1.5-1.7s.7-1.7 1.5-1.7 1.5.8 1.5 1.7-.7 1.7-1.5 1.7zm4.6 0c-.8 0-1.5-.8-1.5-1.7s.7-1.7 1.5-1.7 1.5.8 1.5 1.7-.6 1.7-1.5 1.7z" />
    </svg>
);

export const IconGlobe = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.6} {...p}>
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18M12 3c2.5 2.5 3.8 5.6 3.8 9s-1.3 6.5-3.8 9c-2.5-2.5-3.8-5.6-3.8-9S9.5 5.5 12 3z" />
    </svg>
);

export const IconCheck = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2.4} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <path d="M4 12.5 9.5 18 20 6" />
    </svg>
);

export const IconGear = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path
            fillRule="evenodd"
            d="M10 1h4v2.6a8.5 8.5 0 0 1 3 1.3l1.9-1.9 2.8 2.9-1.8 1.8a8.5 8.5 0 0 1 1.2 3H24v4h-2.9a8.5 8.5 0 0 1-1.2 3l1.8 1.8-2.8 2.9-1.9-1.9a8.5 8.5 0 0 1-3 1.3V23h-4v-2.6a8.5 8.5 0 0 1-3-1.3l-1.9 1.9-2.8-2.9 1.8-1.8a8.5 8.5 0 0 1-1.2-3H0v-4h2.9a8.5 8.5 0 0 1 1.2-3L2.3 5.9l2.8-2.9L7 4.9a8.5 8.5 0 0 1 3-1.3V1zm2 7a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"
        />
    </svg>
);

export const IconBell = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M12 22a2 2 0 0 0 2-2h-4a2 2 0 0 0 2 2zm6-6v-5a6 6 0 0 0-4-5.7V4a2 2 0 1 0-4 0v1.3A6 6 0 0 0 6 11v5l-2 2v1h16v-1l-2-2z" />
    </svg>
);

export const IconChevronDown = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M12 15.4 4.6 8 6 6.6l6 6 6-6L19.4 8 12 15.4z" />
    </svg>
);

export const IconSearch = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} {...p}>
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.3-4.3" strokeLinecap="round" />
    </svg>
);

export const IconCloud = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M7 18a5 5 0 0 1-.6-9.97A6 6 0 0 1 18 7.06 4.5 4.5 0 0 1 17.5 18H7z" />
    </svg>
);

export const IconServer = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path fillRule="evenodd" d="M2 3h20v7H2V3zm2 2v3h16V5H4zM2 14h20v7H2v-7zm2 2v3h16v-3H4z" />
        <path d="M5 6h2v1H5zM5 15h2v1H5z" />
    </svg>
);

export const IconGrid = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M3 3h8v8H3V3zm10 0h8v8h-8V3zM3 13h8v8H3v-8zm10 0h8v8h-8v-8z" />
    </svg>
);

export const IconLayers = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M12 2 2 7l10 5 10-5-10-5zM2 12l10 5 10-5M2 17l10 5 10-5" stroke="currentColor" strokeWidth={2} fill="none" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
);

export const IconActivity = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <path d="M2 12h4l2 8 4-16 2 8h8" />
    </svg>
);

export const IconPlus = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M11 5h2v6h6v2h-6v6h-2v-6H5v-2h6V5z" />
    </svg>
);

export const IconMinus = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M5 11h14v2H5z" />
    </svg>
);

export const IconRefresh = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z" />
    </svg>
);

export const IconCpu = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path fillRule="evenodd" d="M9 2h2v2h2V2h2v2h1a3 3 0 0 1 3 3v1h2v2h-2v2h2v2h-2v1a3 3 0 0 1-3 3h-1v2h-2v-2h-2v2H9v-2H8a3 3 0 0 1-3-3v-1H3v-2h2v-2H3V9h2V8a3 3 0 0 1 3-3h1V2zm-1 5a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V8a1 1 0 0 0-1-1H8z" />
        <path d="M9.5 9.5h5v5h-5z" />
    </svg>
);

export const IconMemory = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <rect x="3" y="6" width="18" height="12" rx="1.2" />
        <path d="M7 6V3M11 6V3M13 6V3M17 6V3M7 21v-3M11 21v-3M13 21v-3M17 21v-3" />
    </svg>
);

export const IconDisk = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <rect x="3" y="4" width="18" height="16" rx="1.5" />
        <circle cx="12" cy="14.3" r="3.1" />
        <circle cx="12" cy="14.3" r="0.6" fill="currentColor" stroke="none" />
        <path d="M7 7.5h2" />
    </svg>
);

export const IconSettings = IconGear;

export const IconUsers = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <circle cx="9" cy="8" r="3.2" />
        <path d="M2.5 20c.6-3.6 3.2-5.8 6.5-5.8s5.9 2.2 6.5 5.8" />
        <circle cx="17.5" cy="8.5" r="2.6" />
        <path d="M15.8 14.4c2.6.3 4.5 2.3 5 5.6" />
    </svg>
);

export const IconChevronRight = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M8.6 4.6 7.2 6l6 6-6 6 1.4 1.4L16 12z" />
    </svg>
);

export const IconMenu = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M3 6h18v2H3zM3 11h18v2H3zM3 16h18v2H3z" />
    </svg>
);

export const IconCopy = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={1.8} strokeLinecap="round" strokeLinejoin="round" {...p}>
        <rect x="8" y="8" width="13" height="13" rx="1.5" />
        <path d="M16 8V5.5A1.5 1.5 0 0 0 14.5 4H4.5A1.5 1.5 0 0 0 3 5.5v10A1.5 1.5 0 0 0 4.5 17H8" />
    </svg>
);

export const IconX = (p: P) => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} strokeLinecap="round" {...p}>
        <path d="M6 6l12 12M18 6 6 18" />
    </svg>
);

/* Fenster-Buttons — Iconify "pixel"-Set, CC BY 4.0 */

export const IconWinMin = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M23 11v2h-1v1H2v-1H1v-2h1v-1h20v1z" />
    </svg>
);

export const IconWinMax = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M9 20v3H2v-1H1v-7h3v5zM9 1v3H4v5H1V2h1V1zm14 14v7h-1v1h-7v-3h5v-5zm0-13v7h-3V4h-5V1h7v1z" />
    </svg>
);

export const IconWinClose = (p: P) => (
    <svg viewBox="0 0 24 24" fill="currentColor" {...p}>
        <path d="M22 2V1H2v1H1v20h1v1h20v-1h1V2zm-4 7h-1v1h-1v1h-1v2h1v1h1v1h1v1h-1v1h-1v1h-1v-1h-1v-1h-1v-1h-2v1h-1v1H9v1H8v-1H7v-1H6v-1h1v-1h1v-1h1v-2H8v-1H7V9H6V8h1V7h1V6h1v1h1v1h1v1h2V8h1V7h1V6h1v1h1v1h1z" />
    </svg>
);