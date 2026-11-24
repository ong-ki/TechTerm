import React, { useEffect, useMemo, useState } from 'react';
import {
  Bell,
  BellOff,
  Check,
  Code2,
  Copy,
  Home,
  Moon,
  Search,
  SearchX,
  Sun,
  X,
} from 'lucide-react';
import { AnimatePresence, motion } from 'motion/react';
import rawTerms from '../app/src/main/assets/terms.json';
import { TERM_CATEGORIES, Term } from './types';
import { isCodeLikeTerm, renderHighlightedCode } from './utils/highlightCode';

const TERMS: Term[] = (rawTerms as Omit<Term, 'id'>[]).map((item, idx) => ({
  id: idx + 1,
  term: (item.term ?? '').trim(),
  category: (item.category ?? '').trim(),
  difficulty: (item.difficulty ?? '').trim(),
  meaningMy: (item.meaningMy ?? '').trim(),
  meaningEn: (item.meaningEn ?? '').trim(),
  example: (item.example ?? '').trim(),
  codeSnippet: item.codeSnippet?.trimEnd() || null,
  codeLanguage: item.codeLanguage?.trim() || null,
  usageNote: item.usageNote?.trim() || null,
}));

const TERMS_WITH_CODE: Term[] = TERMS.filter((t) => Boolean(t.codeSnippet?.trim()));

function getDayOfYear(date: Date): number {
  const start = new Date(date.getFullYear(), 0, 0);
  const diff = date.getTime() - start.getTime();
  return Math.floor(diff / (1000 * 60 * 60 * 24));
}

function getTermOfTheDay(devTermsOnly: boolean, date: Date): Term | null {
  const day = getDayOfYear(date);
  const year = date.getFullYear();
  if (devTermsOnly && TERMS_WITH_CODE.length > 0) {
    const idx = ((day + year) % TERMS_WITH_CODE.length + TERMS_WITH_CODE.length) % TERMS_WITH_CODE.length;
    return TERMS_WITH_CODE[idx] ?? null;
  }
  if (TERMS.length === 0) return null;
  const idx = ((day + year) % TERMS.length + TERMS.length) % TERMS.length;
  return TERMS[idx] ?? null;
}

function formatEnglishDate(date: Date): string {
  const weekday = new Intl.DateTimeFormat('en-US', { weekday: 'short' }).format(date);
  const day = date.getDate();
  const month = new Intl.DateTimeFormat('en-US', { month: 'short' }).format(date);
  return `${weekday}, ${day} ${month}`;
}

function formatTime12h(hour: number, minute: number): string {
  const period = hour >= 12 ? 'PM' : 'AM';
  const h12 = hour % 12 === 0 ? 12 : hour % 12;
  const mm = String(minute).padStart(2, '0');
  return `${h12}:${mm} ${period}`;
}

type ActiveTab = 'home' | 'browse';

