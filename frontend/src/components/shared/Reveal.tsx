import { useEffect, useRef, useState, type ReactNode } from 'react';

interface RevealProps {
  children: ReactNode;
  /** Délai de transition en ms (staggering). */
  delay?: number;
  /** Classes additionnelles appliquées au wrapper. */
  className?: string;
  /** Direction d'entrée de l'animation. */
  direction?: 'up' | 'down' | 'left' | 'right';
}

const DIRECTION_TRANSFORMS: Record<NonNullable<RevealProps['direction']>, string> = {
  up: 'translateY(24px)',
  down: 'translateY(-24px)',
  left: 'translateX(24px)',
  right: 'translateX(-24px)',
};

/**
 * Révèle son contenu avec une animation de transition lorsque l'élément
 * entre dans le viewport. Désactivé (contenu visible) si IntersectionObserver
 * est indisponible (jsdom, anciens navigateurs) ou si l'utilisateur préfère
 * réduire les animations (géré côté CSS avec prefers-reduced-motion).
 */
export default function Reveal({
  children,
  delay = 0,
  className = '',
  direction = 'up',
}: RevealProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (typeof IntersectionObserver === 'undefined') {
      setVisible(true);
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            setVisible(true);
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -48px 0px' },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div
      ref={ref}
      className={`reveal ${visible ? 'reveal-visible' : ''} ${className}`}
      style={{
        transitionDelay: `${delay}ms`,
        ['--reveal-transform' as string]: DIRECTION_TRANSFORMS[direction],
      }}
    >
      {children}
    </div>
  );
}
