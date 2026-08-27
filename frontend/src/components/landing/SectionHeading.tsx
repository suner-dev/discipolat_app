import Reveal from '@/components/shared/Reveal';

interface SectionHeadingProps {
  eyebrow: string;
  title: string;
  titleAccent?: string;
  subtitle?: string;
  align?: 'center' | 'left';
  tone?: 'primary' | 'gold' | 'violet' | 'emerald';
}

const TONE: Record<NonNullable<SectionHeadingProps['tone']>, { chip: string; dot: string; accent: string }> = {
  primary: {
    chip: 'bg-primary-500/10 border-primary-500/20 text-primary-600 dark:text-primary-400',
    dot: 'bg-primary-500',
    accent: 'text-gradient',
  },
  gold: {
    chip: 'bg-gold-500/10 border-gold-500/20 text-gold-600 dark:text-gold-400',
    dot: 'bg-gold-500',
    accent: 'text-gradient-gold',
  },
  violet: {
    chip: 'bg-violet-500/10 border-violet-500/20 text-violet-600 dark:text-violet-400',
    dot: 'bg-violet-500',
    accent: 'text-gradient',
  },
  emerald: {
    chip: 'bg-emerald-500/10 border-emerald-500/20 text-emerald-600 dark:text-emerald-400',
    dot: 'bg-emerald-500',
    accent: 'text-gradient',
  },
};

export default function SectionHeading({ eyebrow, title, titleAccent, subtitle, align = 'center', tone = 'primary' }: SectionHeadingProps) {
  const c = TONE[tone];
  const center = align === 'center';
  return (
    <Reveal>
      <div className={`${center ? 'text-center' : 'text-left'} mb-14 sm:mb-16`}>
        <span className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full border text-xs font-medium mb-4 ${c.chip}`}>
          <span className={`w-1.5 h-1.5 rounded-full ${c.dot}`} />
          {eyebrow}
        </span>
        <h2 className={`text-3xl sm:text-4xl lg:text-5xl font-bold text-gray-900 dark:text-white font-display tracking-tight ${center ? 'mx-auto' : ''} text-balance`}>
          {title}{titleAccent ? <span className={c.accent}> {titleAccent}</span> : null}
        </h2>
        {subtitle && (
          <p className={`mt-4 text-sm sm:text-base text-gray-500 dark:text-gray-400 ${center ? 'max-w-2xl mx-auto' : 'max-w-2xl'}`}>
            {subtitle}
          </p>
        )}
      </div>
    </Reveal>
  );
}