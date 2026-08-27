import { useState } from 'react';
import LandingNavbar from '@/components/landing/LandingNavbar';
import Hero from '@/components/landing/Hero';
import DemoModal from '@/components/landing/DemoModal';
import SectionProblem from '@/components/landing/SectionProblem';
import SectionSolution from '@/components/landing/SectionSolution';
import SectionEcosystem from '@/components/landing/SectionEcosystem';
import { Features, Modules } from '@/components/landing/SectionFeatures';
import RoleExplorer from '@/components/landing/RoleExplorer';
import SectionBeforeAfter from '@/components/landing/SectionBeforeAfter';
import StatsSection from '@/components/landing/SectionStats';
import HowItWorks from '@/components/landing/SectionHowItWorks';
import { Customization, Security } from '@/components/landing/SectionCustomSecurity';
import MultiDevice from '@/components/landing/SectionDevice';
import Pricing from '@/components/landing/SectionPricing';
import { CTAFinal, Footer } from '@/components/landing/CTAFooter';

/* ============================================================================
 * Landing page Discipolat — expérience premium modulaire.
 * Chaque section est un composant dédié, piloté par i18n et le système de
 * marque (couleurs, logo, typographie). Navigation par ancres + démo modale.
 * ========================================================================== */
export default function LandingPage() {
  const [demoOpen, setDemoOpen] = useState(false);

  const scrollTo = (id: string) => {
    const el = document.getElementById(id);
    if (el) {
      const y = el.getBoundingClientRect().top + window.pageYOffset - 64;
      window.scrollTo({ top: y, behavior: 'smooth' });
    }
  };

  const openDemo = () => setDemoOpen(true);

  return (
    <div className="min-h-screen relative">
      <LandingNavbar onNavigate={scrollTo} onDemo={openDemo} />
      <main id="main-content" className="relative z-10">
        <Hero onNavigate={scrollTo} onDemo={openDemo} />
        <SectionProblem />
        <SectionSolution />
        <Features />
        <Modules />
        <SectionEcosystem />
        <RoleExplorer />
        <SectionBeforeAfter />
        <StatsSection />
        <HowItWorks />
        <Customization />
        <Security />
        <MultiDevice />
        <Pricing />
        <CTAFinal onDemo={openDemo} />
      </main>
      <Footer />
      <DemoModal open={demoOpen} onClose={() => setDemoOpen(false)} />
    </div>
  );
}
