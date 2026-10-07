export function getTodayStr(): string {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function formatDateStr(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function addDays(dateStr: string, days: number): string {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  date.setDate(date.getDate() + days);
  return formatDateStr(date);
}

export function parseFlexibleDate(dateStr: string): Date | null {
  const raw = dateStr.trim();
  if (!raw) return null;

  const lower = raw.toLowerCase();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  if (lower === 'today') {
    return today;
  }
  if (lower === 'tomorrow') {
    const tm = new Date(today);
    tm.setDate(tm.getDate() + 1);
    return tm;
  }

  const inDayMatch = lower.match(/^in\s+(\d+)\s+day/);
  if (inDayMatch) {
    const num = parseInt(inDayMatch[1], 10);
    const target = new Date(today);
    target.setDate(target.getDate() + num);
    return target;
  }

  const inWkMatch = lower.match(/^in\s+(\d+)\s+(?:week|wk)/);
  if (inWkMatch) {
    const num = parseInt(inWkMatch[1], 10);
    const target = new Date(today);
    target.setDate(target.getDate() + num * 7);
    return target;
  }

  // ISO check YYYY-MM-DD
  const isoMatch = raw.match(/^(\d{4})-(\d{1,2})-(\d{1,2})$/);
  if (isoMatch) {
    const y = parseInt(isoMatch[1], 10);
    const m = parseInt(isoMatch[2], 10) - 1;
    const d = parseInt(isoMatch[3], 10);
    return new Date(y, m, d);
  }

  // DD/MM/YYYY or DD-MM-YYYY
  const dmyMatch = raw.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$/);
  if (dmyMatch) {
    const d = parseInt(dmyMatch[1], 10);
    const m = parseInt(dmyMatch[2], 10) - 1;
    const y = parseInt(dmyMatch[3], 10);
    return new Date(y, m, d);
  }

  // Native parse fallback
  const parsed = new Date(raw);
  if (!isNaN(parsed.getTime())) {
    return parsed;
  }

  return null;
}

export function normalizeDate(dateStr: string): string {
  const parsed = parseFlexibleDate(dateStr);
  if (!parsed) return dateStr.trim();
  return formatDateStr(parsed);
}

export function calculateEndTime(startTime: string, durationMinutes: number): string {
  try {
    const isPm = startTime.toLowerCase().includes('pm');
    const isAm = startTime.toLowerCase().includes('am');
    const cleanTime = startTime.replace(/(am|pm)/i, '').trim();
    const parts = cleanTime.split(':');
    let hour = parseInt(parts[0], 10);
    const min = parts[1] ? parseInt(parts[1], 10) : 0;

    if (isPm && hour !== 12) hour += 12;
    if (isAm && hour === 12) hour = 0;

    const totalMin = hour * 60 + min + durationMinutes;
    const endHour = Math.floor(totalMin / 60) % 24;
    const endMin = totalMin % 60;

    return `${String(endHour).padStart(2, '0')}:${String(endMin).padStart(2, '0')}`;
  } catch {
    return '12:00';
  }
}

export function formatReadableDate(dateStr: string): string {
  try {
    const [y, m, d] = dateStr.split('-').map(Number);
    const date = new Date(y, m - 1, d);
    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

export function getDaysRemaining(dateStr: string | null | undefined): number | null {
  if (!dateStr || !dateStr.trim()) return null;
  const target = parseFlexibleDate(dateStr);
  if (!target) return null;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  target.setHours(0, 0, 0, 0);
  const diffTime = target.getTime() - today.getTime();
  return Math.round(diffTime / (1000 * 60 * 60 * 24));
}

export function formatExamDateStatus(dateStr: string | null | undefined): {
  label: string;
  badgeClass: string;
  days: number | null;
  status: 'NONE' | 'PASSED' | 'TODAY' | 'TOMORROW' | 'APPROACHING' | 'FUTURE';
} {
  const days = getDaysRemaining(dateStr);
  if (days === null) {
    return {
      label: 'No exam date set',
      badgeClass: 'bg-slate-100 text-slate-500 border-slate-200',
      days: null,
      status: 'NONE',
    };
  }
  if (days < 0) {
    return {
      label: 'Exam completed',
      badgeClass: 'bg-slate-100 text-slate-500 border-slate-200',
      days,
      status: 'PASSED',
    };
  }
  if (days === 0) {
    return {
      label: 'Exam today!',
      badgeClass: 'bg-rose-100 text-rose-800 border-rose-300 animate-pulse font-extrabold',
      days: 0,
      status: 'TODAY',
    };
  }
  if (days === 1) {
    return {
      label: 'Exam tomorrow (1 day remaining)',
      badgeClass: 'bg-rose-100 text-rose-800 border-rose-300 font-bold',
      days: 1,
      status: 'TOMORROW',
    };
  }
  if (days <= 7) {
    return {
      label: `Exam approaching (${days} days remaining)`,
      badgeClass: 'bg-amber-100 text-amber-800 border-amber-300 font-bold',
      days,
      status: 'APPROACHING',
    };
  }
  return {
    label: `Exam in ${days} days`,
    badgeClass: 'bg-indigo-100 text-indigo-800 border-indigo-200 font-semibold',
    days,
    status: 'FUTURE',
  };
}

