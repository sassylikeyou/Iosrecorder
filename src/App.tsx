/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { 
  FileCode, FolderOpen, Code2, Terminal, Download, CircleDot, 
  Sparkles, Crop, Gauge, BarChart3, Video, Settings as SettingsIcon,
  Mic, MousePointer2, Eye, Clock, Cpu, Scissors, MonitorPlay
} from 'lucide-react';
import React, { useState, useEffect } from 'react';

const ANDROID_FILES = [
  { name: 'README.md', path: '/my-mobile-app/README.md', language: 'markdown' },
  { name: 'AndroidManifest.xml', path: '/my-mobile-app/app/src/main/AndroidManifest.xml', language: 'xml' },
  { name: 'MainActivity.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/MainActivity.kt', language: 'kotlin' },
  { name: 'SettingsActivity.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/SettingsActivity.kt', language: 'kotlin' },
  { name: 'SettingsRepository.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/SettingsRepository.kt', language: 'kotlin' },
  { name: 'RecordingService.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/RecordingService.kt', language: 'kotlin' },
  { name: 'RecorderTileService.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/RecorderTileService.kt', language: 'kotlin' },
  { name: 'RecordingManager.kt', path: '/my-mobile-app/app/src/main/java/com/example/screenrecorder/RecordingManager.kt', language: 'kotlin' },
  { name: 'build.gradle.kts (App)', path: '/my-mobile-app/app/build.gradle.kts', language: 'kotlin' },
  { name: 'activity_main.xml', path: '/my-mobile-app/app/src/main/res/layout/activity_main.xml', language: 'xml' },
  { name: 'activity_settings.xml', path: '/my-mobile-app/app/src/main/res/layout/activity_settings.xml', language: 'xml' }
];

function Switch({ value, onChange, theme }: { value: boolean, onChange: (value: boolean) => void, theme: string }) {
  return (
    <div 
      onClick={() => onChange(!value)}
      className={`w-11 h-6 rounded-full flex items-center p-1 cursor-pointer transition-colors ${
        value ? 'bg-[#7C3AED]' : theme === 'dark' ? 'bg-[#374151]' : 'bg-[#D1D5DB]'
      }`}
    >
      <div 
        className={`w-4 h-4 rounded-full bg-white shadow-sm transform transition-transform ${
          value ? 'translate-x-5' : 'translate-x-0'
        }`}
      />
    </div>
  );
}

