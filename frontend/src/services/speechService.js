/**
 * Speech Recognition Service utilizing Browser Web Speech API
 * Supports continuous live speech-to-text with interim and final transcript handling.
 */

const SpeechRecognition =
  typeof window !== 'undefined'
    ? window.SpeechRecognition || window.webkitSpeechRecognition
    : null;

export const SUPPORTED_LANGUAGES = [
  { code: 'en-IN', name: 'English (India)' },
  { code: 'en-US', name: 'English (US)' },
  { code: 'hi-IN', name: 'Hindi (हिंदी)' },
  { code: 'kn-IN', name: 'Kannada (ಕನ್ನಡ)' },
  { code: 'en-GB', name: 'English (UK)' },
];

const STORAGE_LANG_KEY = 'aiStudioSpeechLanguage';

let activeRecognitionInstance = null;

export const speechService = {
  /**
   * Checks whether the current browser environment supports the Web Speech API.
   */
  isSpeechRecognitionSupported: () => {
    return Boolean(SpeechRecognition);
  },

  /**
   * Gets the saved speech recognition language from localStorage or defaults to 'en-IN'.
   */
  getSelectedLanguage: () => {
    try {
      return localStorage.getItem(STORAGE_LANG_KEY) || 'en-IN';
    } catch {
      return 'en-IN';
    }
  },

  /**
   * Saves the preferred language in localStorage.
   */
  setSelectedLanguage: (langCode) => {
    try {
      localStorage.setItem(STORAGE_LANG_KEY, langCode);
    } catch (e) {
      console.warn('Failed to persist speech language', e);
    }
  },

  /**
   * Starts a new speech recognition session.
   *
   * @param {Object} options
   * @param {Function} options.onTranscript Callback called with { interim, final, combined }
   * @param {Function} options.onStart Callback when recognition starts
   * @param {Function} options.onEnd Callback when recognition concludes
   * @param {Function} options.onError Callback when an error occurs with human-readable message
   * @param {string} options.language Language code (e.g. 'en-IN')
   */
  startRecognition: ({
    onTranscript,
    onStart,
    onEnd,
    onError,
    language,
  }) => {
    if (!SpeechRecognition) {
      if (onError) {
        onError("Voice input isn't supported in this browser. Please try Chrome or a Web Speech-compatible browser.");
      }
      return null;
    }

    // Stop any existing recognition instance first
    speechService.abortRecognition();

    try {
      const recognition = new SpeechRecognition();
      recognition.continuous = true;
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;
      recognition.lang = language || speechService.getSelectedLanguage();

      let finalTranscriptAccumulator = '';

      recognition.onstart = () => {
        if (onStart) onStart();
      };

      recognition.onresult = (event) => {
        let interimTranscript = '';

        for (let i = event.resultIndex; i < event.results.length; i++) {
          const result = event.results[i];
          const transcriptChunk = result[0].transcript;

          if (result.isFinal) {
            finalTranscriptAccumulator += (finalTranscriptAccumulator ? ' ' : '') + transcriptChunk.trim();
          } else {
            interimTranscript += transcriptChunk;
          }
        }

        if (onTranscript) {
          onTranscript({
            interim: interimTranscript.trim(),
            final: finalTranscriptAccumulator.trim(),
          });
        }
      };

      recognition.onerror = (event) => {
        console.warn('[SpeechService] Recognition error:', event.error);
        let userMessage = 'Voice recognition error. Please try again.';

        switch (event.error) {
          case 'not-allowed':
          case 'service-not-allowed':
            userMessage = 'Microphone permission was denied. Please allow microphone access in your browser settings and try again.';
            break;
          case 'audio-capture':
            userMessage = 'No microphone was detected. Please check your audio input device.';
            break;
          case 'network':
            userMessage = 'Network error during speech recognition. Please check your internet connection.';
            break;
          case 'no-speech':
            userMessage = 'No speech was detected. Please speak into your microphone.';
            break;
          case 'aborted':
            return; // Normal cancellation, do not trigger error popup
          default:
            userMessage = `Speech recognition error: ${event.error}`;
        }

        if (onError) {
          onError(userMessage);
        }
      };

      recognition.onend = () => {
        activeRecognitionInstance = null;
        if (onEnd) onEnd();
      };

      activeRecognitionInstance = recognition;
      recognition.start();
      return recognition;

    } catch (err) {
      console.error('[SpeechService] Failed to initialize SpeechRecognition:', err);
      if (onError) {
        onError('Unable to start speech recognition: ' + err.message);
      }
      return null;
    }
  },

  /**
   * Gracefully stops the active recognition session after capturing remaining speech.
   */
  stopRecognition: () => {
    if (activeRecognitionInstance) {
      try {
        activeRecognitionInstance.stop();
      } catch (e) {
        console.warn('Error stopping recognition:', e);
      }
      activeRecognitionInstance = null;
    }
  },

  /**
   * Immediately aborts the active recognition session.
   */
  abortRecognition: () => {
    if (activeRecognitionInstance) {
      try {
        activeRecognitionInstance.abort();
      } catch (e) {
        console.warn('Error aborting recognition:', e);
      }
      activeRecognitionInstance = null;
    }
  },
};

export default speechService;
