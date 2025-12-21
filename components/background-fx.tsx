"use client"

export function BackgroundFX() {
  return (
    <div className="pointer-events-none fixed inset-0 z-0">
      {/* Subtle noise texture overlay */}
      <div
        className="absolute inset-0 opacity-[0.015] dark:opacity-[0.02]"
        style={{
          backgroundImage: `url("data:image/svg+xml,%3Csvg viewBox='0 0 400 400' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' /%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' /%3E%3C/svg%3E")`,
        }}
      />

      {/* Soft vignette */}
      <div
        className="absolute inset-0"
        style={{
          background: "radial-gradient(circle at 50% 50%, transparent 0%, hsl(var(--background) / 0.3) 100%)",
        }}
      />

      {/* Faint dot grid pattern */}
      <div
        className="absolute inset-0 opacity-[0.025] dark:opacity-[0.035]"
        style={{
          backgroundImage: `radial-gradient(circle, hsl(var(--foreground)) 1px, transparent 1px)`,
          backgroundSize: "32px 32px",
        }}
      />

      {/* Subtle animated gradient drift */}
      <div className="absolute inset-0 animate-gradient-drift opacity-[0.15] dark:opacity-[0.08]">
        <div
          className="absolute inset-0"
          style={{
            background:
              "radial-gradient(ellipse 80% 50% at 50% 120%, hsl(var(--primary) / 0.15), transparent), radial-gradient(ellipse 60% 50% at 80% -20%, hsl(var(--primary) / 0.1), transparent)",
          }}
        />
      </div>
    </div>
  )
}
