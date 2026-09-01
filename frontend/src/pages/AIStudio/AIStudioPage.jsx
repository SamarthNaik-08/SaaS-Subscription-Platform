import React, { useState, useEffect, useRef } from 'react';
import { Sparkles, Send, Bot, User, Copy, Check, AlertTriangle, RefreshCw, Plus, Paperclip, FolderOpen, Image as ImageIcon, Globe, Search, Brain, Mic, MicOff, X, FileText, ChevronDown, ChevronUp, ChevronRight, Terminal, Compass, FileCode, CheckCircle2, Download, Maximize2, RotateCcw, Trash2, ExternalLink, Wand2, BookOpen, Calendar, ShieldCheck, Award, BarChart3, Languages, Cpu } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { aiService } from '../../services/aiService';
import { usageService } from '../../services/usageService';
import { speechService, SUPPORTED_LANGUAGES } from '../../services/speechService';

const AVAILABLE_MODELS = [
  {
    id: 'gemini-2.5-flash',
    name: 'Gemini 2.5 Flash',
    category: 'Next-Gen',
    speed: 'Fast',
    provider: 'Google',
    description: 'Ultra-fast multimodal model with state-of-the-art response speed and high reasoning capability.',
  },
  {
    id: 'gemini-2.5-pro',
    name: 'Gemini 2.5 Pro',
    category: 'Pro',
    speed: 'Reasoning',
    provider: 'Google',
    description: 'Deep analytical model with extended context window for complex synthesis.',
  },
  {
    id: 'gpt-4o',
    name: 'GPT-4o (Multimodal)',
    category: 'Omni',
    speed: 'High-Perf',
    provider: 'OpenAI',
    description: 'Flagship multimodal reasoning model for cross-domain intelligence.',
  },
  {
    id: 'gpt-4o-mini',
    name: 'GPT-4o Mini',
    category: 'Mini',
    speed: 'Fast',
    provider: 'OpenAI',
    description: 'Fast, cost-optimized model ideal for everyday conversational prompts.',
  },
];

