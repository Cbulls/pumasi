/** 안정 시드 셔플 — 같은 seed면 같은 순서 (응답 중 깜빡임 방지) */
export function seededShuffle<T>(items: T[], seed: string): T[] {
  const arr = [...items];
  let h = 2166136261;
  for (let i = 0; i < seed.length; i++) {
    h ^= seed.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  for (let i = arr.length - 1; i > 0; i--) {
    h = Math.imul(h ^ (h >>> 15), 2246822507);
    h = Math.imul(h ^ (h >>> 13), 3266489909);
    h ^= h >>> 16;
    const j = (h >>> 0) % (i + 1);
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

export function respondShuffleSeed(questionId: string): string {
  if (typeof window === "undefined") return questionId;
  const key = "pumasi.shuffle.session";
  let sid = window.sessionStorage.getItem(key);
  if (!sid) {
    sid = crypto.randomUUID();
    window.sessionStorage.setItem(key, sid);
  }
  return `${sid}:${questionId}`;
}
