'use client';

import { CheckCircle2, Loader2, Send, ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  ANTI_PASTE_NOTICE,
  isBlockedBeforeInputType,
  isSuddenBulkInsert,
} from '@/lib/anti-paste';
import {
  MAX_POST_EVAL_REVISIONS,
  MAX_SUBMISSION_CHARS,
  MIN_SUBMISSION_CHARS,
} from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import type { StudySubmissionResponse } from '@/types';

interface StudyEssayEditorProps {
  submissionsCount: number;
  revisionCount: number;
  maxRevisions: number;
  lessonTitle: string;
  content: string;
  normalizedCount: number;
  lengthValid: boolean;
  progressPct: number;
  pasteBlockedNotice: boolean;
  draftSavedAt: string | null;
  isEvaluating: boolean;
  isSubmitting: boolean;
  onLessonTitleChange: (value: string) => void;
  onContentChange: (value: string) => void;
  onNotifyPasteBlocked: () => void;
  onClearDraft: () => void;
  onCancelRevision: () => void;
  onSubmit: () => void;
}

export function StudyEssayEditor({
  submissionsCount,
  revisionCount,
  maxRevisions,
  lessonTitle,
  content,
  normalizedCount,
  lengthValid,
  progressPct,
  pasteBlockedNotice,
  draftSavedAt,
  isEvaluating,
  isSubmitting,
  onLessonTitleChange,
  onContentChange,
  onNotifyPasteBlocked,
  onClearDraft,
  onCancelRevision,
  onSubmit,
}: StudyEssayEditorProps) {
  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-lg">
          {submissionsCount > 0
            ? `Viết bản chỉnh sửa (Lần ${revisionCount + 1}/${maxRevisions})`
            : 'Viết bài của con'}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="space-y-1.5">
          <Label htmlFor="study-lesson-title">Tên bài học</Label>
          <Input
            id="study-lesson-title"
            value={lessonTitle}
            onChange={(e) => onLessonTitleChange(e.target.value)}
            maxLength={200}
            placeholder="VD: Bài 11: Ước chung và Ước chung lớn nhất"
            className="h-11 text-base md:text-sm"
          />
        </div>

        <div className="space-y-2">
          <div className="flex items-center justify-between gap-2">
            <Label htmlFor="study-content-editor">
              Nội dung tóm tắt kiến thức
            </Label>
            <span
              className={cn(
                'font-mono text-xs font-semibold',
                lengthValid
                  ? 'text-emerald-600 dark:text-emerald-400'
                  : normalizedCount > MAX_SUBMISSION_CHARS
                    ? 'text-rose-600 dark:text-rose-400'
                    : 'text-brand-strong dark:text-sky-300'
              )}
            >
              {normalizedCount} / {MAX_SUBMISSION_CHARS} ký tự
            </span>
          </div>

          {pasteBlockedNotice ? (
            <div
              role="status"
              className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-xs font-medium text-amber-900 dark:border-amber-800 dark:bg-amber-950/40 dark:text-amber-200"
            >
              <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0 text-amber-600 dark:text-amber-400" />
              <span>{ANTI_PASTE_NOTICE}</span>
            </div>
          ) : null}

          <textarea
            id="study-content-editor"
            value={content}
            onPaste={(e) => {
              e.preventDefault();
              onNotifyPasteBlocked();
            }}
            onDrop={(e) => {
              e.preventDefault();
              onNotifyPasteBlocked();
            }}
            onContextMenu={(e) => {
              e.preventDefault();
              onNotifyPasteBlocked();
            }}
            onKeyDown={(e) => {
              const key = e.key.toLowerCase();
              if (
                ((e.ctrlKey || e.metaKey) && key === 'v') ||
                (e.shiftKey && e.key === 'Insert')
              ) {
                e.preventDefault();
                onNotifyPasteBlocked();
              }
            }}
            onBeforeInput={(e) => {
              const native = e.nativeEvent;
              const inputType =
                'inputType' in native && typeof native.inputType === 'string'
                  ? native.inputType
                  : '';
              if (isBlockedBeforeInputType(inputType)) {
                e.preventDefault();
                onNotifyPasteBlocked();
              }
            }}
            onChange={(e) => {
              const nextValue = e.target.value;
              if (isSuddenBulkInsert(content, nextValue)) {
                onNotifyPasteBlocked();
                return;
              }
              onContentChange(nextValue);
            }}
            rows={12}
            placeholder="Con tự gõ các ý chính đã học hoặc tóm tắt bài mới bằng lời hiểu của mình (400–3.000 ký tự, không hỗ trợ dán văn bản)..."
            className="min-h-[210px] w-full rounded-lg border bg-background p-3.5 text-base leading-relaxed text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring sm:min-h-[300px] md:text-sm xl:min-h-[420px]"
          />

          <p className="flex items-center gap-1.5 text-[11px] text-muted-foreground">
            <ShieldAlert className="h-3.5 w-3.5 shrink-0 text-brand-strong dark:text-sky-400" />
            <span>
              Chế độ tự học trung thực: vui lòng tự gõ nội dung, hệ thống chặn dán văn bản từ bên ngoài.
            </span>
          </p>

          <div className="space-y-1.5 rounded-lg border bg-muted/35 p-3">
            <div className="h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                className={cn(
                  'h-full transition-all',
                  lengthValid
                    ? 'bg-emerald-500'
                    : normalizedCount > MAX_SUBMISSION_CHARS
                      ? 'bg-rose-500'
                      : 'bg-brand-strong'
                )}
                style={{ width: `${progressPct}%` }}
              />
            </div>
            <p className="text-xs text-muted-foreground">
              {normalizedCount < MIN_SUBMISSION_CHARS ? (
                <>
                  Cần thêm{' '}
                  <strong className="text-foreground">
                    {MIN_SUBMISSION_CHARS - normalizedCount}
                  </strong>{' '}
                  ký tự nữa để đạt tối thiểu {MIN_SUBMISSION_CHARS}.
                </>
              ) : normalizedCount > MAX_SUBMISSION_CHARS ? (
                <span className="font-semibold text-rose-600 dark:text-rose-400">
                  Đang vượt quá {normalizedCount - MAX_SUBMISSION_CHARS} ký tự
                  (tối đa {MAX_SUBMISSION_CHARS}).
                </span>
              ) : (
                <span className="inline-flex items-center gap-1 font-semibold text-emerald-700 dark:text-emerald-400">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Độ dài hợp lệ ({MIN_SUBMISSION_CHARS}–{MAX_SUBMISSION_CHARS}{' '}
                  ký tự). Con có thể nộp bài!
                </span>
              )}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-2.5 pt-2">
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            {draftSavedAt ? (
              <>
                <span>Đã lưu nháp tự động lúc {draftSavedAt}</span>
                <button
                  type="button"
                  onClick={onClearDraft}
                  className="font-semibold text-rose-600 hover:underline dark:text-rose-400"
                >
                  Xóa nháp
                </button>
              </>
            ) : (
              <span>Nháp được tự động lưu trên trình duyệt</span>
            )}
          </div>

          <div className="flex w-full flex-col-reverse gap-2 sm:w-auto sm:flex-row sm:items-center sm:gap-2.5">
            {submissionsCount > 0 ? (
              <Button
                type="button"
                variant="outline"
                className="h-11 w-full sm:w-auto"
                onClick={onCancelRevision}
              >
                Hủy viết bản mới
              </Button>
            ) : null}

            <Button
              type="button"
              className="h-11 w-full gap-2 px-6 text-sm font-semibold sm:w-auto"
              disabled={
                !lengthValid ||
                !lessonTitle.trim() ||
                isEvaluating ||
                isSubmitting
              }
              onClick={onSubmit}
            >
              {isSubmitting ? (
                <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
              ) : (
                <Send className="h-4 w-4" strokeWidth={1.75} />
              )}
              Nộp bài để AI xem
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

interface SubmittedEssayCardProps {
  submission: StudySubmissionResponse;
  submissionsCount: number;
  revisionCount: number;
}

export function SubmittedEssayCard({
  submission,
  submissionsCount,
  revisionCount,
}: SubmittedEssayCardProps) {
  return (
    <Card>
      <CardHeader className="border-b pb-3.5">
        <CardTitle className="text-base">
          Bài đã nộp (Bản {submission.version} · {submission.charCount} ký tự)
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3 pt-4">
        <p className="break-words text-sm font-semibold text-foreground [overflow-wrap:anywhere]">
          Bài: {submission.lessonTitle}
        </p>
        <div className="whitespace-pre-wrap break-words rounded-lg border bg-muted/25 p-4 text-sm leading-relaxed text-foreground [overflow-wrap:anywhere]">
          {submission.content}
        </div>
        {submissionsCount >= 3 || revisionCount >= MAX_POST_EVAL_REVISIONS ? (
          <p className="text-xs text-muted-foreground">
            Con đã nộp đủ tối đa 3 phiên bản (v1–v3) cho bài này.
          </p>
        ) : null}
      </CardContent>
    </Card>
  );
}
