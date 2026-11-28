'use client';

import { useMemo, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Edit3,
  Eye,
  EyeOff,
  FilePlus2,
  Plus,
  Search,
  Trash2,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';
import type {
  GuardianQuizSettingsResponse,
  MoveDirection,
  QuizQuestionResponse,
} from '@/types';

interface GuardianQuizBankPanelProps {
  questions: QuizQuestionResponse[];
  settings: GuardianQuizSettingsResponse;
  isBusy: boolean;
  onAddSingle: () => void;
  onAddBulk: () => void;
  onEdit: (question: QuizQuestionResponse) => void;
  onToggleActive: (question: QuizQuestionResponse) => void;
  onMove: (questionId: number, direction: MoveDirection) => void;
  onDelete: (questionId: number) => void;
}

export function GuardianQuizBankPanel({
  questions,
  settings,
  isBusy,
  onAddSingle,
  onAddBulk,
  onEdit,
  onToggleActive,
  onMove,
  onDelete,
}: GuardianQuizBankPanelProps) {
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'PAUSED'>(
    'ALL'
  );
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  // Map active question ID -> 1-based active index & rotation batch number (1-10 -> Lô 1, 11-20 -> Lô 2...)
  const activeBatchMap = useMemo(() => {
    const map = new Map<number, { activePos: number; batchIndex: number }>();
    const perDay = Math.max(1, settings.questionsPerDay);
    let pos = 0;
    for (const q of questions) {
      if (!q.active) continue;
      const batchIndex = Math.floor(pos / perDay) + 1;
      map.set(q.id, { activePos: pos + 1, batchIndex });
      pos += 1;
    }
    return map;
  }, [questions, settings.questionsPerDay]);

  const filtered = useMemo(() => {
    const needle = searchText.trim().toLowerCase();
    return questions.filter((q) => {
      if (statusFilter === 'ACTIVE' && !q.active) return false;
      if (statusFilter === 'PAUSED' && q.active) return false;
      if (!needle) return true;
      return (
        q.questionText.toLowerCase().includes(needle) ||
        (q.topicLabel?.toLowerCase().includes(needle) ?? false) ||
        q.optionA.toLowerCase().includes(needle) ||
        q.optionB.toLowerCase().includes(needle)
      );
    });
  }, [questions, searchText, statusFilter]);

  return (
    <Card>
      <CardHeader className="border-b bg-brand-soft/25 dark:bg-sky-950/20">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <CardTitle className="text-base font-bold">
              Ngân hàng câu hỏi 2 lựa chọn ({settings.activeQuestionCount} đang bật /{' '}
              {settings.totalQuestionCount} tổng)
            </CardTitle>
            <p className="mt-1 text-xs text-muted-foreground">
              Mỗi lô {settings.questionsPerDay} câu đang bật sẽ lần lượt hiện cho con qua từng ngày.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onAddBulk}
              disabled={isBusy}
              className="h-10 gap-1.5"
            >
              <FilePlus2 className="h-4 w-4" strokeWidth={1.75} />
              <span>Thêm nhanh nhiều câu</span>
            </Button>
            <Button
              type="button"
              size="sm"
              onClick={onAddSingle}
              disabled={isBusy}
              className="h-10 gap-1.5"
            >
              <Plus className="h-4 w-4" strokeWidth={1.75} />
              <span>Thêm 1 câu</span>
            </Button>
          </div>
        </div>
      </CardHeader>

      <CardContent className="space-y-4 p-4 sm:p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative flex-1 sm:max-w-xs">
            <Search
              className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground"
              strokeWidth={1.75}
            />
            <Input
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              placeholder="Tìm câu hỏi hoặc chủ đề..."
              className="pl-9"
            />
          </div>

          <div className="flex items-center gap-1.5">
            {(
              [
                { key: 'ALL', label: 'Tất cả' },
                { key: 'ACTIVE', label: 'Đang bật' },
                { key: 'PAUSED', label: 'Đang tắt' },
              ] as const
            ).map((tab) => (
              <Button
                key={tab.key}
                type="button"
                size="sm"
                variant={statusFilter === tab.key ? 'default' : 'outline'}
                onClick={() => setStatusFilter(tab.key)}
                className="h-9 text-xs"
              >
                {tab.label}
              </Button>
            ))}
          </div>
        </div>

        {filtered.length === 0 ? (
          <div className="rounded-xl border border-dashed bg-muted/30 p-8 text-center text-sm text-muted-foreground">
            {questions.length === 0
              ? 'Chưa có câu hỏi nào. Hãy bấm "Thêm 1 câu" hoặc "Thêm nhanh nhiều câu" để bắt đầu tạo bộ câu hỏi cho con.'
              : 'Không tìm thấy câu hỏi phù hợp với bộ lọc hiện tại.'}
          </div>
        ) : (
          <div className="space-y-3">
            {filtered.map((q) => {
              const batchInfo = activeBatchMap.get(q.id);
              const originalIndex = questions.findIndex((item) => item.id === q.id);

              return (
                <div
                  key={q.id}
                  className={cn(
                    'rounded-xl border p-4 transition-colors',
                    q.active
                      ? 'bg-card'
                      : 'border-dashed bg-muted/40 opacity-75'
                  )}
                >
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div className="min-w-0 flex-1 space-y-2">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <Badge variant="brand" className="font-mono">
                          #{q.sortOrder}
                        </Badge>
                        {batchInfo &&
                        settings.selectionMode === 'SEQUENTIAL_ROTATION' ? (
                          <Badge variant="info" className="font-mono">
                            Lô {batchInfo.batchIndex} (Câu {batchInfo.activePos})
                          </Badge>
                        ) : null}
                        {q.topicLabel ? (
                          <Badge variant="secondary">{q.topicLabel}</Badge>
                        ) : null}
                        {!q.active ? (
                          <Badge variant="secondary">Tạm tắt</Badge>
                        ) : null}
                      </div>

                      <p className="break-words text-sm font-semibold text-foreground sm:text-base [overflow-wrap:anywhere]">
                        {q.questionText}
                      </p>

                      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
                        {(['A', 'B'] as const).map((opt) => {
                          const isCorrect = q.correctOption === opt;
                          const label = opt === 'A' ? q.optionA : q.optionB;
                          return (
                            <div
                              key={opt}
                              className={cn(
                                'flex items-center gap-2 rounded-lg border px-3 py-2 text-xs sm:text-sm',
                                isCorrect
                                  ? 'border-emerald-300 bg-emerald-50/70 font-semibold text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/35 dark:text-emerald-200'
                                  : 'bg-background text-muted-foreground'
                              )}
                            >
                              <span className="font-mono font-bold">{opt}.</span>
                              <span className="min-w-0 flex-1 break-words [overflow-wrap:anywhere]">
                                {label}
                              </span>
                              {isCorrect ? (
                                <CheckCircle2
                                  className="h-4 w-4 shrink-0 text-emerald-600 dark:text-emerald-400"
                                  strokeWidth={2}
                                />
                              ) : null}
                            </div>
                          );
                        })}
                      </div>

                      {q.explanation ? (
                        <p className="break-words text-xs text-muted-foreground [overflow-wrap:anywhere]">
                          Giải thích: {q.explanation}
                        </p>
                      ) : null}
                    </div>

                    <div className="flex shrink-0 flex-wrap items-center gap-1.5">
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        disabled={isBusy || originalIndex <= 0}
                        onClick={() => onMove(q.id, 'UP')}
                        aria-label="Đưa lên"
                        className="h-10 w-10 sm:h-9 sm:w-9"
                      >
                        <ArrowUp className="h-4 w-4" strokeWidth={1.75} />
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        disabled={isBusy || originalIndex >= questions.length - 1}
                        onClick={() => onMove(q.id, 'DOWN')}
                        aria-label="Đưa xuống"
                        className="h-10 w-10 sm:h-9 sm:w-9"
                      >
                        <ArrowDown className="h-4 w-4" strokeWidth={1.75} />
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        disabled={isBusy}
                        onClick={() => onToggleActive(q)}
                        aria-label={q.active ? 'Tạm tắt câu hỏi' : 'Bật lại câu hỏi'}
                        title={q.active ? 'Tạm tắt câu hỏi' : 'Bật lại câu hỏi'}
                        className="h-10 w-10 sm:h-9 sm:w-9"
                      >
                        {q.active ? (
                          <Eye className="h-4 w-4" strokeWidth={1.75} />
                        ) : (
                          <EyeOff className="h-4 w-4" strokeWidth={1.75} />
                        )}
                      </Button>
                      <Button
                        type="button"
                        variant="outline"
                        size="icon"
                        disabled={isBusy}
                        onClick={() => onEdit(q)}
                        aria-label="Sửa câu hỏi"
                        className="h-10 w-10 sm:h-9 sm:w-9"
                      >
                        <Edit3 className="h-4 w-4" strokeWidth={1.75} />
                      </Button>
                      {confirmDeleteId === q.id ? (
                        <div className="flex items-center gap-1">
                          <Button
                            type="button"
                            size="sm"
                            variant="destructive"
                            disabled={isBusy}
                            onClick={() => {
                              setConfirmDeleteId(null);
                              onDelete(q.id);
                            }}
                            className="h-10 px-2.5 text-xs sm:h-9"
                          >
                            Xác nhận xóa
                          </Button>
                          <Button
                            type="button"
                            size="sm"
                            variant="ghost"
                            disabled={isBusy}
                            onClick={() => setConfirmDeleteId(null)}
                            className="h-10 px-2 text-xs sm:h-9"
                          >
                            Hủy
                          </Button>
                        </div>
                      ) : (
                        <Button
                          type="button"
                          variant="outline"
                          size="icon"
                          disabled={isBusy}
                          onClick={() => setConfirmDeleteId(q.id)}
                          aria-label="Xóa câu hỏi"
                          className="h-10 w-10 text-rose-600 hover:bg-rose-50 hover:text-rose-700 dark:text-rose-400 sm:h-9 sm:w-9"
                        >
                          <Trash2 className="h-4 w-4" strokeWidth={1.75} />
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
