/**
 * Moteur TTS Web — Web Speech API avec file d'attente FIFO.
 *
 * Gère la synthèse vocale dans le navigateur avec :
 * - File d'attente FIFO pour lecture séquentielle
 * - Déblocage autoplay (clic utilisateur requis)
 * - Configuration voix (langue, vitesse, pitch)
 * - Gestion d'appareils sans moteur vocal
 */

export interface TTSOptions {
  lang?: string;
  rate?: number;    // 0.1 to 10
  pitch?: number;   // 0 to 2
  volume?: number;  // 0 to 1
  voice?: SpeechSynthesisVoice | null;
  onComplete?: () => void;
}

interface QueueItem {
  text: string;
  options?: TTSOptions;
  onComplete?: () => void;
}

class TTSEngine {
  private synth: SpeechSynthesis | null = null;
  private queue: QueueItem[] = [];
  private isPlaying = false;
  private isUnlocked = false;
  private defaultOptions: TTSOptions = {
    lang: 'fr-FR',
    rate: 0.9,
    pitch: 1.0,
    volume: 1.0,
  };

  constructor() {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.synth = window.speechSynthesis;
    }
  }

  /**
   * Vérififie si le moteur TTS est disponible sur cet appareil.
   */
  isAvailable(): boolean {
    return this.synth !== null;
  }

  /**
   * Débloque l'audio (doit être appelé depuis un événement utilisateur).
   */
  async unlock(): Promise<boolean> {
    if (!this.synth) return false;

    try {
      // Créer un utterance silencieux pour débloquer l'autoplay
      const utterance = new SpeechSynthesisUtterance('');
      utterance.volume = 0;
      this.synth.speak(utterance);
      this.isUnlocked = true;
      return true;
    } catch {
      return false;
    }
  }

  /**
   * Vérifie si l'audio est débloqué.
   */
  getIsUnlocked(): boolean {
    return this.isUnlocked;
  }

  /**
   * Lit un texte immédiatement (ajouté à la file d'attente).
   */
  speak(text: string, options?: TTSOptions): void {
    if (!this.synth || !text.trim()) return;

    const item: QueueItem = {
      text: text.trim(),
      options: { ...this.defaultOptions, ...options },
    };

    this.queue.push(item);
    this.processQueue();
  }

  /**
   * Lit un texte en priorité (ajouté en tête de file).
   */
  speakPriority(text: string, options?: TTSOptions): void {
    if (!this.synth || !text.trim()) return;

    const item: QueueItem = {
      text: text.trim(),
      options: { ...this.defaultOptions, ...options },
    };

    this.queue.unshift(item);
    this.processQueue();
  }

  /**
   * Arrête la lecture en cours et vide la file.
   */
  stop(): void {
    if (this.synth) {
      this.synth.cancel();
    }
    this.queue = [];
    this.isPlaying = false;
  }

  /**
   * Met en pause la lecture.
   */
  pause(): void {
    if (this.synth) {
      this.synth.pause();
    }
  }

  /**
   * Reprend la lecture.
   */
  resume(): void {
    if (this.synth) {
      this.synth.resume();
    }
  }

  /**
   * Vide la file d'attente sans arrêter la lecture en cours.
   */
  clearQueue(): void {
    this.queue = [];
  }

  /**
   * Retourne le nombre d'éléments dans la file d'attente.
   */
  getQueueLength(): number {
    return this.queue.length;
  }

  /**
   * Vérifie si le moteur est en train de parler.
   */
  getIsPlaying(): boolean {
    return this.isPlaying;
  }

  /**
   * Retourne les voix disponibles.
   */
  getVoices(): SpeechSynthesisVoice[] {
    if (!this.synth) return [];
    return this.synth.getVoices();
  }

  /**
   * Définit les options par défaut.
   */
  setDefaultOptions(options: TTSOptions): void {
    this.defaultOptions = { ...this.defaultOptions, ...options };
  }

  /**
   * Traitement de la file d'attente FIFO.
   */
  private processQueue(): void {
    if (!this.synth || this.isPlaying || this.queue.length === 0) return;

    const item = this.queue.shift()!;
    this.playUtterance(item);
  }

  /**
   * Lit un utterance individuel.
   */
  private playUtterance(item: QueueItem): void {
    if (!this.synth) return;

    this.isPlaying = true;

    const utterance = new SpeechSynthesisUtterance(item.text);
    utterance.lang = item.options?.lang || this.defaultOptions.lang || 'fr-FR';
    utterance.rate = item.options?.rate || this.defaultOptions.rate || 1;
    utterance.pitch = item.options?.pitch || this.defaultOptions.pitch || 1;
    utterance.volume = item.options?.volume || this.defaultOptions.volume || 1;

    if (item.options?.voice) {
      utterance.voice = item.options.voice;
    }

    utterance.onend = () => {
      this.isPlaying = false;
      item.onComplete?.();
      this.processQueue();
    };

    utterance.onerror = (event) => {
      console.error('[TTS] Erreur lecture:', event.error);
      this.isPlaying = false;
      this.processQueue();
    };

    this.synth.speak(utterance);
  }
}

// Instance singleton
export const ttsEngine = new TTSEngine();
export default ttsEngine;
