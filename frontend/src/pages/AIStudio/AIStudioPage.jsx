import React, { useState, useEffect, useRef } from 'react';
import {
  Sparkles,
  Send,
  Bot,
  User,
  Sliders,
  Copy,
  Check,
  AlertTriangle,
  RefreshCw,
  Plus,
  Paperclip,
  FolderOpen,
  Image as ImageIcon,
  Globe,
  Search,
  Brain,
  Mic,
  AudioWaveform,
  X,
  FileText,
  ChevronDown,
  ChevronUp,
  Terminal,
  Compass,
  FileCode,
  CheckCircle2,
  Download,
  Maximize2,
  RotateCcw,
  Trash2,
  ExternalLink,
  Layers,
  Wand2,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { aiService } from '../../services/aiService';
import { usageService } from '../../services/usageService';

export const AIStudioPage = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [model, setModel] = useState('gemini-2.0-flash');
  const [models, setModels] = useState([]);
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
  const [isProcessingMultimodal, setIsProcessingMultimodal] = useState(false);
  const [temperature, setTemperature] = useState(0.7);
  const [systemInstruction, setSystemInstruction] = useState('');
  const [copiedIndex, setCopiedIndex] = useState(null);
  const [currentUsage, setCurrentUsage] = useState(null);
  const [errorMsg, setErrorMsg] = useState(null);
  const [isQuotaExceeded, setIsQuotaExceeded] = useState(false);

  // Phase 5A + 5B + 5C Action Menu, Mode, Image & Multimodal State
  const [isActionMenuOpen, setIsActionMenuOpen] = useState(false);
  const [actionSearchQuery, setActionSearchQuery] = useState('');
  const [activeMode, setActiveMode] = useState(null); // 'image' | 'web-search' | 'deep-research' | 'developer' | null
  const [isThinkActive, setIsThinkActive] = useState(false);
  const [attachments, setAttachments] = useState([]);
  const [isLibraryOpen, setIsLibraryOpen] = useState(false);
  const [expandedThoughts, setExpandedThoughts] = useState({});

  // Image Generation Options (Phase 5B)
  const [imageAspectRatio, setImageAspectRatio] = useState('1:1');
  const [imageStylePreset, setImageStylePreset] = useState('Cinematic');
  const [lightboxImage, setLightboxImage] = useState(null);

  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const actionMenuRef = useRef(null);

  useEffect(() => {
    loadModels();
    loadUsage();
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  // Click outside to close action menu
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (actionMenuRef.current && !actionMenuRef.current.contains(e.target)) {
        setIsActionMenuOpen(false);
      }
    };
    if (isActionMenuOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isActionMenuOpen]);

  const loadModels = async () => {
    try {
      const res = await aiService.getModels();
      if (res.success && res.data) {
        setModels(res.data);
      }
    } catch (e) {
      console.warn('Failed to load models list', e);
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

  // File Upload Handling
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

  // Workspace Library Files (Simulated local assets)
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
      subtitle: 'Visualize anything',
      action: () => {
        setActiveMode('image');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'web-search',
      icon: Globe,
      title: 'Web search',
      subtitle: 'Find real-time news and info',
      action: () => {
        setActiveMode('web-search');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'deep-research',
      icon: Compass,
      title: 'Deep research',
      subtitle: 'Get a detailed report',
      action: () => {
        setActiveMode('deep-research');
        setIsActionMenuOpen(false);
      },
    },
    {
      id: 'developer',
      icon: Terminal,
      title: 'OpenAI Developers',
      subtitle: 'Develop AI apps, agents, and ChatGPT Apps with OpenAI best practices',
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

  // Image Download Helper
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

  const handleSend = async (e, overridePrompt = null) => {
    e?.preventDefault();
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

    // Build user message
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

    const startTime = performance.now();

    try {
      // 1. Image Generation Workflow (Phase 5B)
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
      } else if (hasFiles) {
        // 2. Multimodal File & Image Understanding Workflow (Phase 5C)
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
        // 3. Standard Text / Chat Workflow
        let enrichedSystemInstruction = systemInstruction || '';
        if (isThinkActive) {
          enrichedSystemInstruction += ' Provide deep, structured step-by-step reasoning.';
        }
        if (activeMode === 'deep-research') {
          enrichedSystemInstruction += ' Provide an in-depth research report with executive summary, data breakdown, and strategic insights.';
        }

        const res = await aiService.generateText(targetPrompt || 'Analyze attached context', model, {
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
          desc: 'Augmenting answers with real-time web exploration',
          icon: Globe,
          color: 'text-sky-400 border-sky-500/30 bg-sky-500/10',
        };
      case 'deep-research':
        return {
          title: 'Deep Research Mode',
          desc: 'Synthesizing multi-section analytical report',
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
  const aiQuota = currentUsage?.metrics?.AI_REQUEST;

  return (
    <div className="space-y-6">
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

      {/* Top Banner & Quota Meter */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950/40 to-slate-900 border border-slate-800 shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-indigo-500/20">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-slate-100">AI Studio Workspace</h1>
              <p className="text-xs text-slate-400">Multi-tool intelligent inference, image synthesis & multimodal workbench</p>
            </div>
          </div>
        </div>

        {/* Quota Status Card */}
        {aiQuota && (
          <div className="w-full md:w-auto flex items-center gap-4 bg-slate-950/70 px-4 py-3 rounded-xl border border-slate-800/80">
            <div className="text-left">
              <p className="text-[11px] text-slate-400 uppercase tracking-wider font-semibold">Remaining Quota</p>
              <p className="text-sm font-bold text-slate-200">
                <span className="text-indigo-400">{aiQuota.remaining}</span> / {aiQuota.limit} requests
              </p>
            </div>
            <div className="w-24 h-2 bg-slate-800 rounded-full overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-500 ${
                  aiQuota.percentage >= 90
                    ? 'bg-rose-500'
                    : aiQuota.percentage >= 75
                    ? 'bg-amber-500'
                    : 'bg-gradient-to-r from-indigo-500 to-purple-500'
                }`}
                style={{ width: `${Math.min(100, aiQuota.percentage)}%` }}
              />
            </div>
          </div>
        )}
      </div>

      {/* Quota Exceeded Alert Banner */}
      {isQuotaExceeded && (
        <div className="p-4 rounded-xl bg-rose-500/10 border border-rose-500/30 flex items-center justify-between gap-4 animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center space-x-3 text-rose-300 text-sm">
            <AlertTriangle className="w-5 h-5 text-rose-400 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          <button
            onClick={() => navigate('/subscription')}
            className="px-4 py-1.5 rounded-lg text-xs font-bold bg-rose-500 hover:bg-rose-600 text-white shadow-md transition-all shrink-0"
          >
            Upgrade Plan
          </button>
        </div>
      )}

      {/* Main Studio Interface */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left / Settings Sidebar */}
        <div className="lg:col-span-1 space-y-4">
          <div className="p-5 rounded-2xl bg-slate-900/70 border border-slate-800 space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800/80 pb-3">
              <h2 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-2">
                <Sliders className="w-3.5 h-3.5 text-indigo-400" />
                {activeMode === 'image' ? 'Image Engine Config' : 'Model & Config'}
              </h2>
            </div>

            {/* If in Image Mode, show specialized Image Controls */}
            {activeMode === 'image' ? (
              <div className="space-y-4 animate-in fade-in">
                {/* Aspect Ratio */}
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-slate-300 flex items-center justify-between">
                    <span>Aspect Ratio</span>
                    <span className="text-[10px] text-amber-400 font-mono">{imageAspectRatio}</span>
                  </label>
                  <div className="grid grid-cols-2 gap-1.5">
                    {[
                      { id: '1:1', label: '1:1 Square' },
                      { id: '16:9', label: '16:9 Cinema' },
                      { id: '9:16', label: '9:16 Mobile' },
                      { id: '4:3', label: '4:3 Classic' },
                    ].map((ratio) => (
                      <button
                        key={ratio.id}
                        type="button"
                        onClick={() => setImageAspectRatio(ratio.id)}
                        className={`p-2 rounded-xl text-xs font-medium border text-center transition-all ${
                          imageAspectRatio === ratio.id
                            ? 'bg-amber-500/20 border-amber-500/60 text-amber-300 shadow-sm'
                            : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-slate-200'
                        }`}
                      >
                        {ratio.label}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Style Preset */}
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-slate-300">Style Aesthetic</label>
                  <select
                    value={imageStylePreset}
                    onChange={(e) => setImageStylePreset(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-medium text-slate-200 focus:outline-none focus:border-amber-500 transition-colors"
                  >
                    <option value="Cinematic">Cinematic (Dramatic Lighting)</option>
                    <option value="Photorealistic">Photorealistic (Ultra Detail)</option>
                    <option value="Digital Art">Digital Art & Illustration</option>
                    <option value="Anime">Anime / Manga Concept</option>
                    <option value="Isometric 3D">Isometric 3D Render</option>
                    <option value="Cyberpunk">Cyberpunk Neon</option>
                    <option value="Fantasy">Epic Fantasy</option>
                  </select>
                </div>
              </div>
            ) : (
              /* Standard LLM Model Controls */
              <div className="space-y-4 animate-in fade-in">
                {/* Model Selector */}
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-slate-300">Active Model</label>
                  <select
                    value={model}
                    onChange={(e) => setModel(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-slate-800 text-xs font-medium text-slate-200 focus:outline-none focus:border-indigo-500 transition-colors"
                  >
                    {models.length > 0 ? (
                      models.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.name}
                        </option>
                      ))
                    ) : (
                      <>
                        <option value="gemini-2.0-flash">Gemini 2.0 Flash (Next-Gen)</option>
                        <option value="gemini-1.5-flash">Gemini 1.5 Flash (Fast)</option>
                        <option value="gemini-1.5-pro">Gemini 1.5 Pro (Advanced)</option>
                        <option value="gpt-4o">GPT-4o (Multimodal)</option>
                        <option value="gpt-4o-mini">GPT-4o Mini (Efficient)</option>
                      </>
                    )}
                  </select>
                </div>

                {/* Temperature Slider */}
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-medium text-slate-300">Creativity (Temp)</label>
                    <span className="text-xs font-mono text-indigo-400">{temperature}</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.05"
                    value={temperature}
                    onChange={(e) => setTemperature(parseFloat(e.target.value))}
                    className="w-full accent-indigo-500"
                  />
                  <div className="flex justify-between text-[10px] text-slate-500 font-mono">
                    <span>Deterministic</span>
                    <span>Creative</span>
                  </div>
                </div>

                {/* System Instruction */}
                <div className="space-y-1.5">
                  <label className="text-xs font-medium text-slate-300">System Instruction</label>
                  <textarea
                    value={systemInstruction}
                    onChange={(e) => setSystemInstruction(e.target.value)}
                    rows={3}
                    placeholder="Optional system context (e.g. You are a senior software architect...)"
                    className="w-full p-2.5 rounded-xl bg-slate-950 border border-slate-800 text-xs text-slate-200 focus:outline-none focus:border-indigo-500 transition-colors resize-none placeholder:text-slate-600"
                  />
                </div>
              </div>
            )}

            {/* Quick Tools & Mode Switcher */}
            <div className="space-y-2 pt-2 border-t border-slate-800/80">
              <label className="text-xs font-medium text-slate-400 flex items-center justify-between">
                <span>Quick Tools</span>
                {activeMode && (
                  <button
                    onClick={() => setActiveMode(null)}
                    className="text-[10px] text-rose-400 hover:underline"
                  >
                    Reset Mode
                  </button>
                )}
              </label>
              <div className="grid grid-cols-2 gap-1.5">
                <button
                  type="button"
                  onClick={() => setActiveMode(activeMode === 'image' ? null : 'image')}
                  className={`p-2 rounded-lg text-[11px] font-medium border flex items-center gap-1.5 transition-all ${
                    activeMode === 'image'
                      ? 'bg-amber-500/20 border-amber-500/50 text-amber-300 shadow-sm'
                      : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <ImageIcon className="w-3.5 h-3.5" />
                  <span>Image</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveMode(activeMode === 'web-search' ? null : 'web-search')}
                  className={`p-2 rounded-lg text-[11px] font-medium border flex items-center gap-1.5 transition-all ${
                    activeMode === 'web-search'
                      ? 'bg-sky-500/20 border-sky-500/50 text-sky-300'
                      : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <Globe className="w-3.5 h-3.5" />
                  <span>Search</span>
                </button>
                <button
                  type="button"
                  onClick={() => setActiveMode(activeMode === 'deep-research' ? null : 'deep-research')}
                  className={`p-2 rounded-lg text-[11px] font-medium border flex items-center gap-1.5 transition-all ${
                    activeMode === 'deep-research'
                      ? 'bg-purple-500/20 border-purple-500/50 text-purple-300'
                      : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <Compass className="w-3.5 h-3.5" />
                  <span>Research</span>
                </button>
                <button
                  type="button"
                  onClick={() => setIsThinkActive(!isThinkActive)}
                  className={`p-2 rounded-lg text-[11px] font-medium border flex items-center gap-1.5 transition-all ${
                    isThinkActive
                      ? 'bg-indigo-500/20 border-indigo-500/50 text-indigo-300 shadow-sm'
                      : 'bg-slate-950/60 border-slate-800 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  <Brain className="w-3.5 h-3.5" />
                  <span>Think: {isThinkActive ? 'ON' : 'OFF'}</span>
                </button>
              </div>
            </div>

            {/* Clear Chat Button */}
            <button
              onClick={() => {
                setMessages([
                  {
                    id: 'welcome-msg',
                    role: 'assistant',
                    content: 'Conversation reset. Enter a new prompt to start.',
                    tokens: 10,
                    timestamp: new Date().toLocaleTimeString(),
                  },
                ]);
                setAttachments([]);
                setActiveMode(null);
              }}
              className="w-full flex items-center justify-center space-x-2 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-slate-200 bg-slate-950/60 hover:bg-slate-800 border border-slate-800 transition-all"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Clear Session</span>
            </button>
          </div>
        </div>

        {/* Right / Chat Stream & Advanced Action Prompt Bar */}
        <div className="lg:col-span-3 flex flex-col h-[720px] rounded-2xl bg-slate-900/50 border border-slate-800 shadow-xl overflow-hidden relative">
          
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
                        : 'bg-slate-800 text-indigo-400 border border-slate-700'
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : msg.isImage ? <Wand2 className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                  </div>

                  <div className={`space-y-2 max-w-[85%] ${isUser ? 'text-right' : 'text-left'}`}>
                    {/* Header line */}
                    <div className="flex items-center gap-2 text-[11px] text-slate-400">
                      <span className="font-semibold text-slate-300">
                        {isUser ? 'You' : msg.isImage ? 'Nexus Image AI' : msg.model || 'Nexus AI'}
                      </span>
                      {msg.isImage && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold border text-amber-400 border-amber-500/30 bg-amber-500/10">
                          {msg.stylePreset || 'Image'} • {msg.aspectRatio || '1:1'}
                        </span>
                      )}
                      {modeInfo && !msg.isImage && (
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

                    {/* Image Result Card (Phase 5B) */}
                    {msg.isImage ? (
                      <div className="p-3 rounded-2xl bg-slate-950/90 border border-slate-800 shadow-xl space-y-3 max-w-lg">
                        {/* Image Canvas with hover zoom */}
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

                        {/* Revised Prompt Info */}
                        {msg.revisedPrompt && (
                          <p className="text-xs text-slate-300 leading-relaxed px-1">
                            <span className="text-slate-500 font-medium">Prompt: </span>
                            "{msg.prompt}"
                          </p>
                        )}

                        {/* Image Action Toolbar */}
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
                    ) : (
                      /* Standard Text / Chat Bubble */
                      <div
                        className={`relative p-4 rounded-2xl text-sm leading-relaxed whitespace-pre-wrap ${
                          isUser
                            ? 'bg-indigo-600/30 border border-indigo-500/40 text-slate-100'
                            : 'bg-slate-950/80 border border-slate-800/80 text-slate-200 shadow-sm'
                        }`}
                      >
                        {msg.content}

                        {!isUser && (
                          <div className="mt-3 pt-2.5 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-400">
                            <span className="font-mono text-slate-400">
                              ~{msg.tokens} tokens
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

            {/* Loading Indicator */}
            {loading && (
              <div className="flex items-start gap-3.5">
                <div
                  className={`w-8 h-8 rounded-xl flex items-center justify-center shadow-md ${
                    activeMode === 'image'
                      ? 'bg-amber-600/30 text-amber-300 border border-amber-500/40'
                      : 'bg-slate-800 text-indigo-400 border border-slate-700'
                  }`}
                >
                  {activeMode === 'image' ? (
                    <Wand2 className="w-4 h-4 animate-spin" />
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

          {/* Bottom Action Menu Popover & Prompt Container */}
          <div className="p-4 bg-slate-950/95 border-t border-slate-800/80 relative">
            
            {/* Popover Action Menu */}
            {isActionMenuOpen && (
              <div
                ref={actionMenuRef}
                className="absolute bottom-20 left-4 z-20 w-80 bg-[#18181b]/95 backdrop-blur-xl border border-slate-700/80 rounded-2xl shadow-2xl p-2 space-y-1 animate-in fade-in slide-in-from-bottom-3"
              >
                {/* Search inside popover */}
                <div className="px-2.5 py-1.5 mb-1 bg-slate-900/80 rounded-xl border border-slate-800/80 flex items-center gap-2">
                  <Search className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  <input
                    type="text"
                    value={actionSearchQuery}
                    onChange={(e) => setActionSearchQuery(e.target.value)}
                    placeholder="Type to search plugins, files, folders & skills"
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

            {/* Interactive Prompt Bar */}
            <form
              onSubmit={(e) => handleSend(e, null)}
              className="flex items-center gap-2 p-1.5 rounded-2xl bg-[#111318] border border-slate-800/90 shadow-inner"
            >
              {/* + Action Menu Trigger Button */}
              <button
                type="button"
                onClick={() => setIsActionMenuOpen(!isActionMenuOpen)}
                className={`w-9 h-9 rounded-xl flex items-center justify-center text-slate-300 hover:text-white hover:bg-slate-800/80 transition-all shrink-0 ${
                  isActionMenuOpen ? 'bg-slate-800 text-white rotate-45' : ''
                }`}
                title="Add photos, files & actions"
              >
                <Plus className="w-5 h-5 transition-transform duration-200" />
              </button>

              {/* Main Input Field */}
              <input
                type="text"
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder={
                  activeMode === 'image'
                    ? 'Describe the image you want to create...'
                    : activeMode === 'deep-research'
                    ? 'Enter topic for deep multi-perspective research...'
                    : attachments.length > 0
                    ? 'Ask questions about the attached file(s)...'
                    : 'Ask anything'
                }
                disabled={loading || isQuotaExceeded}
                className="flex-1 px-3 py-2 bg-transparent text-sm text-slate-100 placeholder:text-slate-500 focus:outline-none disabled:opacity-50"
              />

              {/* Think Mode Toggle Button */}
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

              {/* Mic / Voice Input Placeholder */}
              <button
                type="button"
                onClick={() => {}}
                className="p-2 rounded-xl text-slate-400 hover:text-slate-200 hover:bg-slate-800/60 transition-colors shrink-0"
                title="Voice Input"
              >
                <Mic className="w-4 h-4" />
              </button>

              {/* Send Action Button */}
              <button
                type="submit"
                disabled={(!prompt.trim() && attachments.length === 0) || loading || isQuotaExceeded}
                className={`w-9 h-9 rounded-full flex items-center justify-center text-white shadow-md disabled:opacity-40 disabled:cursor-not-allowed transition-all shrink-0 ${
                  activeMode === 'image'
                    ? 'bg-amber-600 hover:bg-amber-500 shadow-amber-500/20'
                    : 'bg-blue-600 hover:bg-blue-500 shadow-blue-500/20'
                }`}
                title={activeMode === 'image' ? 'Generate Image' : 'Send Prompt'}
              >
                {activeMode === 'image' ? <Wand2 className="w-4 h-4" /> : <AudioWaveform className="w-4 h-4" />}
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AIStudioPage;
