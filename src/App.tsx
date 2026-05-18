/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { FileCode, FolderOpen, Code2, DownloadCloud, Terminal, Download } from 'lucide-react';
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
      <div className="flex-1 flex flex-col h-full bg-[#000000]">
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
    </div>
  );
}