export default function App() {
  const [darkMode, setDarkMode] = useState(false);
  const [activeTab, setActiveTab] = useState<ActiveTab>('home');
  const [selectedTerm, setSelectedTerm] = useState<Term | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Notification preferences (persisted in localStorage)
  const [notificationsEnabled, setNotificationsEnabled] = useState<boolean>(() => {
    const saved = localStorage.getItem('techterms_notif_enabled');
    return saved === null ? true : saved === 'true';
  });
  const [notificationHour, setNotificationHour] = useState<number>(() => {
    const saved = Number(localStorage.getItem('techterms_notif_hour'));
    return Number.isFinite(saved) && saved >= 0 && saved <= 23 ? saved : 8;
  });
  const [notificationMinute, setNotificationMinute] = useState<number>(() => {
    const saved = Number(localStorage.getItem('techterms_notif_minute'));
    return Number.isFinite(saved) && saved >= 0 && saved <= 59 ? saved : 0;
  });
  const [devTermsOnly, setDevTermsOnly] = useState<boolean>(() => {
    return localStorage.getItem('techterms_dev_only') === 'true';
  });
  const [showTimePicker, setShowTimePicker] = useState(false);

  // Browse / Search state
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [filterHasCode, setFilterHasCode] = useState(false);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode);
  }, [darkMode]);

  useEffect(() => {
    localStorage.setItem('techterms_notif_enabled', String(notificationsEnabled));
  }, [notificationsEnabled]);

  useEffect(() => {
    localStorage.setItem('techterms_notif_hour', String(notificationHour));
    localStorage.setItem('techterms_notif_minute', String(notificationMinute));
  }, [notificationHour, notificationMinute]);

  useEffect(() => {
    localStorage.setItem('techterms_dev_only', String(devTermsOnly));
  }, [devTermsOnly]);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    window.setTimeout(() => {
      setToastMessage((prev) => (prev === msg ? null : prev));
    }, 1600);
  };

  const today = useMemo(() => new Date(), []);
  const todayLabel = useMemo(() => formatEnglishDate(today), [today]);

  const termOfTheDay = useMemo(
    () => getTermOfTheDay(devTermsOnly, today),
    [devTermsOnly, today]
  );

  const snippetTerm = useMemo(() => {
    if (termOfTheDay?.codeSnippet) return termOfTheDay;
    return getTermOfTheDay(true, today);
  }, [termOfTheDay, today]);

  const filteredTerms = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return TERMS.filter((t) => {
      if (selectedCategory && t.category !== selectedCategory) return false;
      if (filterHasCode && !t.codeSnippet) return false;
      if (!q) return true;
      return (
        t.term.toLowerCase().includes(q) ||
        t.meaningMy.toLowerCase().includes(q) ||
        t.meaningEn.toLowerCase().includes(q) ||
        (t.codeSnippet?.toLowerCase().includes(q) ?? false)
      );
    }).sort((a, b) => a.term.localeCompare(b.term, 'en', { sensitivity: 'base' }));
  }, [searchQuery, selectedCategory, filterHasCode]);

  const copyText = (text: string) => {
    navigator.clipboard?.writeText(text).catch(() => {});
    showToast('Copied');
  };

  return (
    <div className="min-h-screen w-full bg-[#FAFAFA] text-[#111111] dark:bg-[#0E0E0E] dark:text-[#F3F3F5] flex flex-col">
      {/* Main responsive container */}
      <div className="w-full max-w-2xl mx-auto flex-1 flex flex-col pb-20">
        {activeTab === 'home' ? (
          <HomeView
            todayLabel={todayLabel}
            termOfTheDay={termOfTheDay}
            snippetTerm={snippetTerm}
            notificationsEnabled={notificationsEnabled}
            notificationHour={notificationHour}
            notificationMinute={notificationMinute}
            devTermsOnly={devTermsOnly}
            darkMode={darkMode}
            onToggleDarkMode={() => setDarkMode((d) => !d)}
            onTermClick={setSelectedTerm}
            onCopyCode={copyText}
            onToggleNotifications={setNotificationsEnabled}
            onOpenTimePicker={() => setShowTimePicker(true)}
            onToggleDevTermsOnly={setDevTermsOnly}
          />
        ) : (
          <BrowseView
            query={searchQuery}
            selectedCategory={selectedCategory}
            hasCodeOnly={filterHasCode}
            terms={filteredTerms}
            darkMode={darkMode}
            onToggleDarkMode={() => setDarkMode((d) => !d)}
            onQueryChange={setSearchQuery}
            onCategoryChange={setSelectedCategory}
            onToggleHasCode={() => setFilterHasCode((v) => !v)}
            onTermClick={setSelectedTerm}
          />
        )}
      </div>

      {/* Bottom Navigation Bar (Home / Search) */}
      <nav
        aria-label="Primary navigation"
        className="fixed bottom-0 left-0 right-0 z-30 bg-[#FFFFFF] dark:bg-[#151517] border-t border-[#E9E9EE] dark:border-[#242429]"
      >
        <div className="max-w-2xl mx-auto grid grid-cols-2 h-16">
          <button
            type="button"
            onClick={() => setActiveTab('home')}
            className={`flex flex-col items-center justify-center gap-1 text-xs font-medium transition-colors ${
              activeTab === 'home'
                ? 'text-[#3F51B5] dark:text-[#7986CB]'
                : 'text-[#666A73] dark:text-[#9A9EA8] hover:text-[#111111] dark:hover:text-[#F3F3F5]'
            }`}
          >
            <Home className="w-5 h-5" strokeWidth={activeTab === 'home' ? 2.25 : 1.75} />
            <span>Home</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('browse')}
            className={`flex flex-col items-center justify-center gap-1 text-xs font-medium transition-colors ${
              activeTab === 'browse'
                ? 'text-[#3F51B5] dark:text-[#7986CB]'
                : 'text-[#666A73] dark:text-[#9A9EA8] hover:text-[#111111] dark:hover:text-[#F3F3F5]'
            }`}
          >
            <Search className="w-5 h-5" strokeWidth={activeTab === 'browse' ? 2.25 : 1.75} />
            <span>Search</span>
          </button>
        </div>
      </nav>

      {/* Term Detail Modal Bottom Sheet */}
      <AnimatePresence>
        {selectedTerm && (
          <TermDetailSheet
            term={selectedTerm}
            onClose={() => setSelectedTerm(null)}
            onCopy={copyText}
          />
        )}
      </AnimatePresence>

      {/* Notification Time Picker Dialog */}
      <AnimatePresence>
        {showTimePicker && (
          <TimePickerDialog
            initialHour={notificationHour}
            initialMinute={notificationMinute}
            onCancel={() => setShowTimePicker(false)}
            onConfirm={(h, m) => {
              setNotificationHour(h);
              setNotificationMinute(m);
              setShowTimePicker(false);
            }}
          />
        )}
      </AnimatePresence>

      {/* Toast */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 8 }}
            transition={{ duration: 0.16 }}
            className="fixed bottom-20 left-1/2 -translate-x-1/2 z-50 px-4 py-2 rounded-full bg-[#111111] text-[#FAFAFA] dark:bg-[#F3F3F5] dark:text-[#0E0E0E] text-xs font-medium shadow-sm"
          >
            {toastMessage}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

