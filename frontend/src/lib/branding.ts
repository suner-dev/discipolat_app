import type { PublicBranding } from '@/types';

/**
 * Moteur de marque de l'église.
 *
 * Applique l'identité configurée (nom, logo, favicon, couleurs, typographie)
 * sur l'ensemble de l'application via des variables CSS runtime. Le design
 * system Tailwind (`primary` / `gold`) lit ces variables : une modification
 * côté administration se répercute instantanément partout, sans changement
 * de code ni rebuild.
 */

export const DEFAULT_BRANDING: PublicBranding = {
  churchName: 'Discipolat',
  platformName: 'Discipolat',
  slogan: 'Former des disciples de Jésus-Christ',
  description: 'Plateforme de gestion du discipolat',
  logoUrl: undefined,
  faviconUrl: undefined,
  bannerUrl: undefined,
  primaryColor: '#16a34a',
  accentColor: '#f59e0b',
  buttonColor: '#16a34a',
  fontFamily: 'Inter',
  allowDarkMode: true,
  address: undefined,
  phone: undefined,
  email: undefined,
  website: undefined,
  socialLinks: undefined,
};

const BRANDING_CACHE_KEY = 'discipolat.branding.v1';

const FONT_STACKS: Record<string, string> = {
  Inter: "'Inter', system-ui, -apple-system, sans-serif",
  'Playfair Display': "'Playfair Display', Georgia, serif",
  Poppins: "'Poppins', system-ui, sans-serif",
  Roboto: "'Roboto', system-ui, sans-serif",
  Georgia: "Georgia, 'Times New Roman', serif",
  'Open Sans': "'Open Sans', system-ui, sans-serif",
  Montserrat: "'Montserrat', system-ui, sans-serif",
};

export function fontStack(fontFamily: string): string {
  return FONT_STACKS[fontFamily] || FONT_STACKS.Inter;
}

/* ------------------------------------------------------------------ *
 * Génération de la gamme de teintes (50 → 950) depuis une couleur
 * ------------------------------------------------------------------ */

function clamp(v: number): number {
  return Math.max(0, Math.min(255, Math.round(v)));
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const clean = hex.replace('#', '');
  const full = clean.length === 3
    ? clean.split('').map((c) => c + c).join('')
    : clean;
  const int = parseInt(full, 16);
  return { r: (int >> 16) & 255, g: (int >> 8) & 255, b: int & 255 };
}

function mixWith(base: { r: number; g: number; b: number }, target: { r: number; g: number; b: number }, ratio: number) {
  return {
    r: clamp(base.r + (target.r - base.r) * ratio),
    g: clamp(base.g + (target.g - base.g) * ratio),
    b: clamp(base.b + (target.b - base.b) * ratio),
  };
}

const WHITE = { r: 255, g: 255, b: 255 };
const BLACK = { r: 0, g: 0, b: 0 };

/** Proportions de mélange vers blanc (shades clairs) / noir (shades foncés). */
const SHADE_MIX: Record<number, { toward: typeof WHITE | typeof BLACK; ratio: number }> = {
  50: { toward: WHITE, ratio: 0.92 },
  100: { toward: WHITE, ratio: 0.82 },
  200: { toward: WHITE, ratio: 0.62 },
  300: { toward: WHITE, ratio: 0.42 },
  400: { toward: WHITE, ratio: 0.22 },
  500: { toward: WHITE, ratio: 0 },
  600: { toward: BLACK, ratio: 0.14 },
  700: { toward: BLACK, ratio: 0.3 },
  800: { toward: BLACK, ratio: 0.46 },
  900: { toward: BLACK, ratio: 0.62 },
  950: { toward: BLACK, ratio: 0.76 },
};

/** Produit un mapping shade → triplet RGB "r g b" (format attendu par Tailwind). */
export function shadeScale(hex: string): Record<string, string> {
  const base = hexToRgb(hex || '#16a34a');
  const scale: Record<string, string> = {};
  for (const [shade, { toward, ratio }] of Object.entries(SHADE_MIX)) {
    const mixed = mixWith(base, toward, ratio);
    scale[shade] = `${mixed.r} ${mixed.g} ${mixed.b}`;
  }
  return scale;
}

/* ------------------------------------------------------------------ *
 * Application du thème
 * ------------------------------------------------------------------ */

export function applyBranding(branding: PublicBranding): void {
  const root = document.documentElement;
  const b = { ...DEFAULT_BRANDING, ...branding };

  // Couleurs : le design system lit les variables --color-primary-* / --color-gold-*.
  const primary = shadeScale(b.primaryColor);
  const accent = shadeScale(b.accentColor);
  Object.entries(primary).forEach(([shade, rgb]) => {
    root.style.setProperty(`--color-primary-${shade}`, rgb);
  });
  Object.entries(accent).forEach(([shade, rgb]) => {
    root.style.setProperty(`--color-gold-${shade}`, rgb);
  });

  // Boutons : couleur dédiée (ou primaire par défaut), avec teinte hover dérivée.
  const button = b.buttonColor && b.buttonColor !== b.primaryColor
    ? b.buttonColor
    : b.primaryColor;
  const buttonRgb = hexToRgb(button);
  const buttonHover = mixWith(buttonRgb, BLACK, 0.14);
  root.style.setProperty('--color-button', `${buttonRgb.r} ${buttonRgb.g} ${buttonRgb.b}`);
  root.style.setProperty('--color-button-hover', `${buttonHover.r} ${buttonHover.g} ${buttonHover.b}`);

  // Typographie.
  root.style.setProperty('--font-sans', fontStack(b.fontFamily));
  root.style.setProperty('--font-display', fontStack(b.fontFamily));

  // Identité.
  if (b.platformName) {
    document.title = b.platformName;
  }
  if (b.faviconUrl) {
    let link = document.querySelector<HTMLLinkElement>("link[rel='icon']");
    if (!link) {
      link = document.createElement('link');
      link.rel = 'icon';
      document.head.appendChild(link);
    }
    link.href = b.faviconUrl;
  }

  // Le mode sombre peut être désactivé par l'église.
  if (!b.allowDarkMode) {
    root.classList.remove('dark');
    localStorage.setItem('darkMode', 'false');
  }
}

export function getCachedBranding(): PublicBranding | null {
  try {
    const raw = localStorage.getItem(BRANDING_CACHE_KEY);
    return raw ? (JSON.parse(raw) as PublicBranding) : null;
  } catch {
    return null;
  }
}

export function cacheBranding(branding: PublicBranding): void {
  try {
    localStorage.setItem(BRANDING_CACHE_KEY, JSON.stringify(branding));
  } catch {
    /* stockage indisponible (navigation privée…) */
  }
}

/** Applique le cache immédiatement (anti-flash) puis le serveur a le dernier mot. */
export function bootstrapBranding(): void {
  const cached = getCachedBranding();
  if (cached) {
    applyBranding(cached);
  }
}