function MobileMockup() {
  const [activeTab, setActiveTab] = useState('Settings');
  const [theme, setTheme] = useState('dark');
  const [recordAudio, setRecordAudio] = useState(true);
  const [showTouches, setShowTouches] = useState(false);

  const toggleTheme = () => setTheme(prev => prev === 'dark' ? 'light' : 'dark');

  const isDark = theme === 'dark';
  const bg = isDark ? 'bg-[#111827]' : 'bg-[#F8FAFC]';
  const cardBg = isDark ? 'bg-[#1F1F2B]' : 'bg-white';
  const textTitle = isDark ? 'text-white' : 'text-[#111827]';
  const textLabel = isDark ? 'text-white' : 'text-[#111827]';
  const textValue = isDark ? 'text-[#9CA3AF]' : 'text-[#6B7280]';
  const divider = isDark ? 'border-[#374151]' : 'border-[#F1F5F9]';

  return (
    <div className={`w-[375px] h-[812px] ${bg} rounded-[3rem] shadow-2xl overflow-hidden border-8 border-white flex flex-col relative shrink-0 transition-colors duration-300`}>
      {/* Dynamic Content Area */}
      <div className="flex-1 overflow-y-auto pb-24" style={{ scrollbarWidth: 'none' }}>
        {activeTab === 'Settings' && (
          <div className="p-5">
            <h1 className={`text-[24px] font-bold ${textTitle} mt-8 mb-6`}>Settings</h1>
            
            {/* VIDEO SECTION */}
            <div className={`text-[12px] font-semibold ${textValue} mb-2 mt-4`}>VIDEO</div>
            <div className={`${cardBg} rounded-xl px-4 py-2 transition-colors duration-300 shadow-[0_2px_10px_rgba(0,0,0,0.02)]`}>
              
              <div className={`flex items-center py-3 border-b ${divider}`}>
                <Crop size={22} color="#7C3AED" />
                <div className="flex-1 ml-3 flex justify-between items-center">
                  <span className={`text-[15px] ${textLabel}`}>Resolution</span>
                  <span className={`text-[14px] ${textValue} font-medium`}>720p HD <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                </div>
              </div>
              
              <div className={`flex items-center py-3 border-b ${divider}`}>
                <Gauge size={22} color="#2563EB" />
                <div className="flex-1 ml-3 flex justify-between items-center">
                  <span className={`text-[15px] ${textLabel}`}>Frame Rate</span>
                  <span className={`text-[14px] ${textValue} font-medium`}>30 FPS <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                </div>
              </div>

              <div className={`flex items-center py-3 border-b ${divider}`}>
                <Clock size={22} color="#10B981" />
                <div className="flex-1 ml-3 flex justify-between items-center">
                  <span className={`text-[15px] ${textLabel}`}>Countdown</span>
                  <span className={`text-[14px] ${textValue} font-medium`}>3s <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                </div>
              </div>

              <div className={`flex items-center py-3 border-b ${divider}`}>
                <BarChart3 size={22} color="#F59E0B" />
                <div className="flex-1 ml-3 flex justify-between items-center">
                  <span className={`text-[15px] ${textLabel}`}>Bitrate</span>
                  <span className={`text-[14px] ${textValue} font-medium`}>8 Mbps <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                </div>
              </div>

              <div className={`flex items-start py-3 border-b ${divider}`}>
                <Cpu size={22} color="#A78BFA" className="mt-0.5" />
                <div className="flex-1 ml-3">
                  <div className="flex justify-between items-center">
                    <span className={`text-[15px] ${textLabel}`}>Video Encoder</span>
                    <span className={`text-[14px] ${textValue} font-medium tracking-tight`}>H.264 <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                  </div>
                  <div className={`text-[12px] ${textValue} mt-1 leading-snug pr-4`}>
                    H.264 is highly compatible with most standard video players.
                  </div>
                </div>
              </div>

              <div className={`flex items-center py-3`}>
                <Scissors size={22} color="#F472B6" />
                <div className="flex-1 ml-3 flex justify-between items-center">
                  <span className={`text-[15px] ${textLabel}`}>Max File Size</span>
                  <span className={`text-[14px] ${textValue} font-medium`}>No Limit <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
                </div>
              </div>

            </div>

            {/* AUDIO & TOUCHES */}
            <div className={`${cardBg} rounded-xl px-4 py-2 transition-colors duration-300 mt-5 shadow-[0_2px_10px_rgba(0,0,0,0.02)]`}>
              <div className={`flex items-center py-3 border-b ${divider}`}>
                <Mic size={22} color="#7C3AED" />
                <div className="flex-1 ml-3 flex justify-between items-center pr-2">
                  <span className={`text-[15px] ${textLabel}`}>Record Audio</span>
                  <Switch value={recordAudio} onChange={setRecordAudio} theme={theme} />
                </div>
              </div>
              
              <div className={`flex items-center py-3`}>
                <MousePointer2 size={22} color="#60A5FA" />
                <div className="flex-1 ml-3 flex justify-between items-center pr-2">
                  <span className={`text-[15px] ${textLabel}`}>Show Touches</span>
                  <Switch value={showTouches} onChange={setShowTouches} theme={theme} />
                </div>
              </div>

              <div className={`text-[12px] ${textValue} py-3 border-t ${divider} leading-snug`}>
                Record microphone audio or show screen touches while recording. (Show Touches requires system modify permission)
              </div>
            </div>

            {/* APPEARANCE */}
            <div className={`text-[12px] font-semibold ${textValue} mb-2 mt-6`}>APPEARANCE</div>
            <div onClick={toggleTheme} className={`${cardBg} rounded-xl px-4 py-3 flex items-center transition-colors duration-300 cursor-pointer shadow-[0_2px_10px_rgba(0,0,0,0.02)]`}>
              <Eye size={22} color="#10B981" />
              <div className="flex-1 ml-3 flex justify-between items-center">
                <span className={`text-[15px] ${textLabel}`}>App Theme</span>
                <span className={`text-[14px] ${textValue} font-medium`}>{isDark ? 'Dark Theme' : 'Light Theme'} <span className="text-[12px] ml-1 opacity-50">&gt;</span></span>
              </div>
            </div>
          </div>
        )}

        {(activeTab === 'Videos' || activeTab === 'Record') && (
          <div className="p-5 flex flex-col items-center justify-center h-full pt-32">
            <Video size={60} color="#7C3AED" />
            <h2 className={`text-[20px] font-bold ${textTitle} mt-4`}>No recordings yet</h2>
            <p className={`text-[14px] ${textValue} text-center mt-2 px-6 leading-relaxed`}>
              Your recorded videos will appear here. Start recording to capture your screen.
            </p>
            <button className="flex items-center bg-[#7C3AED] hover:bg-[#6D28D9] text-white px-5 py-3 rounded-lg mt-6 shadow-[0_4px_14px_rgba(124,58,237,0.3)] transition-colors">
              <CircleDot size={20} className="mr-2" />
              <span className="font-bold text-[15px]">Start Recording</span>
            </button>
          </div>
        )}
      </div>

      {/* Bottom Nav */}
      <div className={`absolute bottom-0 w-full ${isDark ? 'bg-[#111827] border-[#1F1F2B]' : 'bg-white border-gray-50'} border-t pt-3 pb-8 flex flex-row justify-around z-10 transition-colors duration-300`}>
        <button onClick={() => setActiveTab('Record')} className="flex flex-col items-center group w-20">
          <CircleDot size={24} color={activeTab === 'Record' ? '#7C3AED' : (isDark ? '#9CA3AF' : '#6B7280')} className="mb-1 transition-colors" />
          <span className={`text-[11px] transition-colors ${activeTab === 'Record' ? 'text-[#7C3AED] font-medium' : isDark ? 'text-[#9CA3AF]' : 'text-[#6B7280]'}`}>Record</span>
        </button>
        <button onClick={() => setActiveTab('Videos')} className="flex flex-col items-center group w-20">
          <MonitorPlay size={24} color={activeTab === 'Videos' ? '#7C3AED' : (isDark ? '#9CA3AF' : '#6B7280')} className="mb-1 transition-colors" />
          <span className={`text-[11px] transition-colors ${activeTab === 'Videos' ? 'text-[#7C3AED] font-medium' : isDark ? 'text-[#9CA3AF]' : 'text-[#6B7280]'}`}>Videos</span>
        </button>
        <button onClick={() => setActiveTab('Settings')} className="flex flex-col items-center group w-20">
          <SettingsIcon size={24} color={activeTab === 'Settings' ? '#7C3AED' : (isDark ? '#9CA3AF' : '#6B7280')} className="mb-1 transition-colors" />
          <span className={`text-[11px] transition-colors ${activeTab === 'Settings' ? 'text-[#7C3AED] font-medium' : isDark ? 'text-[#9CA3AF]' : 'text-[#6B7280]'}`}>Settings</span>
        </button>
      </div>
    </div>
  );
}

