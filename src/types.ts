export interface Term {
  id: number;
  term: string;
  category: string;
  difficulty: string;
  meaningMy: string;
  meaningEn: string;
  example: string;
  codeSnippet?: string | null;
  codeLanguage?: string | null;
  usageNote?: string | null;
}

export const TERM_CATEGORIES: readonly string[] = [
  'Programming',
  'Networking',
  'AI',
  'Security',
  'Hardware',
  'Web',
  'Mobile',
  'Database',
  'Cloud',
  'Git & Version Control',
  'Linux & Terminal',
  'DevOps',
  'Software Design',
  'Data & Formats',
];