interface HomeViewProps {
  todayLabel: string;
  termOfTheDay: Term | null;
  snippetTerm: Term | null;
  notificationsEnabled: boolean;
  notificationHour: number;
  notificationMinute: number;
  devTermsOnly: boolean;
  darkMode: boolean;
  onToggleDarkMode: () => void;
  onTermClick: (term: Term) => void;
  onCopyCode: (code: string) => void;
  onToggleNotifications: (enabled: boolean) => void;
  onOpenTimePicker: () => void;
  onToggleDevTermsOnly: (devOnly: boolean) => void;
}

function HomeView({
  todayLabel,
  termOfTheDay,
  snippetTerm,
  notificationsEnabled,
  notificationHour,
  notificationMinute,
  devTermsOnly,
  darkMode,
  onToggleDarkMode,
  onTermClick,
  onCopyCode,
  onToggleNotifications,
  onOpenTimePicker,
  onToggleDevTermsOnly,
}: HomeViewProps) {
  const [copiedSnippet, setCopiedSnippet] = useState(false);

  const previewLines = useMemo(() => {
    if (!snippetTerm?.codeSnippet) return '';
    return snippetTerm.codeSnippet.split('\n').slice(0, 4).join('\n');
  }, [snippetTerm]);

  const formattedTime = formatTime12h(notificationHour, notificationMinute);

  return (
    <main className="px-4 py-3 flex flex-col gap-3">
      {/* Slim top row: title left, date + theme toggle right */}
      <header className="flex items-center justify-between py-1">
        <h1 className="text-[19px] font-bold tracking-tight text-[#111111] dark:text-[#F3F3F5]">
          TechTerms
        </h1>
        <div className="flex items-center gap-2.5">
          <span className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8]">
            {todayLabel}
          </span>
          <button
            type="button"
            onClick={onToggleDarkMode}
            aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
            className="w-9 h-9 rounded-full flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:bg-[#F2F2F5] dark:hover:bg-[#1E1E22] transition-colors"
          >
            {darkMode ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          </button>
        </div>
      </header>

      {/* Section 1: Term of the Day */}
      <section className="flex flex-col gap-1.5">
        <h2 className="pl-0.5 text-[11px] font-medium tracking-wider uppercase text-[#666A73] dark:text-[#9A9EA8]">
          Term of the Day
        </h2>

        {termOfTheDay ? (
          <motion.button
            key={termOfTheDay.id}
            type="button"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, ease: [0.16, 1, 0.3, 1] }}
            onClick={() => onTermClick(termOfTheDay)}
            className="w-full text-left rounded-[16px] bg-[#FFFFFF] dark:bg-[#151517] border border-[#E9E9EE] dark:border-[#242429] p-3.5 hover:border-[#DCDCE2] dark:hover:border-[#2F2F36] transition-colors focus-visible:outline-2 focus-visible:outline-[#3F51B5]"
          >
            <div className="flex items-center gap-1.5 mb-2">
              <span className="flex-1 text-[22px] leading-7 font-bold tracking-[-0.02em] text-[#111111] dark:text-[#F3F3F5] truncate">
                {termOfTheDay.term}
              </span>
              <span className="shrink-0 max-w-[116px] truncate text-[10px] font-medium px-2 py-0.5 rounded-full border border-[#3F51B5]/45 dark:border-[#7986CB]/45 text-[#3F51B5] dark:text-[#7986CB]">
                {termOfTheDay.category}
              </span>
              <span className="shrink-0 text-[10px] font-medium px-2 py-0.5 rounded-full border border-[#DCDCE2] dark:border-[#2F2F36] text-[#666A73] dark:text-[#9A9EA8]">
                {termOfTheDay.difficulty}
              </span>
            </div>

            <p className="font-myanmar text-[14px] leading-[1.55] text-[#111111] dark:text-[#F3F3F5] line-clamp-3 mb-1.5">
              {termOfTheDay.meaningMy}
            </p>

            <p className="text-[13px] leading-[18px] text-[#666A73] dark:text-[#9A9EA8] line-clamp-2">
              {termOfTheDay.meaningEn}
            </p>
          </motion.button>
        ) : (
          <div className="rounded-[16px] bg-[#FFFFFF] dark:bg-[#151517] border border-[#E9E9EE] dark:border-[#242429] p-3.5 text-sm text-[#666A73] dark:text-[#9A9EA8]">
            No terms available yet
          </div>
        )}
      </section>

      {/* Section 2: Developer snippet */}
      {snippetTerm && snippetTerm.codeSnippet && (
        <section className="flex flex-col gap-1.5">
          <h2 className="pl-0.5 text-[11px] font-medium tracking-wider uppercase text-[#666A73] dark:text-[#9A9EA8]">
            Developer snippet
          </h2>

          <div
            role="button"
            tabIndex={0}
            onClick={() => onTermClick(snippetTerm)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onTermClick(snippetTerm);
              }
            }}
            className="w-full text-left rounded-[16px] bg-[#FFFFFF] dark:bg-[#151517] border border-[#E9E9EE] dark:border-[#242429] p-3.5 hover:border-[#DCDCE2] dark:hover:border-[#2F2F36] transition-colors cursor-pointer"
          >
            {snippetTerm.id !== termOfTheDay?.id && (
              <div className="text-xs font-semibold text-[#3F51B5] dark:text-[#7986CB] mb-1 truncate">
                {snippetTerm.term}
              </div>
            )}

            {snippetTerm.usageNote && (
              <p className="font-myanmar text-[14px] leading-[1.55] text-[#111111] dark:text-[#F3F3F5] line-clamp-2 mb-2">
                {snippetTerm.usageNote}
              </p>
            )}

            <div className="rounded-[12px] bg-[#F3F4F7] dark:bg-[#17181D] border border-[#E9E9EE] dark:border-[#242429] overflow-hidden">
              <div className="flex items-center justify-between pl-2.5 pr-1 h-9 border-b border-[#E9E9EE] dark:border-[#242429]">
                <span className="font-code text-[11px] text-[#666A73] dark:text-[#9A9EA8]">
                  {(snippetTerm.codeLanguage || 'code').toLowerCase()}
                </span>
                <button
                  type="button"
                  aria-label={copiedSnippet ? 'Copied' : 'Copy code'}
                  onClick={(e) => {
                    e.stopPropagation();
                    onCopyCode(snippetTerm.codeSnippet ?? '');
                    setCopiedSnippet(true);
                    window.setTimeout(() => setCopiedSnippet(false), 1500);
                  }}
                  className="w-9 h-9 rounded-lg flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:text-[#111111] dark:hover:text-[#F3F3F5] transition-colors"
                >
                  {copiedSnippet ? (
                    <Check className="w-4 h-4 text-[#3F51B5] dark:text-[#7986CB]" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                </button>
              </div>
              <pre className="overflow-x-auto px-2.5 py-2 font-code text-[12px] leading-[17px] whitespace-pre">
                <code>{renderHighlightedCode(previewLines)}</code>
              </pre>
            </div>
          </div>
        </section>
      )}

      {/* Section 3: Notification */}
      <section className="flex flex-col gap-1.5">
        <h2 className="pl-0.5 text-[11px] font-medium tracking-wider uppercase text-[#666A73] dark:text-[#9A9EA8]">
          Notification
        </h2>

        <div className="rounded-[16px] bg-[#FFFFFF] dark:bg-[#151517] border border-[#E9E9EE] dark:border-[#242429]">
          <div className="min-h-[60px] px-3.5 py-2 flex items-center gap-3">
            {notificationsEnabled ? (
              <Bell className="w-5 h-5 shrink-0 text-[#3F51B5] dark:text-[#7986CB]" />
            ) : (
              <BellOff className="w-5 h-5 shrink-0 text-[#666A73] dark:text-[#9A9EA8]" />
            )}

            <div className="flex-1 min-w-0">
              <div className="text-sm font-medium text-[#111111] dark:text-[#F3F3F5]">
                Daily notification
              </div>
              <button
                type="button"
                onClick={onOpenTimePicker}
                className={`text-xs font-medium py-0.5 rounded hover:underline ${
                  notificationsEnabled
                    ? 'text-[#3F51B5] dark:text-[#7986CB]'
                    : 'text-[#666A73] dark:text-[#9A9EA8]'
                }`}
              >
                {notificationsEnabled ? formattedTime : `${formattedTime} · Off`}
              </button>
            </div>

            <button
              type="button"
              role="switch"
              aria-checked={notificationsEnabled}
              aria-label="Daily notification"
              onClick={() => onToggleNotifications(!notificationsEnabled)}
              className={`relative inline-flex h-7 w-12 shrink-0 items-center rounded-full border transition-colors ${
                notificationsEnabled
                  ? 'bg-[#3F51B5] dark:bg-[#7986CB] border-transparent'
                  : 'bg-[#F2F2F5] dark:bg-[#1E1E22] border-[#DCDCE2] dark:border-[#2F2F36]'
              }`}
            >
              <span
                className={`inline-block h-5 w-5 rounded-full transition-transform ${
                  notificationsEnabled
                    ? 'translate-x-6 bg-[#FFFFFF] dark:bg-[#0E0E0E]'
                    : 'translate-x-1 bg-[#666A73] dark:bg-[#9A9EA8]'
                }`}
              />
            </button>
          </div>

          <div className="mx-3.5 h-px bg-[#E9E9EE] dark:bg-[#242429]" />

          <label className="flex items-center gap-2.5 px-3.5 py-2.5 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={devTermsOnly}
              onChange={(e) => onToggleDevTermsOnly(e.target.checked)}
              className="w-4 h-4 rounded accent-[#3F51B5] dark:accent-[#7986CB] cursor-pointer"
            />
            <span className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8]">
              Developer terms only
            </span>
          </label>
        </div>
      </section>
    </main>
  );
}

