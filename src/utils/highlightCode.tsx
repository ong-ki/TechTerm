import React from 'react';

const COMMON_KEYWORDS = new Set([
  'fun', 'val', 'var', 'class', 'interface', 'object', 'data', 'override', 'suspend',
  'return', 'if', 'else', 'when', 'while', 'for', 'in', 'import', 'private', 'const',
  'let', 'async', 'await', 'export', 'default', 'throw', 'new', 'def', 'print', 'true', 'false', 'null',
  'SELECT', 'FROM', 'WHERE', 'INNER', 'LEFT', 'JOIN', 'ON', 'ORDER', 'BY', 'ASC', 'DESC',
  'GROUP', 'CREATE', 'TABLE', 'INDEX', 'ALTER', 'ADD', 'COLUMN', 'UPDATE', 'SET',
  'BEGIN', 'TRANSACTION', 'COMMIT', 'AND', 'OR', 'NOT', 'NULL', 'PRIMARY', 'KEY', 'REFERENCES', 'EXPLAIN', 'QUERY', 'PLAN',
  'GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'HTTP/1.1',
  'WORKDIR', 'COPY', 'RUN', 'CMD',
  'git', 'docker', 'kubectl', 'sudo', 'curl', 'grep', 'chmod', 'ssh', 'tar', 'sed', 'resource', 'syntax', 'message', 'query',
]);

const TOKEN_REGEX = /("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'|\b\d+(?:\.\d+)?\b|\b[A-Za-z_][A-Za-z0-9_/.-]*\b)/g;

function findLineCommentStart(line: string): number {
  let inDouble = false;
  let inSingle = false;
  for (let i = 0; i < line.length; i++) {
    const c = line[i];
    const prev = i > 0 ? line[i - 1] : '';
    if (c === '"' && !inSingle && prev !== '\\') {
      inDouble = !inDouble;
    } else if (c === "'" && !inDouble && prev !== '\\') {
      inSingle = !inSingle;
    } else if (!inDouble && !inSingle) {
      if (c === '#' && (i === 0 || /\s/.test(line[i - 1]))) return i;
      if (c === '/' && line[i + 1] === '/' && (i === 0 || /\s/.test(line[i - 1]))) return i;
      if (
        c === '-' &&
        line[i + 1] === '-' &&
        (i === 0 || /\s/.test(line[i - 1])) &&
        (i + 2 >= line.length || /\s/.test(line[i + 2]))
      ) {
        return i;
      }
    }
  }
  return -1;
}

export function renderHighlightedCode(code: string): React.ReactNode {
  const lines = code.split('\n');
  return lines.map((line, lineIdx) => {
    const commentStart = findLineCommentStart(line);
    const codePart = commentStart >= 0 ? line.slice(0, commentStart) : line;
    const commentPart = commentStart >= 0 ? line.slice(commentStart) : '';

    const nodes: React.ReactNode[] = [];
    let cursor = 0;
    TOKEN_REGEX.lastIndex = 0;
    let match: RegExpExecArray | null;

    while ((match = TOKEN_REGEX.exec(codePart)) !== null) {
      const start = match.index;
      if (start > cursor) {
        nodes.push(
          <span key={`p-${cursor}`} className="text-[#111111] dark:text-[#F3F3F5]">
            {codePart.slice(cursor, start)}
          </span>
        );
      }

      const token = match[0];
      let className = 'text-[#111111] dark:text-[#F3F3F5]';
      if (
        (token.startsWith('"') && token.endsWith('"')) ||
        (token.startsWith("'") && token.endsWith("'"))
      ) {
        className = 'text-[#3A7D54] dark:text-[#7EC295]';
      } else if (/^\d/.test(token)) {
        className = 'text-[#A66B2B] dark:text-[#D9A05B]';
      } else if (COMMON_KEYWORDS.has(token)) {
        className = 'text-[#3F51B5] dark:text-[#8C9EFF] font-medium';
      }

      nodes.push(
        <span key={`t-${start}`} className={className}>
          {token}
        </span>
      );
      cursor = start + token.length;
    }

    if (cursor < codePart.length) {
      nodes.push(
        <span key={`r-${cursor}`} className="text-[#111111] dark:text-[#F3F3F5]">
          {codePart.slice(cursor)}
        </span>
      );
    }

    if (commentPart) {
      nodes.push(
        <span key="c" className="italic text-[#7D828C] dark:text-[#7E8490]">
          {commentPart}
        </span>
      );
    }

    return (
      <React.Fragment key={lineIdx}>
        {nodes}
        {lineIdx < lines.length - 1 ? '\n' : null}
      </React.Fragment>
    );
  });
}

export function isCodeLikeTerm(term: string): boolean {
  const trimmed = term.trim();
  if (
    trimmed.startsWith('.') ||
    trimmed.includes('/') ||
    (trimmed.includes('-') && /[A-Z]/.test(trimmed))
  ) {
    return true;
  }
  if (!trimmed.includes(' ')) {
    const upperCount = (trimmed.match(/[A-Z]/g) || []).length;
    if (upperCount >= 2) return true;
    if (trimmed === trimmed.toLowerCase()) return true;
    if (['Git', 'Docker', 'OAuth', 'GraphQL', 'NoSQL', 'SQLite', 'WebSocket', 'Regex'].includes(trimmed)) {
      return true;
    }
  }
  return ['REST API', 'IP Address', 'MAC Address', 'Fetch API'].includes(trimmed);
}
