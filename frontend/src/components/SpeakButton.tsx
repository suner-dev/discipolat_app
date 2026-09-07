import { useState, useCallback } from 'react';
import { Volume2, VolumeX, Loader2 } from 'lucide-react';
import ttsEngine from '@/lib/ttsEngine';

interface SpeakButtonProps {
  /** Texte à lire vocalement */
  text: string;
  /** Variante du bouton */
  variant?: 'icon' | 'button' | 'ghost';
  /** Taille de l'icône */
  size?: number;
  /** Classes CSS supplémentaires */
  className?: string;
  /** Options TTS */
  options?: {
    lang?: string;
    rate?: number;
    pitch?: number;
    volume?: number;
  };
  /** Callback après lecture complète */
  onComplete?: () => void;
}

/**
 * Bouton réutilisable pour déclencher la lecture vocale d'un texte.
 *
 * Utilise la Web Speech API du navigateur. Le premier clic débloque l'audio
 * (requis par les navigateurs modernes pour l'autoplay).
 *
 * @example
 * ```tsx
 * <SpeakButton text="Bienvenue sur Discipolat" />
 * <SpeakButton text="Nouveau membre ajouté" variant="button" />
 * ```
 */
export function SpeakButton({
  text,
  variant = 'icon',
  size = 18,
  className = '',
  options,
  onComplete,
}: SpeakButtonProps) {
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isEnabled, setIsEnabled] = useState(ttsEngine.getIsUnlocked());

  const handleSpeak = useCallback(async () => {
    if (!ttsEngine.isAvailable()) {
      console.warn('[SpeakButton] Moteur TTS non disponible sur cet appareil');
      return;
    }

    // Débloquer l'audio si nécessaire
    if (!isEnabled) {
      const unlocked = await ttsEngine.unlock();
      if (!unlocked) return;
      setIsEnabled(true);
    }

    // Arrêter la lecture en cours si déjà en train de parler
    if (isSpeaking) {
      ttsEngine.stop();
      setIsSpeaking(false);
      return;
    }

    setIsSpeaking(true);
    ttsEngine.speak(text, {
      ...options,
      onComplete: () => {
        setIsSpeaking(false);
        onComplete?.();
      },
    });
  }, [text, isEnabled, isSpeaking, options, onComplete]);

  if (!ttsEngine.isAvailable()) {
    return null; // Ne rien afficher si le moteur n'est pas disponible
  }

  if (variant === 'button') {
    return (
      <button
        onClick={handleSpeak}
        className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-indigo-50 text-indigo-700 hover:bg-indigo-100 transition-colors text-sm font-medium ${className}`}
        title={isSpeaking ? 'Arrêter la lecture' : 'Lire vocalement'}
      >
        {isSpeaking ? <VolumeX size={size} /> : <Volume2 size={size} />}
        <span>{isSpeaking ? 'Arrêter' : 'Écouter'}</span>
      </button>
    );
  }

  if (variant === 'ghost') {
    return (
      <button
        onClick={handleSpeak}
        className={`p-1.5 rounded-full hover:bg-gray-100 transition-colors ${className}`}
        title={isSpeaking ? 'Arrêter la lecture' : 'Lire vocalement'}
      >
        {isSpeaking ? (
          <VolumeX size={size} className="text-indigo-600" />
        ) : (
          <Volume2 size={size} className="text-gray-500 hover:text-indigo-600" />
        )}
      </button>
    );
  }

  // Variante icon (défaut)
  return (
    <button
      onClick={handleSpeak}
      className={`p-1.5 rounded-full hover:bg-gray-100 transition-colors inline-flex ${className}`}
      title={isSpeaking ? 'Arrêter la lecture' : 'Lire vocalement'}
    >
      {isSpeaking ? (
        <VolumeX size={size} className="text-indigo-600" />
      ) : (
        <Volume2 size={size} className="text-gray-500 hover:text-indigo-600" />
      )}
    </button>
  );
}

export default SpeakButton;