export default function App() {
  const [selectedFile, setSelectedFile] = useState(ANDROID_FILES[0]);
  const [fileContent, setFileContent] = useState<string>('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    async function loadContent() {
      setIsLoading(true);
      try {
        const response = await fetch(selectedFile.path);
        if (response.ok) {
          const text = await response.text();
          setFileContent(text);
        } else {
          setFileContent(`Error loading file: ${response.statusText}`);
        }
      } catch (err) {
        setFileContent('Failed to fetch file content.');
      } finally {
        setIsLoading(false);
      }
    }
    loadContent();
  }, [selectedFile]);

  return (
    <div className="flex h-screen w-full bg-[#000000] text-white font-sans overflow-hidden">
      {/* Sidebar */}
      <div className="w-80 bg-[#1C1C1E] border-r border-white/5 flex flex-col h-full">
        <div className="p-6 border-b border-white/5">
          <div className="flex items-center gap-3 text-white mb-2">
            <Code2 size={28} className="stroke-[1.5] text-[#FF3B30]" />
            <h1 className="text-[24px] font-semibold tracking-tight">Native Android</h1>
          </div>
          <p className="text-[13px] text-[#8E8E93] md:min-h-0 leading-relaxed">
            iOS-Style Screen Recorder codebase generated successfully.
          </p>
        </div>

        <div className="flex-1 overflow-y-auto py-4">
          <div className="px-6 mb-2 flex items-center gap-2 text-[11px] font-bold uppercase tracking-wider text-[#8E8E93]">
            <FolderOpen size={14} />
            <span>my-mobile-app</span>
          </div>
          <ul className="space-y-1 px-4">
            {ANDROID_FILES.map((file) => (
              <li key={file.path}>
                <button
                  onClick={() => setSelectedFile(file)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-[12px] text-[15px] font-medium transition-colors ${
                    selectedFile.path === file.path
                      ? 'bg-white/10 text-white'
                      : 'text-[#8E8E93] hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <FileCode size={18} className={selectedFile.path === file.path ? "text-[#0A84FF]" : "text-[#8E8E93]"} />
                  {file.name}
                </button>
              </li>
            ))}
          </ul>
        </div>
        
        <div className="p-6 border-t border-white/5">
          <div className="text-[12px] text-[#8E8E93] leading-relaxed">
            <strong className="block text-white mb-1 uppercase text-[10px] tracking-wider font-bold">To use this code:</strong>
            Use the <span className="font-medium text-[#0A84FF]">Export to ZIP</span> option in your AI Studio settings menu to download the complete codebase.
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 flex flex-row h-full bg-[#000000] overflow-hidden">
        {/* Code Viewer */}
        <div className="flex-1 flex flex-col h-full border-r border-white/5">
          <div className="h-16 border-b border-white/5 flex items-center justify-between px-8 bg-[#1C1C1E]">
            <div className="flex items-center gap-2 text-[13px] font-medium text-[#8E8E93] bg-white/5 px-3 py-1.5 rounded-lg border border-white/5">
              <Terminal size={14} className="text-[#8E8E93]" />
              {selectedFile.path}
            </div>
            
            <button 
              onClick={() => navigator.clipboard.writeText(fileContent)}
              className="flex items-center gap-2 px-4 py-1.5 text-[14px] font-medium text-[#0A84FF] bg-[#0A84FF]/10 hover:bg-[#0A84FF]/20 rounded-[12px] transition-colors"
            >
              Copy Code
            </button>
          </div>

          <div className="flex-1 overflow-hidden relative">
            {isLoading ? (
              <div className="absolute inset-0 flex items-center justify-center text-[#8E8E93] text-[14px]">
                Loading...
              </div>
            ) : (
              <pre className="h-full w-full overflow-auto p-8 text-[13px] font-mono text-white/80 leading-relaxed">
                <code>{fileContent}</code>
              </pre>
            )}
          </div>
        </div>
        
        {/* Mobile Mockup Preview */}
        <div className="w-[450px] bg-[#1C1C1E] flex flex-col items-center justify-center p-8">
          <div className="text-white/50 text-[12px] font-medium uppercase tracking-wider mb-6 flex items-center gap-2">
            <Sparkles size={14} />
            React Native UI Mockup
          </div>
          <MobileMockup />
        </div>
      </div>
    </div>
  );
}