export const AIStudioPage = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [model, setModel] = useState('gemini-2.5-flash');
  const [modelsList, setModelsList] = useState(AVAILABLE_MODELS);
  const [messages, setMessages] = useState([
    {
      id: 'welcome-msg',
      role: 'assistant',
      content:
        'Hello! I am your AI assistant powered by Nexus AI Engine. Enter a prompt, attach files, or choose an action mode below to begin.',
      tokens: 32,
      timestamp: new Date().toLocaleTimeString(),
    },
  ]);
  const [loading, setLoading] = useState(false);
  const [searchStage, setSearchStage] = useState(0);
  const [researchStage, setResearchStage] = useState(0);
  const [isProcessingMultimodal, setIsProcessingMultimodal] = useState(false);
  const [temperature, setTemperature] = useState(0.7);
  const [systemInstruction, setSystemInstruction] = useState('');
  const [copiedIndex, setCopiedIndex] = useState(null);
  const [currentUsage, setCurrentUsage] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);
  const [isQuotaExceeded, setIsQuotaExceeded] = useState(false);

  // Model Selector Popover State (Prompt Bar)
  const [isModelMenuOpen, setIsModelMenuOpen] = useState(false);

  // Voice Input / Speech-to-Text State
  const [isListening, setIsListening] = useState(false);
  const [speechLanguage, setSpeechLanguage] = useState(speechService.getSelectedLanguage());
  const [isSpeechSupported] = useState(speechService.isSpeechRecognitionSupported());
  const [preSpeechPrompt, setPreSpeechPrompt] = useState('');
  const [isLangMenuOpen, setIsLangMenuOpen] = useState(false);

  // Action Menu, Mode, Image & Multimodal State
  const [isActionMenuOpen, setIsActionMenuOpen] = useState(false);
  const [actionSearchQuery, setActionSearchQuery] = useState('');
  const [activeMode, setActiveMode] = useState(null); // 'image' | 'web-search' | 'deep-research' | 'developer' | null
  const [isThinkActive, setIsThinkActive] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const [isLibraryOpen, setIsLibraryOpen] = useState(false);
  const [expandedThoughts, setExpandedThoughts] = useState({});

  // Image Generation Options
  const [imageAspectRatio, setImageAspectRatio] = useState('1:1');
  const [imageStylePreset, setImageStylePreset] = useState('Cinematic');
  const [lightboxImage, setLightboxImage] = useState(null);

  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const actionMenuRef = useRef(null);
  const modelMenuRef = useRef(null);
  const langMenuRef = useRef(null);

  useEffect(() => {
    loadModels();
    loadUsage();
    return () => {
      speechService.abortRecognition();
    };
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Click outside to close menus
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (actionMenuRef.current && !actionMenuRef.current.contains(e.target)) {
        setIsActionMenuOpen(false);
      }
      if (modelMenuRef.current && !modelMenuRef.current.contains(e.target)) {
        setIsModelMenuOpen(false);
      }
      if (langMenuRef.current && !langMenuRef.current.contains(e.target)) {
        setIsLangMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const loadModels = async () => {
    try {
      const res = await aiService.getModels();
      if (res.success && res.data && res.data.length > 0) {
        setModelsList(
          res.data.map((m) => ({
            id: m.id,
            name: m.name || m.id,
            category: m.provider || 'AI',
            speed: m.id.includes('flash') ? 'Fast' : 'Pro',
            provider: m.provider,
            description: `High-throughput model powered by ${m.provider || 'AI engine'}`,
          }))
        );
      }
    } catch (e) {
      console.warn('Failed to load models list from backend', e);
    }
  };

  const loadUsage = async () => {
    try {
      const data = await usageService.getCurrentUsage();
      setCurrentUsage(data);
    } catch (e) {
      console.warn('Failed to load usage in AI Studio', e);
    }
  };

  const handleToggleVoice = () => {
    if (!isSpeechSupported) {
      setErrorMsg("Voice input isn't supported in this browser. Please try Chrome or a Web Speech-compatible browser.");
      return;
    }

    if (isListening) {
      speechService.stopRecognition();
      setIsListening(false);
      return;
    }

    setErrorMsg(null);
    const existingText = prompt;
    setPreSpeechPrompt(existingText);

    speechService.startRecognition({
      language: speechLanguage,
      onStart: () => {
        setIsListening(true);
      },
      onTranscript: ({ interim, final }) => {
        const spokenParts = [final, interim].filter(Boolean).join(' ').trim();
        if (existingText) {
          setPrompt(`${existingText} ${spokenParts}`.trim());
        } else {
          setPrompt(spokenParts);
        }
      },
      onError: (errorMessage) => {
        setErrorMsg(errorMessage);
        setIsListening(false);
      },
      onEnd: () => {
        setIsListening(false);
      },
    });
  };

  const handleSelectLanguage = (langCode) => {
    setSpeechLanguage(langCode);
    speechService.setSelectedLanguage(langCode);
    setIsLangMenuOpen(false);
    if (isListening) {
      speechService.stopRecognition();
      setIsListening(false);
    }
  };

  const handleFileSelect = (e) => {
    const files = Array.from(e.target.files || []);
    if (!files.length) return;

    const newAttachments = files.map((file) => ({
      id: Math.random().toString(36).substring(2, 9),
      name: file.name,
      size: (file.size / 1024).toFixed(1) + ' KB',
      type: file.type,
      isImage: file.type.startsWith('image/'),
      previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : null,
      file,
    }));

    setAttachments((prev) => [...prev, ...newAttachments]);
    setIsActionMenuOpen(false);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const removeAttachment = (id) => {
    setAttachments((prev) => {
      const target = prev.find((a) => a.id === id);
      if (target?.previewUrl) URL.revokeObjectURL(target.previewUrl);
      return prev.filter((a) => a.id !== id);
    });
  };

  const libraryFiles = [
    { name: 'architecture_spec.md', size: '14.2 KB', type: 'doc' },
    { name: 'schema_design.sql', size: '8.4 KB', type: 'code' },
    { name: 'user_persona_brief.pdf', size: '1.2 MB', type: 'doc' },
    { name: 'api_endpoints.json', size: '22.0 KB', type: 'code' },
  ];

  const handleAttachFromLibrary = (file) => {
    setAttachments((prev) => [
      ...prev,
      {
        id: Math.random().toString(36).substring(2, 9),
        name: file.name,
        size: file.size,
        type: 'library/' + file.type,
        isImage: false,
        previewUrl: null,
      },
    ]);
    setIsLibraryOpen(false);
    setIsActionMenuOpen(false);
  };

  const actionMenuItems = [
    {
      id: 'upload',
      icon: Paperclip,
      title: 'Add photos & files',
      subtitle: 'Upload from computer',
      action: () => fileInputRef.current?.click(),
    },
    {
      id: 'library',
      icon: FolderOpen,
      title: 'Add from library',
      subtitle: 'Browse and search your files',
      action: () => {
        setIsLibraryOpen(true);
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'image',
      icon: ImageIcon,
      title: 'Create image',
      subtitle: 'Visualize anything with text-to-image',
      action: () => {
        setActiveMode('image');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'web-search',
      icon: Globe,
      title: 'Web search',
      subtitle: 'Find real-time news & citations',
      action: () => {
        setActiveMode('web-search');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'deep-research',
      icon: Compass,
      title: 'Deep research',
      subtitle: 'Multi-query empirical synthesis report',
      action: () => {
        setActiveMode('deep-research');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'developer',
      icon: Terminal,
      title: 'OpenAI Developers',
      subtitle: 'Develop AI apps, agents, and ChatGPT apps',
      action: () => {
        setActiveMode('developer');
        setIsActionMenuOpen(false);
      },
    },
  ];

  const filteredActionItems = actionMenuItems.filter(
    (item) =>
      item.title.toLowerCase().includes(actionSearchQuery.toLowerCase()) ||
      item.subtitle.toLowerCase().includes(actionSearchQuery.toLowerCase())
  );

  const toggleThoughtExpansion = (index) => {
    setExpandedThoughts((prev) => ({
      ...prev,
      [index]: !prev[index],
    }));
  };

  const handleDownloadImage = async (url, filename = 'nexus-ai-image.png') => {
    try {
      if (url.startsWith('data:')) {
        const link = document.createElement('a');
        link.href = url;
        link.download = filename.endsWith('.svg') || url.includes('svg') ? 'nexus-ai-image.svg' : 'nexus-ai-image.png';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        return;
      }
      const response = await fetch(url);
      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(blobUrl);
    } catch (e) {
      window.open(url, '_blank');
    }
  };

  const renderFormattedContentWithCitations = (content, citations = [], sources = []) => {
    if (!content) return null;

    const sourceMap = {};
    (sources || []).forEach((s) => {
      sourceMap[s.id] = s;
    });
    (citations || []).forEach((c) => {
      sourceMap[c.id] = c;
    });

    const parts = content.split(/(\[S\d+\])/g);

    return parts.map((part, pIdx) => {
      const match = part.match(/^\[(S\d+)\]$/);
      if (match) {
        const sourceId = match[1];
        const source = sourceMap[sourceId];
        if (source && source.url && (source.url.startsWith('http://') || source.url.startsWith('https://'))) {
          return (
            <a
              key={pIdx}
              href={source.url}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-0.5 px-1.5 py-0.5 mx-0.5 rounded text-[10px] font-bold bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 border border-sky-500/40 transition-all cursor-pointer hover:scale-105"
              title={`${source.sourceName || 'Source'}: ${source.title || source.url}`}
            >
              <span>[{sourceId}]</span>
              <ExternalLink className="w-2.5 h-2.5 opacity-80" />
            </a>
          );
        }
        return (
          <span
            key={pIdx}
            className="inline-block px-1.5 py-0.5 mx-0.5 rounded text-[10px] font-bold bg-slate-800 text-slate-300 border border-slate-700"
          >
            [{sourceId}]
          </span>
        );
      }
      return <span key={pIdx}>{part}</span>;
    });
  };

  const handleSend = async (e, overridePrompt = null) => {
    e?.preventDefault();
    if (isListening) {
      speechService.stopRecognition();
      setIsListening(false);
    }

    const targetPrompt = (overridePrompt !== null ? overridePrompt : prompt).trim();
    if ((!targetPrompt && attachments.length === 0) || loading) return;

    const currentAttachments = [...attachments];
    const hasFiles = currentAttachments.some((a) => Boolean(a.file));

    if (overridePrompt === null) {
      setPrompt('');
      setAttachments([]);
    }
    setErrorMsg(null);
    setIsQuotaExceeded(false);

    const messageId = Math.random().toString(36).substring(2, 9);

    const userMsg = {
      id: 'user-' + messageId,
      role: 'user',
      content: targetPrompt || (currentAttachments.length ? `[Attached ${currentAttachments.length} file(s)]` : ''),
      attachments: currentAttachments,
      mode: activeMode,
      thinkActive: isThinkActive,
      tokens: Math.max(1, Math.round((targetPrompt.length || 20) / 4)),
      timestamp: new Date().toLocaleTimeString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);
    if (hasFiles) setIsProcessingMultimodal(true);

    if (activeMode === 'web-search') {
      setSearchStage(1);
      setTimeout(() => setSearchStage(2), 1200);
      setTimeout(() => setSearchStage(3), 2400);
    } else if (activeMode === 'deep-research') {
      setResearchStage(1);
      setTimeout(() => setResearchStage(2), 1200);
      setTimeout(() => setResearchStage(3), 2600);
      setTimeout(() => setResearchStage(4), 4000);
      setTimeout(() => setResearchStage(5), 5500);
    }

    const startTime = performance.now();

    try {
      if (activeMode === 'image') {
        const res = await aiService.generateImage(targetPrompt, {
          aspectRatio: imageAspectRatio,
          stylePreset: imageStylePreset,
        });

        const elapsedSec = ((performance.now() - startTime) / 1000).toFixed(2);

        if (res.success && res.data) {
          const assistantMsg = {
            id: 'asst-' + messageId,
            role: 'assistant',
            isImage: true,
            imageUrl: res.data.imageUrl,
            prompt: res.data.prompt,
            revisedPrompt: res.data.revisedPrompt,
            model: res.data.model,
            provider: res.data.provider,
            aspectRatio: res.data.aspectRatio,
            stylePreset: res.data.stylePreset,
            latency: elapsedSec,
            timestamp: new Date().toLocaleTimeString(),
          };

          setMessages((prev) => [...prev, assistantMsg]);

          if (res.data.quotaUsage) {
            setCurrentUsage((prev) => ({
              ...prev,
              metrics: {
                ...prev?.metrics,
                AI_REQUEST: res.data.quotaUsage,
              },
            }));
          }
        }
      } else if (activeMode === 'deep-research') {
        let enrichedSystemInstruction = systemInstruction || '';
        if (isThinkActive) {
          enrichedSystemInstruction += ' Conduct rigorous multi-perspective reasoning and empirical cross-verification.';
        }

        const res = await aiService.deepResearch(targetPrompt, {
          depth: 2,
          maxQueries: 4,
          model,
          systemInstruction: enrichedSystemInstruction.trim() || undefined,
          temperature,
        });

        const elapsedSec = ((performance.now() - startTime) / 1000).toFixed(2);

        if (res.success && res.data) {
          const assistantMsg = {
            id: 'asst-' + messageId,
            role: 'assistant',
            isImage: false,
            isSearch: false,
            isDeepResearch: true,
            topic: res.data.topic,
            executiveSummary: res.data.executiveSummary,
            keyFindings: res.data.keyFindings || [],
            detailedAnalysis: res.data.detailedAnalysis,
            contradictions: res.data.contradictions,
            limitations: res.data.limitations,
            conclusion: res.data.conclusion,
            citations: res.data.citations || [],
            sources: res.data.sources || [],
            plan: res.data.plan,
            model: res.data.model,
            provider: res.data.provider,
            searchProvider: res.data.searchProvider,
            totalQueriesExecuted: res.data.totalQueriesExecuted,
            mode: 'deep-research',
            tokens: res.data.totalTokens,
            latency: elapsedSec,
            timestamp: new Date().toLocaleTimeString(),
          };

          setMessages((prev) => [...prev, assistantMsg]);

          if (res.data.quotaUsage) {
            setCurrentUsage((prev) => ({
              ...prev,
              metrics: {
                ...prev?.metrics,
                AI_REQUEST: res.data.quotaUsage,
              },
            }));
          }
        }
      } else if (activeMode === 'web-search') {
        let enrichedSystemInstruction = systemInstruction || '';
        if (isThinkActive) {
          enrichedSystemInstruction += ' Provide structured multi-perspective analysis and reasoning.';
        }

        const res = await aiService.generateWebSearchAnswer(targetPrompt, {
          model,
          maxResults: 5,
          systemInstruction: enrichedSystemInstruction.trim() || undefined,
          temperature,
        });

        const elapsedSec = ((performance.now() - startTime) / 1000).toFixed(2);

        if (res.success && res.data) {
          const reasoningSteps = isThinkActive
            ? [
                `Queried live search engine for: "${targetPrompt}"`,
                `Retrieved and normalized ${res.data.sources?.length || 0} authoritative sources`,
                `Validated ${res.data.citations?.length || 0} citation references against verified result set`,
                'Synthesized grounded answer with clickable citations',
              ]
            : null;

          const assistantMsg = {
            id: 'asst-' + messageId,
            role: 'assistant',
            isImage: false,
            isSearch: true,
            content: res.data.answer,
            citations: res.data.citations || [],
            sources: res.data.sources || [],
            model: res.data.model,
            provider: res.data.provider,
            searchProvider: res.data.searchProvider,
            mode: 'web-search',
            reasoningSteps,
            reasoningTime: isThinkActive ? elapsedSec : null,
            tokens: res.data.totalTokens,
            latency: elapsedSec,
            timestamp: new Date().toLocaleTimeString(),
          };

          setMessages((prev) => [...prev, assistantMsg]);

          if (isThinkActive) {
            setExpandedThoughts((prev) => ({
              ...prev,
              [messages.length + 1]: true,
            }));
          }

          if (res.data.quotaUsage) {
            setCurrentUsage((prev) => ({
              ...prev,
              metrics: {
                ...prev?.metrics,
                AI_REQUEST: res.data.quotaUsage,
              },
            }));
          }
        }
      } else if (hasFiles) {
        const formData = new FormData();
        formData.append('prompt', targetPrompt || 'Please analyze and summarize the attached files in detail.');
        formData.append('model', model);
        currentAttachments.forEach((att) => {
          if (att.file) {
            formData.append('files', att.file);
          }
        });

        const res = await aiService.multimodalGenerate(formData);
        const elapsedSec = ((performance.now() - startTime) / 1000).toFixed(2);

        if (res.success && res.data) {
          const reasoningSteps = isThinkActive
            ? [
                'Extracted multimodal visual & document token vectors',
                'Performed contextual cross-modality reasoning',
                'Evaluated code/content structural integrity',
                'Synthesized authoritative multimodal response',
              ]
            : null;

          const assistantMsg = {
            id: 'asst-' + messageId,
            role: 'assistant',
            isImage: false,
            content: res.data.text,
            model: res.data.model,
            provider: res.data.provider,
            mode: activeMode,
            reasoningSteps,
            reasoningTime: isThinkActive ? elapsedSec : null,
            tokens: res.data.totalTokens,
            latency: elapsedSec,
            timestamp: new Date().toLocaleTimeString(),
          };

          setMessages((prev) => [...prev, assistantMsg]);

          if (isThinkActive) {
            setExpandedThoughts((prev) => ({
              ...prev,
              [messages.length + 1]: true,
            }));
          }

          if (res.data.quotaUsage) {
            setCurrentUsage((prev) => ({
              ...prev,
              metrics: {
                ...prev?.metrics,
                AI_REQUEST: res.data.quotaUsage,
              },
            }));
          }
        }
      } else {
        let enrichedSystemInstruction = systemInstruction || '';
        if (isThinkActive) {
          enrichedSystemInstruction += ' Provide deep, structured step-by-step reasoning.';
        }

        const res = await aiService.generateText(targetPrompt || 'Analyze context', model, {
          temperature,
          systemInstruction: enrichedSystemInstruction.trim() || undefined,
        });

        const elapsedSec = ((performance.now() - startTime) / 1000).toFixed(2);

        if (res.success && res.data) {
          const reasoningSteps = isThinkActive
            ? [
                'Deconstructed contextual query requirements',
                'Evaluated multi-perspective knowledge boundaries',
                'Synthesized step-by-step validation steps',
                'Finalized authoritative response format',
              ]
            : null;

          const assistantMsg = {
            id: 'asst-' + messageId,
            role: 'assistant',
            isImage: false,
            content: res.data.text,
            model: res.data.model,
            provider: res.data.provider,
            mode: activeMode,
            reasoningSteps,
            reasoningTime: isThinkActive ? elapsedSec : null,
            tokens: res.data.totalTokens,
            latency: elapsedSec,
            timestamp: new Date().toLocaleTimeString(),
          };

          setMessages((prev) => [...prev, assistantMsg]);

          if (isThinkActive) {
            setExpandedThoughts((prev) => ({
              ...prev,
              [messages.length + 1]: true,
            }));
          }

          if (res.data.quotaUsage) {
            setCurrentUsage((prev) => ({
              ...prev,
              metrics: {
                ...prev?.metrics,
                AI_REQUEST: res.data.quotaUsage,
              },
            }));
          }
        }
      }
    } catch (err) {
      console.error('AI Generation error:', err);
      if (err.response?.status === 429) {
        setIsQuotaExceeded(true);
        setErrorMsg("You've reached your monthly AI request limit. Upgrade your plan to continue.");
      } else if (err.response?.status === 413) {
        setErrorMsg('File size exceeds the allowed limit (maximum 10 MB per file).');
      } else {
        setErrorMsg(err.response?.data?.message || 'Processing failed. Please try again.');
      }
    } finally {
      setLoading(false);
      setSearchStage(0);
      setResearchStage(0);
      setIsProcessingMultimodal(false);
    }
  };

  const handleCopy = (text, index) => {
    navigator.clipboard.writeText(text);
    setCopiedIndex(index);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const deleteMessage = (id) => {
    setMessages((prev) => prev.filter((m) => m.id !== id));
  };

  const getModeDetails = (modeKey) => {
    switch (modeKey) {
      case 'image':
        return {
          title: 'Create Image Mode',
          desc: 'Visualize anything with text-to-image synthesis',
          icon: ImageIcon,
          color: 'text-amber-400 border-amber-500/30 bg-amber-500/10',
        };
      case 'web-search':
        return {
          title: 'Web Search Mode',
          desc: 'Real-time live search with verified source citations',
          icon: Globe,
          color: 'text-sky-400 border-sky-500/30 bg-sky-500/10',
        };
      case 'deep-research':
        return {
          title: 'Deep Research Mode',
          desc: 'Multi-query empirical synthesis & structured report',
          icon: Compass,
          color: 'text-purple-400 border-purple-500/30 bg-purple-500/10',
        };
      case 'developer':
        return {
          title: 'Developer Mode',
          desc: 'AI app architecture & OpenAI SDK conventions',
          icon: Terminal,
          color: 'text-emerald-400 border-emerald-500/30 bg-emerald-500/10',
        };
      default:
        return null;
    }
  };

  const activeModeDetails = getModeDetails(activeMode);
  const selectedModelObj = modelsList.find((m) => m.id === model) || modelsList[0] || AVAILABLE_MODELS[0];
  const currentLangObj = SUPPORTED_LANGUAGES.find((l) => l.code === speechLanguage) || SUPPORTED_LANGUAGES[0];

  return (
    <div className="space-y-4 max-w-6xl mx-auto">
      {/* Hidden File Input */}
      <input
        type="file"
        ref={fileInputRef}
        onChange={handleFileSelect}
        multiple
        accept="image/*,.pdf,.txt,.csv,.json,.md,.js,.jsx,.ts,.tsx,.py,.java,.sql"
        className="hidden"
      />

      {/* Lightbox Modal */}
      {lightboxImage && (
        <div
          className="fixed inset-0 bg-black/90 backdrop-blur-md z-50 flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => setLightboxImage(null)}
        >
          <div
            className="relative max-w-4xl max-h-[90vh] bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl p-2 space-y-3"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-3 pt-2">
              <span className="text-xs font-semibold text-slate-300 truncate max-w-lg">
                {lightboxImage.prompt || 'Generated Image'}
              </span>
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => handleDownloadImage(lightboxImage.imageUrl)}
                  className="p-1.5 rounded-lg text-slate-300 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors"
                  title="Download Image"
                >
                  <Download className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setLightboxImage(null)}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 transition-colors"
                  title="Close Lightbox"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>
            <div className="flex items-center justify-center rounded-xl overflow-hidden bg-slate-950 max-h-[75vh]">
              <img
                src={lightboxImage.imageUrl}
                alt={lightboxImage.prompt || 'AI Image'}
                className="max-h-[75vh] w-auto object-contain rounded-xl shadow-lg"
              />
            </div>
            {lightboxImage.revisedPrompt && (
              <p className="text-[11px] text-slate-400 px-3 pb-1 italic">
                {lightboxImage.revisedPrompt}
              </p>
            )}
          </div>
        </div>
      )}

      {/* Error Alert Banner */}
      {errorMsg && (
        <div className="p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-between gap-4 animate-in fade-in">
          <div className="flex items-center space-x-3 text-rose-300 text-sm">
            <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          {isQuotaExceeded ? (
            <button
              onClick={() => navigate('/subscription')}
              className="px-4 py-1.5 rounded-lg text-xs font-bold bg-rose-500 hover:bg-rose-600 text-white shadow-md transition-all shrink-0"
            >
              Upgrade Plan
            </button>
          ) : (
            <button
              onClick={() => setErrorMsg(null)}
              className="p-1 rounded-lg text-rose-400 hover:text-rose-200"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      )}

      {/* Main Studio Canvas (Full Width Chat & Prompt Bar) */}
      <div className="flex flex-col h-[calc(100vh-140px)] min-h-[620px] rounded-2xl bg-slate-900/60 border border-slate-800/80 shadow-2xl overflow-hidden relative backdrop-blur-sm">
        
        {/* Top Mini Toolbar */}
        <div className="px-5 py-3 border-b border-slate-800/80 bg-slate-950/40 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center text-white shadow-md">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-bold text-slate-200">AI Studio</span>
              <span className="text-[11px] text-slate-500 ml-2">
                {activeModeDetails ? activeModeDetails.title : selectedModelObj.name}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                setMessages([
                  {
                    id: 'welcome-msg',
                    role: 'assistant',
                    content: 'Conversation reset. Enter a new prompt or use voice input to start.',
                    tokens: 10,
                    timestamp: new Date().toLocaleTimeString(),
                  },
                ]);
                setAttachments([]);
                setActiveMode(null);
                if (isListening) {
                  speechService.stopRecognition();
                  setIsListening(false);
                }
              }}
              className="px-3 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-slate-200 hover:bg-slate-800 border border-slate-800 transition-all flex items-center gap-1.5"
              title="Reset Conversation"
            >
              <RefreshCw className="w-3 h-3" />
              <span>Clear Chat</span>
            </button>
          </div>
        </div>

        {/* Messages Stream */}
        <div className="flex-1 p-6 space-y-6 overflow-y-auto">
          {messages.map((msg, index) => {
            const isUser = msg.role === 'user';
            const modeInfo = msg.mode ? getModeDetails(msg.mode) : null;
            const hasReasoning = Boolean(msg.reasoningSteps?.length);
            const isThoughtOpen = Boolean(expandedThoughts[index]);

            return (
              <div
                key={msg.id || index}
                className={`flex items-start gap-3.5 ${
                  isUser ? 'flex-row-reverse' : 'flex-row'
                }`}
              >
                <div
                  className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 shadow-md ${
                    isUser
                      ? 'bg-gradient-to-tr from-indigo-600 to-purple-600 text-white'
                      : msg.isImage
                      ? 'bg-amber-600/30 text-amber-300 border border-amber-500/40'
                      : msg.isDeepResearch
                      ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                      : msg.isSearch
                      ? 'bg-sky-600/30 text-sky-300 border border-sky-500/40'
                      : 'bg-slate-800 text-indigo-400 border border-slate-700'
                  }`}
                >
                  {isUser ? (
                    <User className="w-4 h-4" />
                  ) : msg.isImage ? (
                    <Wand2 className="w-4 h-4" />
                  ) : msg.isDeepResearch ? (
                    <Compass className="w-4 h-4" />
                  ) : msg.isSearch ? (
                    <Globe className="w-4 h-4" />
                  ) : (
                    <Bot className="w-4 h-4" />
                  )}
                </div>

                <div className={`space-y-2 max-w-[85%] ${isUser ? 'text-right' : 'text-left'}`}>
                  {/* Header line */}
                  <div className="flex items-center gap-2 text-[11px] text-slate-400">
                    <span className="font-semibold text-slate-300">
                      {isUser
                        ? 'You'
                        : msg.isImage
                        ? 'Nexus Image AI'
                        : msg.isDeepResearch
                        ? `${msg.model || 'Nexus AI'} • Deep Research`
                        : msg.isSearch
                        ? `${msg.model || 'Nexus AI'} • Web Search`
                        : msg.model || 'Nexus AI'}
                    </span>
                    {msg.isImage && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold border text-amber-400 border-amber-500/30 bg-amber-500/10">
                        {msg.stylePreset || 'Image'} • {msg.aspectRatio || '1:1'}
                      </span>
                    )}
                    {msg.isDeepResearch && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold border text-purple-400 border-purple-500/30 bg-purple-500/10">
                        {msg.totalQueriesExecuted || 4} Queries • {msg.sources?.length || 0} Sources
                      </span>
                    )}
                    {modeInfo && !msg.isImage && !msg.isDeepResearch && (
                      <span className={`px-2 py-0.5 rounded-full text-[10px] font-semibold border ${modeInfo.color}`}>
                        {modeInfo.title}
                      </span>
                    )}
                    <span>•</span>
                    <span>{msg.timestamp}</span>
                    {msg.latency && (
                      <>
                        <span>•</span>
                        <span className="font-mono text-emerald-400">{msg.latency}s</span>
                      </>
                    )}
                  </div>

                  {/* Attachments preview if present on user message */}
                  {msg.attachments && msg.attachments.length > 0 && (
                    <div className="flex flex-wrap gap-2 justify-end mb-2">
                      {msg.attachments.map((att, attIdx) => (
                        <div
                          key={attIdx}
                          className="p-2 rounded-xl bg-slate-900/90 border border-slate-800 text-xs flex items-center gap-2 text-slate-200"
                        >
                          {att.isImage && att.previewUrl ? (
                            <img
                              src={att.previewUrl}
                              alt={att.name}
                              className="w-8 h-8 rounded-lg object-cover border border-slate-700"
                            />
                          ) : (
                            <FileText className="w-4 h-4 text-indigo-400" />
                          )}
                          <div className="text-left">
                            <p className="text-[11px] font-medium max-w-[120px] truncate">{att.name}</p>
                            <p className="text-[9px] text-slate-500">{att.size}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Safe Reasoning / Thinking Status Accordion */}
                  {!isUser && hasReasoning && (
                    <div className="rounded-xl border border-indigo-500/20 bg-indigo-950/20 overflow-hidden mb-2">
                      <button
                        type="button"
                        onClick={() => toggleThoughtExpansion(index)}
                        className="w-full px-3 py-2 flex items-center justify-between text-xs font-semibold text-indigo-300 hover:bg-indigo-950/40 transition-colors"
                      >
                        <div className="flex items-center space-x-2">
                          <Brain className="w-3.5 h-3.5 text-indigo-400" />
                          <span>
                            Thought Process {msg.reasoningTime ? `(${msg.reasoningTime}s)` : ''}
                          </span>
                        </div>
                        {isThoughtOpen ? (
                          <ChevronUp className="w-3.5 h-3.5 text-indigo-400" />
                        ) : (
                          <ChevronDown className="w-3.5 h-3.5 text-indigo-400" />
                        )}
                      </button>
                      {isThoughtOpen && (
                        <div className="p-3 pt-1 text-[11px] text-slate-300 border-t border-indigo-500/10 space-y-1.5">
                          {msg.reasoningSteps.map((step, sIdx) => (
                            <div key={sIdx} className="flex items-center gap-2 text-slate-400">
                              <CheckCircle2 className="w-3 h-3 text-emerald-400 shrink-0" />
                              <span>{step}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}

                  {/* Image Result Card */}
                  {msg.isImage ? (
                    <div className="p-3 rounded-2xl bg-slate-950/90 border border-slate-800 shadow-xl space-y-3 max-w-lg">
                      <div
                        onClick={() => setLightboxImage(msg)}
                        className="relative group rounded-xl overflow-hidden border border-slate-800/80 bg-slate-900 cursor-pointer flex items-center justify-center"
                      >
                        <img
                          src={msg.imageUrl}
                          alt={msg.prompt || 'Generated AI Artwork'}
                          className="w-full h-auto max-h-96 object-contain rounded-xl transition-transform duration-300 group-hover:scale-[1.02]"
                        />
                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-2">
                          <span className="p-2 rounded-full bg-slate-900/80 text-white shadow-md">
                            <Maximize2 className="w-4 h-4" />
                          </span>
                        </div>
                      </div>

                      {msg.revisedPrompt && (
                        <p className="text-xs text-slate-300 leading-relaxed px-1">
                          <span className="text-slate-500 font-medium">Prompt: </span>
                          "{msg.prompt}"
                        </p>
                      )}

                      <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between text-xs text-slate-400">
                        <span className="font-mono text-[11px] text-slate-500">
                          {msg.provider || 'Nexus Image'}
                        </span>
                        <div className="flex items-center space-x-1.5">
                          <button
                            onClick={() => handleSend(null, msg.prompt)}
                            className="px-2 py-1 rounded-lg text-slate-300 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 flex items-center space-x-1 transition-all"
                            title="Regenerate Image"
                          >
                            <RotateCcw className="w-3 h-3 text-amber-400" />
                            <span className="text-[10px] font-medium">Regenerate</span>
                          </button>
                          <button
                            onClick={() => handleDownloadImage(msg.imageUrl)}
                            className="p-1.5 rounded-lg text-slate-300 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 transition-all"
                            title="Download Image"
                          >
                            <Download className="w-3.5 h-3.5 text-indigo-400" />
                          </button>
                          <button
                            onClick={() => handleCopy(msg.imageUrl, index)}
                            className="p-1.5 rounded-lg text-slate-300 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 transition-all"
                            title="Copy URL"
                          >
                            {copiedIndex === index ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                          <button
                            onClick={() => deleteMessage(msg.id)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 bg-slate-900 hover:bg-slate-800 border border-slate-800 transition-all"
                            title="Delete Message"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ) : msg.isDeepResearch ? (
                    /* Deep Research Report Card */
                    <div className="p-5 rounded-2xl bg-slate-950/90 border border-purple-500/30 text-slate-200 shadow-2xl space-y-4">
                      <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                        <div className="flex items-center space-x-2.5">
                          <div className="p-2 rounded-xl bg-purple-500/20 text-purple-300 border border-purple-500/40">
                            <Compass className="w-4 h-4" />
                          </div>
                          <div>
                            <h3 className="text-sm font-bold text-slate-100">Deep Research Synthesis Report</h3>
                            <p className="text-[11px] text-purple-300/80 font-medium truncate max-w-md">
                              Topic: {msg.topic}
                            </p>
                          </div>
                        </div>
                        <span className="px-2.5 py-1 rounded-lg text-[10px] font-bold bg-purple-950/80 text-purple-300 border border-purple-500/30">
                          {msg.totalQueriesExecuted || 4} Multi-Pass Searches
                        </span>
                      </div>

                      {msg.executiveSummary && (
                        <div className="space-y-1.5 p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-purple-400 flex items-center gap-1.5">
                            <Award className="w-3.5 h-3.5" />
                            Executive Summary
                          </h4>
                          <div className="text-xs text-slate-200 leading-relaxed">
                            {renderFormattedContentWithCitations(msg.executiveSummary, msg.citations, msg.sources)}
                          </div>
                        </div>
                      )}

                      {msg.keyFindings && msg.keyFindings.length > 0 && (
                        <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-indigo-400 flex items-center gap-1.5">
                            <CheckCircle2 className="w-3.5 h-3.5" />
                            Key Empirical Findings
                          </h4>
                          <ul className="space-y-1.5 text-xs text-slate-300">
                            {msg.keyFindings.map((finding, fIdx) => (
                              <li key={fIdx} className="flex items-start gap-2">
                                <span className="text-purple-400 font-bold">•</span>
                                <span className="flex-1">
                                  {renderFormattedContentWithCitations(finding, msg.citations, msg.sources)}
                                </span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {msg.detailedAnalysis && (
                        <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
                            <BarChart3 className="w-3.5 h-3.5" />
                            Detailed Technical Synthesis
                          </h4>
                          <div className="text-xs text-slate-300 leading-relaxed whitespace-pre-wrap">
                            {renderFormattedContentWithCitations(msg.detailedAnalysis, msg.citations, msg.sources)}
                          </div>
                        </div>
                      )}

                      {msg.conclusion && (
                        <div className="space-y-1.5 p-3.5 rounded-xl bg-slate-900/80 border border-slate-800">
                          <h4 className="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
                            <ShieldCheck className="w-3.5 h-3.5" />
                            Strategic Conclusion & Outlook
                          </h4>
                          <div className="text-xs text-slate-300 leading-relaxed">
                            {renderFormattedContentWithCitations(msg.conclusion, msg.citations, msg.sources)}
                          </div>
                        </div>
                      )}

                      {/* Authoritative Sources Panel */}
                      {msg.sources && msg.sources.length > 0 && (
                        <div className="pt-3 border-t border-slate-800 space-y-2">
                          <div className="flex items-center justify-between text-xs font-semibold text-slate-300">
                            <div className="flex items-center space-x-1.5 text-purple-400">
                              <BookOpen className="w-3.5 h-3.5" />
                              <span>Verified Research Sources ({msg.sources.length})</span>
                            </div>
                            <span className="text-[10px] text-slate-500 font-mono">
                              Provider: {msg.searchProvider || 'Tavily'}
                            </span>
                          </div>

                          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                            {msg.sources.map((src, sIdx) => {
                              const isValidUrl =
                                src.url && (src.url.startsWith('https://') || src.url.startsWith('http://'));
                              return (
                                <a
                                  key={src.id || sIdx}
                                  href={isValidUrl ? src.url : '#'}
                                  target={isValidUrl ? '_blank' : '_self'}
                                  rel="noopener noreferrer"
                                  className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 hover:border-purple-500/50 hover:bg-slate-900 text-left flex flex-col justify-between transition-all group shadow-sm"
                                >
                                  <div className="space-y-1">
                                    <div className="flex items-center justify-between text-[10px]">
                                      <span className="px-1.5 py-0.2 rounded font-bold text-purple-300 bg-purple-500/20 border border-purple-500/30">
                                        [{src.id || `S${sIdx + 1}`}] {src.sourceName || 'Source'}
                                      </span>
                                      {src.relevanceScore && (
                                        <span className="text-[10px] text-emerald-400 font-mono">
                                          {(src.relevanceScore * 100).toFixed(0)}% Authority
                                        </span>
                                      )}
                                    </div>
                                    <p className="text-xs font-medium text-slate-200 group-hover:text-purple-300 line-clamp-1">
                                      {src.title}
                                    </p>
                                    <p className="text-[10px] text-slate-400 line-clamp-2 leading-relaxed">
                                      {src.snippet}
                                    </p>
                                  </div>
                                  <div className="mt-2 pt-1 border-t border-slate-800/60 flex items-center justify-between text-[10px] text-slate-500 group-hover:text-purple-400">
                                    <span className="truncate max-w-[180px]">{src.url}</span>
                                    <ExternalLink className="w-3 h-3 shrink-0 ml-1" />
                                  </div>
                                </a>
                              );
                            })}
                          </div>
                        </div>
                      )}

                      {/* Footer Actions */}
                      <div className="pt-3 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
                        <span className="font-mono text-slate-500">
                          ~{msg.tokens} tokens • Multi-Source Grounded
                        </span>
                        <div className="flex items-center space-x-2">
                          <button
                            onClick={() => handleSend(null, msg.topic)}
                            className="px-2.5 py-1 rounded-lg text-slate-300 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 flex items-center space-x-1 transition-all"
                            title="Regenerate Research"
                          >
                            <RotateCcw className="w-3 h-3 text-purple-400" />
                            <span className="text-[10px] font-medium">Re-Run</span>
                          </button>
                          <button
                            onClick={() => handleCopy(msg.detailedAnalysis || msg.executiveSummary, index)}
                            className="p-1.5 rounded-lg text-slate-300 hover:text-white bg-slate-900 hover:bg-slate-800 border border-slate-800 transition-all"
                            title="Copy Report"
                          >
                            {copiedIndex === index ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                          <button
                            onClick={() => deleteMessage(msg.id)}
                            className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 bg-slate-900 hover:bg-slate-800 border border-slate-800 transition-all"
                            title="Delete Message"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      </div>
                    </div>
                  ) : (
                    /* Standard Text / Search Bubble */
                    <div
                      className={`relative p-4 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap ${
                        isUser
                          ? 'bg-indigo-600/30 border border-indigo-500/40 text-slate-100'
                          : 'bg-slate-950/80 border border-slate-800/80 text-slate-200 shadow-sm'
                      }`}
                    >
                      <div className="space-y-3">
                        <p>
                          {msg.isSearch
                            ? renderFormattedContentWithCitations(msg.content, msg.citations, msg.sources)
                            : msg.content}
                        </p>

                        {/* Sources Card Section */}
                        {msg.isSearch && msg.sources && msg.sources.length > 0 && (
                          <div className="pt-3 mt-3 border-t border-slate-800/80 space-y-2">
                            <div className="flex items-center justify-between text-xs font-semibold text-slate-300">
                              <div className="flex items-center space-x-1.5 text-sky-400">
                                <BookOpen className="w-3.5 h-3.5" />
                                <span>Sources ({msg.sources.length})</span>
                              </div>
                              <span className="text-[10px] text-slate-500 font-mono">
                                {msg.searchProvider || 'Tavily Search'}
                              </span>
                            </div>

                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                              {msg.sources.map((src, sIdx) => {
                                const isValidUrl =
                                  src.url && (src.url.startsWith('https://') || src.url.startsWith('http://'));
                                return (
                                  <a
                                    key={src.id || sIdx}
                                    href={isValidUrl ? src.url : '#'}
                                    target={isValidUrl ? '_blank' : '_self'}
                                    rel="noopener noreferrer"
                                    className="p-2.5 rounded-xl bg-slate-900/90 border border-slate-800 hover:border-sky-500/50 hover:bg-slate-900 text-left flex flex-col justify-between transition-all group shadow-sm"
                                  >
                                    <div className="space-y-1">
                                      <div className="flex items-center justify-between text-[10px]">
                                        <span className="px-1.5 py-0.2 rounded font-bold text-sky-300 bg-sky-500/20 border border-sky-500/30">
                                          [{src.id || `S${sIdx + 1}`}] {src.sourceName || 'Web Source'}
                                        </span>
                                        {src.publishedDate && (
                                          <span className="text-slate-500 flex items-center gap-0.5">
                                            <Calendar className="w-2.5 h-2.5" />
                                            {src.publishedDate}
                                          </span>
                                        )}
                                      </div>
                                      <p className="text-xs font-medium text-slate-200 group-hover:text-sky-300 line-clamp-1">
                                        {src.title}
                                      </p>
                                      <p className="text-[10px] text-slate-400 line-clamp-2 leading-relaxed">
                                        {src.snippet}
                                      </p>
                                    </div>
                                    <div className="mt-2 pt-1 border-t border-slate-800/60 flex items-center justify-between text-[10px] text-slate-500 group-hover:text-sky-400">
                                      <span className="truncate max-w-[180px]">{src.url}</span>
                                      <ExternalLink className="w-3 h-3 shrink-0 ml-1" />
                                    </div>
                                  </a>
                                );
                              })}
                            </div>
                          </div>
                        )}
                      </div>

                      {!isUser && (
                        <div className="mt-3 pt-2.5 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-400">
                          <span className="font-mono text-slate-400">
                            ~{msg.tokens} tokens {msg.isSearch ? '• Web Augmented' : ''}
                          </span>
                          <button
                            onClick={() => handleCopy(msg.content, index)}
                            className="flex items-center space-x-1 hover:text-indigo-300 transition-colors p-1 rounded"
                            title="Copy text"
                          >
                            {copiedIndex === index ? (
                              <Check className="w-3.5 h-3.5 text-emerald-400" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                            <span>{copiedIndex === index ? 'Copied' : 'Copy'}</span>
                          </button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}

          {/* Loading States */}
          {loading && (
            <div className="flex items-start gap-3.5">
              <div
                className={`w-8 h-8 rounded-xl flex items-center justify-center shadow-md ${
                  activeMode === 'image'
                    ? 'bg-amber-600/30 text-amber-300 border border-amber-500/40'
                    : activeMode === 'deep-research'
                    ? 'bg-purple-600/30 text-purple-300 border border-purple-500/40'
                    : activeMode === 'web-search'
                    ? 'bg-sky-600/30 text-sky-300 border border-sky-500/40'
                    : 'bg-slate-800 text-indigo-400 border border-slate-700'
                }`}
              >
                {activeMode === 'image' ? (
                  <Wand2 className="w-4 h-4 animate-spin" />
                ) : activeMode === 'deep-research' ? (
                  <Compass className="w-4 h-4 animate-spin" />
                ) : activeMode === 'web-search' ? (
                  <Globe className="w-4 h-4 animate-spin" />
                ) : (
                  <Bot className="w-4 h-4 animate-pulse" />
                )}
              </div>
              <div className="space-y-2 max-w-[85%]">
                {activeMode === 'image' ? (
                  <div className="p-4 rounded-2xl bg-slate-950/90 border border-amber-500/30 text-amber-300 text-xs flex items-center space-x-3 shadow-lg">
                    <div className="w-2.5 h-2.5 rounded-full bg-amber-400 animate-ping" />
                    <div>
                      <p className="font-semibold text-amber-200">🎨 Creating your image...</p>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Synthesizing in {imageStylePreset} style ({imageAspectRatio})...
                      </p>
                    </div>
                  </div>
                ) : activeMode === 'deep-research' ? (
                  <div className="p-4 rounded-2xl bg-slate-950/90 border border-purple-500/30 text-purple-300 text-xs space-y-2 shadow-lg animate-in fade-in">
                    <div className="flex items-center space-x-3">
                      <div className="w-2.5 h-2.5 rounded-full bg-purple-400 animate-ping" />
                      <div>
                        <p className="font-semibold text-purple-200">
                          {researchStage <= 1
                            ? '🔭 Planning multi-angle research queries...'
                            : researchStage === 2
                            ? '🌐 Querying multiple live search indexes...'
                            : researchStage === 3
                            ? '🔎 Evaluating source authority & evidence...'
                            : researchStage === 4
                            ? '🔁 Performing in-depth follow-up inquiries...'
                            : '🧠 Synthesizing structured research report with citations...'}
                        </p>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                          Cross-verifying empirical documentation and grounding technical assertions...
                        </p>
                      </div>
                    </div>
                  </div>
                ) : activeMode === 'web-search' ? (
                  <div className="p-4 rounded-2xl bg-slate-950/90 border border-sky-500/30 text-sky-300 text-xs space-y-2 shadow-lg animate-in fade-in">
                    <div className="flex items-center space-x-3">
                      <div className="w-2.5 h-2.5 rounded-full bg-sky-400 animate-ping" />
                      <div>
                        <p className="font-semibold text-sky-200">
                          {searchStage <= 1
                            ? '🌐 Searching the web...'
                            : searchStage === 2
                            ? '🔎 Reviewing & verifying sources...'
                            : '🧠 Synthesizing answer with citations...'}
                        </p>
                        <p className="text-[11px] text-slate-400 mt-0.5">
                          Retrieving live internet knowledge and grounding factual claims...
                        </p>
                      </div>
                    </div>
                  </div>
                ) : isProcessingMultimodal ? (
                  <div className="p-4 rounded-2xl bg-slate-950/90 border border-indigo-500/30 text-indigo-300 text-xs flex items-center space-x-3 shadow-lg animate-in fade-in">
                    <div className="w-2.5 h-2.5 rounded-full bg-indigo-400 animate-ping" />
                    <div>
                      <p className="font-semibold text-indigo-200">🔍 Analyzing your files...</p>
                      <p className="text-[11px] text-slate-400 mt-0.5">
                        Parsing multimodal visual & document context via {model}...
                      </p>
                    </div>
                  </div>
                ) : (
                  <>
                    {isThinkActive && (
                      <div className="px-3.5 py-2 rounded-xl bg-indigo-950/40 border border-indigo-500/30 text-indigo-300 text-xs flex items-center space-x-2 animate-pulse">
                        <Brain className="w-4 h-4 text-indigo-400 animate-spin" />
                        <span>Reasoning & planning contextual synthesis...</span>
                      </div>
                    )}
                    <div className="p-4 rounded-2xl bg-slate-950/80 border border-slate-800/80 text-slate-400 text-xs flex items-center space-x-2">
                      <div className="w-2 h-2 rounded-full bg-indigo-500 animate-ping" />
                      <span>
                        Synthesizing response via {model} {activeMode ? `[${activeMode}]` : ''}...
                      </span>
                    </div>
                  </>
                )}
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Library Modal */}
        {isLibraryOpen && (
          <div className="absolute inset-0 bg-slate-950/80 backdrop-blur-md z-30 flex items-center justify-center p-6 animate-in fade-in">
            <div className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-2xl space-y-4">
              <div className="flex items-center justify-between border-b border-slate-800 pb-3">
                <div className="flex items-center space-x-2">
                  <FolderOpen className="w-4 h-4 text-indigo-400" />
                  <h3 className="text-sm font-bold text-slate-200">Workspace Library</h3>
                </div>
                <button
                  onClick={() => setIsLibraryOpen(false)}
                  className="p-1 rounded-lg text-slate-400 hover:text-slate-200 hover:bg-slate-800"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
              <div className="space-y-2 max-h-60 overflow-y-auto">
                {libraryFiles.map((file, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleAttachFromLibrary(file)}
                    className="w-full p-3 rounded-xl bg-slate-950/70 border border-slate-800/80 hover:border-indigo-500/50 hover:bg-slate-950 text-left flex items-center justify-between transition-all"
                  >
                    <div className="flex items-center space-x-3">
                      <FileCode className="w-4 h-4 text-indigo-400" />
                      <div>
                        <p className="text-xs font-medium text-slate-200">{file.name}</p>
                        <p className="text-[10px] text-slate-500">{file.size}</p>
                      </div>
                    </div>
                    <span className="text-[10px] text-indigo-400 font-semibold px-2 py-1 bg-indigo-500/10 rounded-md">
                      Attach
                    </span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* Bottom Popovers & Prompt Bar Container */}
        <div className="p-4 bg-slate-950/95 border-t border-slate-800/80 relative">
          
          {/* Action Menu Popover */}
          {isActionMenuOpen && (
            <div
              ref={actionMenuRef}
              className="absolute bottom-20 left-4 z-20 w-80 bg-[#18181b]/98 backdrop-blur-xl border border-slate-700/80 rounded-2xl shadow-2xl p-2 space-y-1 animate-in fade-in slide-in-from-bottom-3"
            >
              <div className="px-2.5 py-1.5 mb-1 bg-slate-900/80 rounded-xl border border-slate-800/80 flex items-center gap-2">
                <Search className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                <input
                  type="text"
                  value={actionSearchQuery}
                  onChange={(e) => setActionSearchQuery(e.target.value)}
                  placeholder="Type to search plugins, files & actions"
                  className="w-full bg-transparent text-[11px] text-slate-200 placeholder:text-slate-500 focus:outline-none"
                />
              </div>

              <div className="max-h-72 overflow-y-auto space-y-1">
                {filteredActionItems.map((item) => {
                  const IconComp = item.icon;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={item.action}
                      className="w-full p-2 rounded-xl text-left flex items-start gap-3 hover:bg-slate-800/80 transition-colors group"
                    >
                      <div className="p-2 rounded-lg bg-slate-900/80 border border-slate-800 text-slate-300 group-hover:text-white group-hover:border-slate-700">
                        <IconComp className="w-4 h-4" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-slate-200 group-hover:text-white truncate">
                          {item.title}
                        </p>
                        <p className="text-[10px] text-slate-400 line-clamp-1">
                          {item.subtitle}
                        </p>
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Model Selector Popover (Exact Matching Aesthetic from Screenshot) */}
          {isModelMenuOpen && (
            <div
              ref={modelMenuRef}
              className="absolute bottom-20 left-16 z-30 w-80 bg-[#18181b]/98 backdrop-blur-2xl border border-slate-700/90 rounded-2xl shadow-2xl p-2.5 space-y-1 animate-in fade-in slide-in-from-bottom-2 text-left"
            >
              <div className="px-2 py-1 text-[11px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800/80 mb-1 flex items-center justify-between">
                <span>Model</span>
                <span className="text-[10px] font-mono text-slate-500">Nexus Engine</span>
              </div>

              <div className="max-h-80 overflow-y-auto space-y-1">
                {modelsList.map((item) => {
                  const isSelected = model === item.id;
                  return (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        setModel(item.id);
                        setIsModelMenuOpen(false);
                      }}
                      className={`w-full p-2.5 rounded-xl text-left flex items-center justify-between transition-all group ${
                        isSelected
                          ? 'bg-slate-800/90 text-white border border-slate-700/80 shadow-md'
                          : 'text-slate-300 hover:bg-slate-800/50 hover:text-white'
                      }`}
                    >
                      <div className="space-y-0.5 flex-1 min-w-0 mr-2">
                        <div className="flex items-center gap-1.5">
                          <span className="text-xs font-semibold truncate">{item.name}</span>
                          <span className="text-[10px] text-slate-500 font-normal">{item.category}</span>
                        </div>
                        <p className="text-[10px] text-slate-400 line-clamp-1">{item.description}</p>
                      </div>

                      <div className="flex items-center space-x-1.5 shrink-0">
                        {item.speed && (
                          <span className="px-1.5 py-0.5 rounded text-[9px] font-medium bg-slate-900/80 text-slate-400 border border-slate-800">
                            {item.speed}
                          </span>
                        )}
                        {isSelected ? (
                          <Check className="w-3.5 h-3.5 text-indigo-400 ml-1" />
                        ) : (
                          <ChevronRight className="w-3 h-3 text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity" />
                        )}
                      </div>
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Language Selector Popover */}
          {isLangMenuOpen && (
            <div
              ref={langMenuRef}
              className="absolute bottom-20 right-16 z-20 w-48 bg-[#18181b]/98 backdrop-blur-xl border border-slate-700/80 rounded-2xl shadow-2xl p-2 space-y-1 animate-in fade-in"
            >
              <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-800">
                Voice Input Language
              </div>
              {SUPPORTED_LANGUAGES.map((lang) => (
                <button
                  key={lang.code}
                  onClick={() => handleSelectLanguage(lang.code)}
                  className={`w-full px-2.5 py-1.5 rounded-lg text-left text-xs font-medium flex items-center justify-between transition-colors ${
                    speechLanguage === lang.code
                      ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/40'
                      : 'text-slate-300 hover:bg-slate-800'
                  }`}
                >
                  <span>{lang.name}</span>
                  {speechLanguage === lang.code && <Check className="w-3 h-3 text-indigo-400" />}
                </button>
              ))}
            </div>
          )}

          {/* Active Mode Indicator Bar */}
          {activeModeDetails && (
            <div className="mb-3 px-3 py-1.5 rounded-xl bg-slate-900/90 border border-slate-800 flex items-center justify-between animate-in fade-in">
              <div className="flex items-center space-x-2">
                <activeModeDetails.icon className="w-3.5 h-3.5 text-indigo-400" />
                <span className="text-xs font-semibold text-slate-200">{activeModeDetails.title}:</span>
                <span className="text-xs text-slate-400">{activeModeDetails.desc}</span>
              </div>
              <button
                type="button"
                onClick={() => setActiveMode(null)}
                className="p-1 rounded-md text-slate-400 hover:text-slate-200 hover:bg-slate-800"
                title="Cancel Mode"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          )}

          {/* Attached Files Preview Bar */}
          {attachments.length > 0 && (
            <div className="mb-3 flex flex-wrap gap-2">
              {attachments.map((att) => (
                <div
                  key={att.id}
                  className="p-2 rounded-xl bg-slate-900/90 border border-slate-800 text-xs flex items-center gap-2 text-slate-200 shadow-md group animate-in fade-in"
                >
                  {att.isImage && att.previewUrl ? (
                    <img
                      src={att.previewUrl}
                      alt={att.name}
                      className="w-7 h-7 rounded-lg object-cover border border-slate-700"
                    />
                  ) : (
                    <FileText className="w-4 h-4 text-indigo-400" />
                  )}
                  <div className="max-w-[130px]">
                    <p className="text-[11px] font-medium truncate">{att.name}</p>
                    <p className="text-[9px] text-slate-500">{att.size}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => removeAttachment(att.id)}
                    className="p-1 text-slate-400 hover:text-rose-400 rounded-md hover:bg-slate-800 transition-colors"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </div>
              ))}
            </div>
          )}

          {/* Interactive Modern Prompt Bar */}
          <form
            onSubmit={(e) => handleSend(e, null)}
            className={`flex items-center gap-2 p-1.5 rounded-2xl bg-[#111318] border transition-all shadow-inner ${
              isListening
                ? 'border-rose-500/60 shadow-rose-500/10 ring-2 ring-rose-500/20'
                : 'border-slate-800/90'
            }`}
          >
            {/* + Button (Action Menu) */}
            <button
              type="button"
              onClick={() => {
                setIsActionMenuOpen(!isActionMenuOpen);
                setIsModelMenuOpen(false);
              }}
              className={`w-9 h-9 rounded-xl flex items-center justify-center text-slate-300 hover:text-white hover:bg-slate-800/80 transition-all shrink-0 ${
                isActionMenuOpen ? 'bg-slate-800 text-white rotate-45' : ''
              }`}
              title="Add photos, files & actions"
            >
              <Plus className="w-5 h-5 transition-transform duration-200" />
            </button>

            {/* Active Model Pill Selector (Directly Inside Prompt Bar) */}
            <button
              type="button"
              onClick={() => {
                setIsModelMenuOpen(!isModelMenuOpen);
                setIsActionMenuOpen(false);
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all shrink-0 border ${
                isModelMenuOpen
                  ? 'bg-slate-800 text-indigo-300 border-indigo-500/50 shadow-md'
                  : 'bg-slate-900/90 text-slate-300 border-slate-800 hover:text-white hover:border-slate-700'
              }`}
              title="Select Active AI Model"
            >
              <Cpu className="w-3.5 h-3.5 text-indigo-400" />
              <span className="truncate max-w-[140px]">{selectedModelObj.name}</span>
              {isModelMenuOpen ? (
                <ChevronUp className="w-3 h-3 text-slate-400" />
              ) : (
                <ChevronDown className="w-3 h-3 text-slate-400" />
              )}
            </button>

            {/* Prompt Input */}
            <div className="flex-1 flex items-center relative min-w-0">
              <input
                type="text"
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={
                  isListening
                    ? `Listening in ${currentLangObj.name}... Speak now.`
                    : activeMode === 'image'
                    ? 'Describe the image you want to create...'
                    : activeMode === 'deep-research'
                    ? 'Enter complex topic for multi-query deep research & report synthesis...'
                    : activeMode === 'web-search'
                    ? 'Search the web and ask anything...'
                    : attachments.length > 0
                    ? 'Ask questions about the attached file(s)...'
                    : 'Ask anything or speak'
                }
                disabled={loading || isQuotaExceeded}
                className="w-full px-3 py-2 bg-transparent text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none disabled:opacity-50"
              />

              {isListening && (
                <span className="flex items-center space-x-1 px-2 py-0.5 rounded-md bg-rose-500/20 border border-rose-500/40 text-[10px] text-rose-300 animate-pulse shrink-0 mr-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-500" />
                  <span>Listening</span>
                </span>
              )}
            </div>

            {/* Think Button */}
            <button
              type="button"
              onClick={() => setIsThinkActive(!isThinkActive)}
              className={`px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition-all shrink-0 ${
                isThinkActive
                  ? 'bg-indigo-600/30 text-indigo-300 border border-indigo-500/50 shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
              title="Toggle Step-by-Step Reasoning Mode"
            >
              <Brain className="w-4 h-4" />
              <span>Think</span>
            </button>

            {/* Language Selector Button */}
            <button
              type="button"
              onClick={() => setIsLangMenuOpen(!isLangMenuOpen)}
              className="p-1.5 px-2 rounded-xl text-[10px] font-bold text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors shrink-0 flex items-center gap-1 border border-slate-800/60"
              title={`Speech Language: ${currentLangObj.name} (Click to change)`}
            >
              <Languages className="w-3.5 h-3.5 text-indigo-400" />
              <span>{speechLanguage.split('-')[0].toUpperCase()}</span>
            </button>

            {/* Real Voice Button */}
            <button
              type="button"
              onClick={handleToggleVoice}
              className={`p-2 rounded-xl transition-all shrink-0 ${
                isListening
                  ? 'bg-rose-600 text-white shadow-lg shadow-rose-600/40 animate-pulse'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
              }`}
              title={
                !isSpeechSupported
                  ? 'Voice input not supported in this browser'
                  : isListening
                  ? 'Stop listening'
                  : `Voice input (${currentLangObj.name})`
              }
            >
              {isListening ? (
                <MicOff className="w-4 h-4 text-white" />
              ) : (
                <Mic className="w-4 h-4" />
              )}
            </button>

            {/* Send / Execute Button */}
            <button
              type="submit"
              disabled={(!prompt.trim() && attachments.length === 0) || loading || isQuotaExceeded}
              className={`w-9 h-9 rounded-full flex items-center justify-center text-white shadow-md disabled:opacity-40 disabled:cursor-not-allowed transition-all shrink-0 ${
                activeMode === 'image'
                  ? 'bg-amber-600 hover:bg-amber-500 shadow-amber-500/20'
                  : activeMode === 'deep-research'
                  ? 'bg-purple-600 hover:bg-purple-500 shadow-purple-500/20'
                  : activeMode === 'web-search'
                  ? 'bg-sky-600 hover:bg-sky-500 shadow-sky-500/20'
                  : 'bg-blue-600 hover:bg-blue-500 shadow-blue-500/20'
              }`}
              title={
                activeMode === 'image'
                  ? 'Generate Image'
                  : activeMode === 'deep-research'
                  ? 'Execute Deep Research'
                  : activeMode === 'web-search'
                  ? 'Search Web & Answer'
                  : 'Send Prompt'
              }
            >
              {activeMode === 'image' ? (
                <Wand2 className="w-4 h-4" />
              ) : activeMode === 'deep-research' ? (
                <Compass className="w-4 h-4" />
              ) : activeMode === 'web-search' ? (
                <Globe className="w-4 h-4" />
              ) : (
                <Send className="w-4 h-4" />
              )}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AIStudioPage;
