export const ANTI_PASTE_NOTICE =
  'Ô nội dung không hỗ trợ dán văn bản (Ctrl+V / chuột phải / kéo thả). Con hãy tự gõ lại bằng lời hiểu của mình nhé!';

export const MAX_SINGLE_INPUT_DELTA = 30;

const BLOCKED_BEFORE_INPUT_TYPES = new Set<string>([
  'insertFromPaste',
  'insertFromPasteAsQuotation',
  'insertFromDrop',
  'insertFromYank',
]);

/**
 * Checks whether a DOM InputEvent.inputType represents pasting, dropping, or yanking external text.
 */
export function isBlockedBeforeInputType(inputType: string): boolean {
  return BLOCKED_BEFORE_INPUT_TYPES.has(inputType);
}

/**
 * Fallback bulk-insert detector catching external clipboard managers or programmatic injections
 * while safely permitting Vietnamese IME (Telex/VNI) and mobile keyboard suggestions.
 */
export function isSuddenBulkInsert(
  previousValue: string,
  nextValue: string
): boolean {
  return nextValue.length - previousValue.length > MAX_SINGLE_INPUT_DELTA;
}
