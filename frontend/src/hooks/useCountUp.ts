import { useEffect, useRef, useState } from 'react';

/**
 * Hook qui anime un compteur de 0 à `end` lorsque l'élément entre dans le viewport.
 * Retourne la valeur courante à afficher.
 */
export function useCountUp(end: number, duration = 1800, startOnView = true) {
  const [value, setValue] = useState(0);
  const ref = useRef<HTMLDivElement>(null);
  const hasAnimated = useRef(false);

  useEffect(() => {
    if (!startOnView) {
      animateCount(0, end, duration, setValue);
      return;
    }

    const el = ref.current;
    if (!el) return;

    if (typeof IntersectionObserver === 'undefined') {
      setValue(end);
      return;
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasAnimated.current) {
          hasAnimated.current = true;
          animateCount(0, end, duration, setValue);
          observer.disconnect();
        }
      },
      { threshold: 0.3 },
    );

    observer.observe(el);
    return () => observer.disconnect();
  }, [end, duration, startOnView]);

  return { value, ref };
}

function animateCount(
  from: number,
  to: number,
  duration: number,
  setter: (v: number) => void,
) {
  const start = performance.now();
  const tick = (now: number) => {
    const elapsed = now - start;
    const progress = Math.min(elapsed / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    setter(Math.round(from + (to - from) * eased));
    if (progress < 1) requestAnimationFrame(tick);
  };
  requestAnimationFrame(tick);
}
