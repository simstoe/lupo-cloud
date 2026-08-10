"use client";

import { useEffect, useRef, useState, type ComponentType, type CSSProperties, type ReactNode, type SVGProps } from "react";

export function GlassCard({
                              children,
                              className = "",
                              style,
                          }: {
    children: ReactNode;
    className?: string;
    style?: CSSProperties;
    glow?: boolean;
}) {
    return (
        <div
            style={style}
            className={`relative border border-white/10 bg-[#0a0a0a] transition-all duration-200 hover:border-white/25 ${className}`}
        >
            {children}
        </div>
    );
}

export function fadeStyle(i: number, stepMs = 35, maxMs = 350): CSSProperties {
    return { animationDelay: `${Math.min(i * stepMs, maxMs)}ms` };
}

export function LiveDot({ color = "bg-white" }: { color?: string }) {
    return (
        <span className="relative flex h-1.5 w-1.5">
      <span className={`absolute inline-flex h-full w-full animate-ping ${color} opacity-60`} />
      <span className={`relative inline-flex h-1.5 w-1.5 ${color}`} />
    </span>
    );
}

export function UsageBar({ value, label }: { value: number; label: string }) {
    const color = value > 85 ? "bg-red-500" : value > 65 ? "bg-amber-400" : "bg-white/70";
    return (
        <div className="flex items-center gap-2.5">
            <span className="w-9 shrink-0 font-mono text-[10px] uppercase tracking-wider text-white/35">{label}</span>
            <div className="h-1 flex-1 bg-white/10">
                <div className={`h-full ${color} transition-[width] duration-700 ease-out`} style={{ width: `${value}%` }} />
            </div>
            <span className="w-8 shrink-0 text-right font-mono text-[10px] tabular-nums text-white/45">{value}%</span>
        </div>
    );
}

export function CountUp({ value, className = "" }: { value: number; className?: string }) {
    const [display, setDisplay] = useState(value);
    const prevRef = useRef(value);

    useEffect(() => {
        const start = prevRef.current;
        const startTime = performance.now();
        const duration = 700;
        let raf: number;

        function tick(now: number) {
            const t = Math.min(1, (now - startTime) / duration);
            const eased = 1 - Math.pow(1 - t, 3);
            setDisplay(Math.round(start + (value - start) * eased));
            if (t < 1) raf = requestAnimationFrame(tick);
            else prevRef.current = value;
        }

        raf = requestAnimationFrame(tick);
        return () => cancelAnimationFrame(raf);
    }, [value]);

    return <span className={`tabular-nums ${className}`}>{display.toLocaleString("de-DE")}</span>;
}

export function Sparkline({ data, className = "", color = "#ffffff" }: { data: number[]; className?: string; color?: string }) {
    const w = 100;
    const h = 32;
    const max = Math.max(...data);
    const min = Math.min(...data);
    const range = max - min || 1;

    const points = data.map((v, i) => {
        const x = (i / (data.length - 1)) * w;
        const y = h - ((v - min) / range) * h;
        return [x, y] as const;
    });

    const line = points.map(([x, y], i) => `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`).join(" ");
    const area = `${line} L${w},${h} L0,${h} Z`;
    const gradId = "spark-grad";

    return (
        <svg viewBox={`0 0 ${w} ${h}`} className={className} preserveAspectRatio="none">
            <defs>
                <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={color} stopOpacity="0.18" />
                    <stop offset="100%" stopColor={color} stopOpacity="0" />
                </linearGradient>
            </defs>
            <path d={area} fill={`url(#${gradId})`} className="animate-[fadeIn_0.8s_ease-out]" />
            <path
                d={line}
                fill="none"
                stroke={color}
                strokeWidth="1.4"
                strokeLinecap="round"
                strokeLinejoin="round"
                pathLength={1}
                style={{
                    strokeDasharray: 1,
                    strokeDashoffset: 0,
                    animation: "drawLine 1.2s ease-out",
                }}
            />
        </svg>
    );
}

export function MetricStat({
    icon: Icon,
    label,
    value,
    unit = "%",
    sub,
    history,
    style,
}: {
    icon: ComponentType<SVGProps<SVGSVGElement>>;
    label: string;
    value: number;
    unit?: string;
    sub?: string;
    history: number[];
    style?: CSSProperties;
}) {
    const tone = value > 85 ? "text-red-400" : value > 65 ? "text-amber-400" : "text-white";
    const sparkColor = value > 85 ? "#f87171" : value > 65 ? "#fbbf24" : "#EAB308";
    return (
        <GlassCard
            style={style}
            className="stagger-in group relative overflow-hidden p-5 hover:-translate-y-0.5 hover:shadow-[0_12px_30px_-8px_rgba(0,0,0,0.5)]"
        >
            <div className="flex items-center justify-between">
                <span className="flex h-8 w-8 items-center justify-center border border-white/15 text-white/50 transition-colors duration-200 group-hover:border-white/30 group-hover:text-white/70">
                    <Icon className="h-4 w-4" />
                </span>
                <span className="font-mono text-[10px] uppercase tracking-wider text-white/35">{label}</span>
            </div>
            <div className={`mt-4 flex items-baseline gap-1 font-minecraft ${tone}`}>
                <CountUp value={value} className="text-4xl leading-none" />
                <span className="text-base text-white/30">{unit}</span>
            </div>
            {sub && <div className="mt-1.5 font-mono text-[11px] text-white/35">{sub}</div>}
            <Sparkline data={history} color={sparkColor} className="absolute inset-x-0 bottom-0 h-12 w-full opacity-[0.35] transition-opacity duration-200 group-hover:opacity-50" />
        </GlassCard>
    );
}

export function SkeletonCard({ className = "" }: { className?: string }) {
    return <div className={`skeleton-shimmer border border-white/10 ${className}`} />;
}

export function Badge({ children, className = "" }: { children: ReactNode; className?: string }) {
    return (
        <span
            className={`inline-flex items-center border border-white/15 px-2 py-0.5 font-mono text-[10px] font-medium uppercase tracking-wider text-white/55 ${className}`}
        >
      {children}
    </span>
    );
}