interface BrowseViewProps {
  query: string;
  selectedCategory: string | null;
  hasCodeOnly: boolean;
  terms: Term[];
  darkMode: boolean;
  onToggleDarkMode: () => void;
  onQueryChange: (q: string) => void;
  onCategoryChange: (cat: string | null) => void;
  onToggleHasCode: () => void;
  onTermClick: (term: Term) => void;
}

function BrowseView({
  query,
  selectedCategory,
  hasCodeOnly,
  terms,
  darkMode,
  onToggleDarkMode,
  onQueryChange,
  onCategoryChange,
  onToggleHasCode,
  onTermClick,
}: BrowseViewProps) {
  return (
    <main className="flex flex-col flex-1">
      {/* Top Bar */}
      <header className="px-5 pt-3 pb-2 flex items-center justify-between">
        <h1 className="text-xl font-semibold text-[#111111] dark:text-[#F3F3F5]">Search</h1>
        <button
          type="button"
          onClick={onToggleDarkMode}
          aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
          className="w-9 h-9 rounded-full flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:bg-[#F2F2F5] dark:hover:bg-[#1E1E22] transition-colors"
        >
          {darkMode ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
        </button>
      </header>

      {/* Search Input */}
      <div className="px-5 py-1">
        <div className="relative flex items-center">
          <Search className="w-4 h-4 absolute left-4 text-[#666A73] dark:text-[#9A9EA8] pointer-events-none" />
          <input
            type="text"
            value={query}
            onChange={(e) => onQueryChange(e.target.value)}
            placeholder="Search terms, meanings, or code…"
            className="w-full h-11 pl-10 pr-10 rounded-full bg-[#F2F2F5] dark:bg-[#1E1E22] text-sm text-[#111111] dark:text-[#F3F3F5] placeholder:text-[#666A73] dark:placeholder:text-[#9A9EA8] border border-transparent focus:border-[#3F51B5] dark:focus:border-[#7986CB] focus:outline-none transition-colors"
          />
          {query && (
            <button
              type="button"
              aria-label="Clear search"
              onClick={() => onQueryChange('')}
              className="absolute right-1.5 w-8 h-8 rounded-full flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:text-[#111111] dark:hover:text-[#F3F3F5]"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Category Filter Row */}
      <div className="mt-2.5 px-5 flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
        <button
          type="button"
          onClick={() => onCategoryChange(null)}
          className={`shrink-0 h-8 px-3.5 rounded-full text-xs font-medium transition-colors whitespace-nowrap ${
            selectedCategory === null
              ? 'bg-[#3F51B5] dark:bg-[#7986CB] text-[#FFFFFF] dark:text-[#0E0E0E]'
              : 'border border-[#DCDCE2] dark:border-[#2F2F36] text-[#666A73] dark:text-[#9A9EA8]'
          }`}
        >
          All
        </button>
        {TERM_CATEGORIES.map((category) => {
          const active = selectedCategory === category;
          return (
            <button
              key={category}
              type="button"
              onClick={() => onCategoryChange(category)}
              className={`shrink-0 h-8 px-3.5 rounded-full text-xs font-medium transition-colors whitespace-nowrap ${
                active
                  ? 'bg-[#3F51B5] dark:bg-[#7986CB] text-[#FFFFFF] dark:text-[#0E0E0E]'
                  : 'border border-[#DCDCE2] dark:border-[#2F2F36] text-[#666A73] dark:text-[#9A9EA8]'
              }`}
            >
              {category}
            </button>
          );
        })}
      </div>

      {/* Has code filter + Term count */}
      <div className="px-5 py-2 flex items-center justify-between border-b border-[#E9E9EE] dark:border-[#242429]">
        <button
          type="button"
          onClick={onToggleHasCode}
          className={`inline-flex items-center gap-1.5 h-8 px-3 rounded-full text-xs font-medium transition-colors ${
            hasCodeOnly
              ? 'bg-[#3F51B5] dark:bg-[#7986CB] text-[#FFFFFF] dark:text-[#0E0E0E]'
              : 'border border-[#DCDCE2] dark:border-[#2F2F36] text-[#666A73] dark:text-[#9A9EA8]'
          }`}
        >
          <Code2 className="w-3.5 h-3.5" />
          <span>Has code</span>
        </button>

        <span className="text-xs font-medium tabular-nums text-[#666A73] dark:text-[#9A9EA8]">
          {terms.length === 1 ? '1 term' : `${terms.length} terms`}
        </span>
      </div>

      {/* Term List */}
      {terms.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center py-20 px-8 text-center gap-3">
          <SearchX className="w-9 h-9 text-[#666A73] dark:text-[#9A9EA8]" />
          <p className="text-base text-[#666A73] dark:text-[#9A9EA8]">No results found</p>
        </div>
      ) : (
        <div className="divide-y divide-[#E9E9EE] dark:divide-[#242429]">
          {terms.map((term) => (
            <button
              key={term.id}
              type="button"
              onClick={() => onTermClick(term)}
              className="w-full text-left px-5 py-3.5 min-h-[64px] hover:bg-[#F2F2F5]/60 dark:hover:bg-[#1E1E22]/50 transition-colors"
            >
              <div className="flex items-center justify-between gap-3 mb-1">
                <span className="text-[15px] font-semibold text-[#111111] dark:text-[#F3F3F5] truncate">
                  {term.term}
                </span>
                <span className="shrink-0 text-[11px] font-medium text-[#666A73] dark:text-[#9A9EA8]">
                  {term.category}
                </span>
              </div>
              <p className="font-myanmar text-[14px] leading-[1.6] text-[#666A73] dark:text-[#9A9EA8] truncate">
                {term.meaningMy}
              </p>
            </button>
          ))}
        </div>
      )}
    </main>
  );
}

interface TermDetailSheetProps {
  term: Term;
  onClose: () => void;
  onCopy: (text: string) => void;
}

function TermDetailSheet({ term, onClose, onCopy }: TermDetailSheetProps) {
  const [copiedTerm, setCopiedTerm] = useState(false);
  const [copiedCode, setCopiedCode] = useState(false);

  const codeLike = isCodeLikeTerm(term.term);
  const difficultyDot =
    term.difficulty.toLowerCase() === 'beginner'
      ? 'bg-[#4F8A65] dark:bg-[#6CA882]'
      : term.difficulty.toLowerCase() === 'intermediate'
      ? 'bg-[#B5863C] dark:bg-[#D1A45A]'
      : 'bg-[#B35858] dark:bg-[#C97171]';

  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="absolute inset-0 bg-black/45 backdrop-blur-[1px]"
      />

      <motion.div
        role="dialog"
        aria-modal="true"
        aria-label={term.term}
        initial={{ y: '100%' }}
        animate={{ y: 0 }}
        exit={{ y: '100%' }}
        transition={{ duration: 0.24, ease: [0.16, 1, 0.3, 1] }}
        className="relative z-10 w-full max-w-2xl max-h-[86vh] overflow-y-auto rounded-t-[24px] bg-[#FFFFFF] dark:bg-[#151517] text-[#111111] dark:text-[#F3F3F5] border-t border-x border-[#E9E9EE] dark:border-[#242429] px-6 pb-8 pt-3"
      >
        {/* Drag handle */}
        <div className="w-9 h-1 rounded-full bg-[#DCDCE2] dark:bg-[#2F2F36] mx-auto mb-4" />

        {/* Header */}
        <div className="flex items-center justify-between gap-3">
          <h2
            className={`text-2xl font-bold tracking-tight ${
              codeLike ? 'font-code' : ''
            }`}
          >
            {term.term}
          </h2>
          <div className="flex items-center gap-1">
            <button
              type="button"
              aria-label={copiedTerm ? 'Copied' : 'Copy term'}
              onClick={() => {
                onCopy(`${term.term}\n${term.meaningMy}`);
                setCopiedTerm(true);
                window.setTimeout(() => setCopiedTerm(false), 1500);
              }}
              className="w-10 h-10 rounded-full flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:bg-[#F2F2F5] dark:hover:bg-[#1E1E22]"
            >
              {copiedTerm ? (
                <Check className="w-5 h-5 text-[#3F51B5] dark:text-[#7986CB]" />
              ) : (
                <Copy className="w-5 h-5" />
              )}
            </button>
            <button
              type="button"
              aria-label="Close"
              onClick={onClose}
              className="w-10 h-10 rounded-full flex items-center justify-center text-[#666A73] dark:text-[#9A9EA8] hover:bg-[#F2F2F5] dark:hover:bg-[#1E1E22]"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Metadata row */}
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="px-3 py-1 rounded-full border border-[#DCDCE2] dark:border-[#2F2F36] text-[11px] font-medium text-[#666A73] dark:text-[#9A9EA8]">
            {term.category}
          </span>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full border border-[#DCDCE2] dark:border-[#2F2F36] text-[11px] font-medium text-[#666A73] dark:text-[#9A9EA8]">
            <span className={`w-1.5 h-1.5 rounded-full ${difficultyDot}`} />
            {term.difficulty}
          </span>
        </div>

        <hr className="my-5 border-[#E9E9EE] dark:border-[#242429]" />

        {/* Meaning (Burmese) */}
        <div className="space-y-5">
          <div>
            <h3 className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8] mb-1.5">
              Meaning (Burmese)
            </h3>
            <p className="font-myanmar text-base leading-[1.65] text-[#111111] dark:text-[#F3F3F5]">
              {term.meaningMy}
            </p>
          </div>

          {/* English meaning */}
          <div>
            <h3 className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8] mb-1.5">
              English meaning
            </h3>
            <p className="text-sm leading-relaxed text-[#666A73] dark:text-[#9A9EA8]">
              {term.meaningEn}
            </p>
          </div>

          {/* Example */}
          <div>
            <h3 className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8] mb-1.5">
              Example
            </h3>
            <div className="rounded-[14px] bg-[#F4F5F8] dark:bg-[#1A1A1F] px-4 py-3.5 border border-[#E9E9EE] dark:border-[#242429]">
              <p className="font-myanmar text-sm leading-[1.65] text-[#111111] dark:text-[#F3F3F5]">
                {term.example}
              </p>
            </div>
          </div>

          {/* How developers use it */}
          {term.usageNote && (
            <div>
              <h3 className="text-xs font-medium text-[#666A73] dark:text-[#9A9EA8] mb-1.5">
                How developers use it
              </h3>
              <p className="font-myanmar text-base leading-[1.65] text-[#111111] dark:text-[#F3F3F5]">
                {term.usageNote}
              </p>
            </div>
          )}

          {/* Code snippet */}
          {term.codeSnippet && (
            <div className="rounded-[14px] bg-[#F3F4F7] dark:bg-[#17181D] border border-[#E9E9EE] dark:border-[#242429] overflow-hidden">
              <div className="flex items-center justify-between pl-3.5 pr-1.5 h-11 border-b border-[#E9E9EE] dark:border-[#242429]">
                <span className="font-code text-[11px] text-[#666A73] dark:text-[#9A9EA8]">
                  {(term.codeLanguage || 'code').toLowerCase()}
                </span>
                <button
                  type="button"
                  onClick={() => {
                    onCopy(term.codeSnippet ?? '');
                    setCopiedCode(true);
                    window.setTimeout(() => setCopiedCode(false), 1500);
                  }}
                  className="inline-flex items-center gap-1.5 px-2.5 h-9 rounded-lg text-[11px] font-medium text-[#666A73] dark:text-[#9A9EA8] hover:text-[#111111] dark:hover:text-[#F3F3F5]"
                >
                  {copiedCode ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-[#3F51B5] dark:text-[#7986CB]" />
                      <span className="text-[#3F51B5] dark:text-[#7986CB]">COPIED</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>COPY</span>
                    </>
                  )}
                </button>
              </div>
              <pre className="overflow-x-auto px-3.5 py-3 font-code text-[13px] leading-5 whitespace-pre">
                <code>{renderHighlightedCode(term.codeSnippet)}</code>
              </pre>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
}

interface TimePickerDialogProps {
  initialHour: number;
  initialMinute: number;
  onCancel: () => void;
  onConfirm: (hour: number, minute: number) => void;
}

function TimePickerDialog({
  initialHour,
  initialMinute,
  onCancel,
  onConfirm,
}: TimePickerDialogProps) {
  const [value, setValue] = useState(
    `${String(initialHour).padStart(2, '0')}:${String(initialMinute).padStart(2, '0')}`
  );

  const handleSave = () => {
    const [hStr, mStr] = value.split(':');
    const h = Number(hStr);
    const m = Number(mStr);
    if (Number.isFinite(h) && Number.isFinite(m)) {
      onConfirm(Math.max(0, Math.min(23, h)), Math.max(0, Math.min(59, m)));
    } else {
      onCancel();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div onClick={onCancel} className="absolute inset-0 bg-black/45" />
      <motion.div
        role="dialog"
        aria-modal="true"
        aria-label="Notification time"
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        exit={{ opacity: 0, scale: 0.96 }}
        transition={{ duration: 0.16 }}
        className="relative z-10 w-full max-w-xs rounded-[16px] bg-[#FFFFFF] dark:bg-[#151517] border border-[#E9E9EE] dark:border-[#242429] p-5"
      >
        <h2 className="text-base font-semibold text-[#111111] dark:text-[#F3F3F5] mb-4">
          Notification time
        </h2>
        <input
          type="time"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          className="w-full h-12 px-3 rounded-xl bg-[#F2F2F5] dark:bg-[#1E1E22] text-[#111111] dark:text-[#F3F3F5] text-lg font-medium border border-[#DCDCE2] dark:border-[#2F2F36] focus:border-[#3F51B5] focus:outline-none"
        />
        <div className="mt-5 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onCancel}
            className="h-10 px-4 rounded-lg text-sm font-medium text-[#666A73] dark:text-[#9A9EA8] hover:bg-[#F2F2F5] dark:hover:bg-[#1E1E22]"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={handleSave}
            className="h-10 px-4 rounded-lg text-sm font-medium text-[#3F51B5] dark:text-[#7986CB] hover:bg-[#EEF0FA] dark:hover:bg-[#252D5C]"
          >
            OK
          </button>
        </div>
      </motion.div>
    </div>
  );
}
