'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CalendarDays,
  Check,
  History,
  Loader2,
  Save,
} from 'lucide-react';
import { toast } from 'sonner';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import {
  GRADE_6_SUBJECTS,
  WEEKDAYS,
  formatViDate,
} from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { timetableService } from '@/services';
import type { SubjectCode, TimetableEntryDto, Weekday } from '@/types';

function makeCellKey(weekday: Weekday, subjectCode: SubjectCode): string {
  return `${weekday}:${subjectCode}`;
}

export function TimetableView() {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const [draftMap, setDraftMap] = useState<Map<string, TimetableEntryDto>>(
    () => new Map()
  );
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

  const timetableQuery = useQuery({
    queryKey: ['student-timetable', runtime.resetCounter, runtime.scenario, runtime.simulatedRole],
    queryFn: () => timetableService.getTimetable(),
  });

  useEffect(() => {
    if (!timetableQuery.data) {
      return;
    }
    const nextMap = new Map<string, TimetableEntryDto>();
    for (const entry of timetableQuery.data.entries) {
      nextMap.set(makeCellKey(entry.weekday, entry.subjectCode), {
        weekday: entry.weekday,
        subjectCode: entry.subjectCode,
        periodNote: entry.periodNote,
      });
    }
    setDraftMap(nextMap);
    setHasUnsavedChanges(false);
  }, [timetableQuery.data]);

  const saveMutation = useMutation({
    mutationFn: (entries: TimetableEntryDto[]) =>
      timetableService.updateTimetable({ entries }),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        ['student-timetable', runtime.resetCounter, runtime.scenario, runtime.simulatedRole],
        saved
      );
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      setHasUnsavedChanges(false);
      toast.success(`Đã lưu thời khóa biểu (Phiên bản v${saved.version})`);
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const toggleSubjectCell = (weekday: Weekday, subjectCode: SubjectCode) => {
    const key = makeCellKey(weekday, subjectCode);
    setDraftMap((prev) => {
      const next = new Map(prev);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.set(key, { weekday, subjectCode, periodNote: '' });
      }
      return next;
    });
    setHasUnsavedChanges(true);
  };

  const updatePeriodNote = (
    weekday: Weekday,
    subjectCode: SubjectCode,
    periodNote: string
  ) => {
    const key = makeCellKey(weekday, subjectCode);
    setDraftMap((prev) => {
      const existing = prev.get(key);
      if (!existing) {
        return prev;
      }
      const next = new Map(prev);
      next.set(key, { ...existing, periodNote });
      return next;
    });
    setHasUnsavedChanges(true);
  };

  const handleSave = () => {
    const entries = Array.from(draftMap.values()).sort(
      (a, b) => a.weekday - b.weekday
    );
    saveMutation.mutate(entries);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 rounded-xl border bg-card p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <CalendarDays
              className="h-5 w-5 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
              Thời khóa biểu tuần
            </h1>
            {timetableQuery.data ? (
              <Badge variant="brand">Phiên bản v{timetableQuery.data.version}</Badge>
            ) : null}
          </div>
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <History className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} />
            <span>
              Chỉnh lịch chỉ áp dụng cho ngày mới, lịch sử bài cũ luôn được giữ nguyên.
            </span>
          </p>
        </div>

        <Button
          type="button"
          className="h-11 gap-2 px-5 text-sm font-semibold"
          disabled={saveMutation.isPending || timetableQuery.isLoading}
          onClick={handleSave}
        >
          {saveMutation.isPending ? (
            <Loader2 className="h-4 w-4 animate-spin" strokeWidth={1.75} />
          ) : (
            <Save className="h-4 w-4" strokeWidth={1.75} />
          )}
          {hasUnsavedChanges ? 'Lưu thay đổi' : 'Lưu thời khóa biểu'}
        </Button>
      </div>

      <QueryStateBoundary
        data={timetableQuery.data}
        isLoading={timetableQuery.isLoading}
        isError={timetableQuery.isError}
        error={timetableQuery.error}
        onRetry={() => void timetableQuery.refetch()}
      >
        {(timetable) => (
          <>
            {timetable.entries.length === 0 && draftMap.size === 0 ? (
              <div className="rounded-lg border border-brand/30 bg-brand-soft/40 px-4 py-3 text-sm text-brand-strong dark:text-sky-300">
                Hiện thời khóa biểu đang trống. Con hãy chạm vào từng ô môn học bên dưới để chọn lịch học tuần này nhé.
              </div>
            ) : null}

            {/* Desktop 5-subject x 7-day grid */}
            <Card className="hidden overflow-hidden md:block">
              <CardContent className="p-0">
                <div className="overflow-x-auto">
                  <table className="w-full border-collapse text-left text-sm">
                    <thead>
                      <tr className="border-b bg-muted/40">
                        <th className="w-44 px-4 py-3.5 font-semibold text-foreground">
                          Môn học lớp 6
                        </th>
                        {WEEKDAYS.map((day) => (
                          <th
                            key={day.value}
                            className="min-w-[112px] px-2.5 py-3.5 text-center font-semibold text-foreground"
                          >
                            {day.label}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y">
                      {GRADE_6_SUBJECTS.map((subject) => (
                        <tr key={subject.code}>
                          <td className="px-4 py-3 font-semibold text-foreground">
                            {subject.name}
                          </td>
                          {WEEKDAYS.map((day) => {
                            const entry = draftMap.get(
                              makeCellKey(day.value, subject.code)
                            );
                            const selected = Boolean(entry);
                            return (
                              <td
                                key={day.value}
                                className="p-2 align-top"
                              >
                                <div className="space-y-1.5">
                                  <button
                                    type="button"
                                    onClick={() =>
                                      toggleSubjectCell(day.value, subject.code)
                                    }
                                    aria-pressed={selected}
                                    className={cn(
                                      'flex h-11 w-full items-center justify-center gap-1.5 rounded-lg border text-xs font-semibold transition-colors',
                                      selected
                                        ? 'border-brand-strong bg-brand-soft text-brand-strong dark:border-sky-500 dark:text-sky-200'
                                        : 'border-dashed border-border bg-background text-muted-foreground hover:border-brand/50 hover:bg-brand-soft/30'
                                    )}
                                  >
                                    {selected ? (
                                      <>
                                        <Check className="h-4 w-4" strokeWidth={2} />
                                        <span>Có học</span>
                                      </>
                                    ) : (
                                      <span>+ Chọn</span>
                                    )}
                                  </button>

                                  {selected && entry ? (
                                    <Input
                                      value={entry.periodNote ?? ''}
                                      onChange={(e) =>
                                        updatePeriodNote(
                                          day.value,
                                          subject.code,
                                          e.target.value
                                        )
                                      }
                                      maxLength={30}
                                      placeholder="Tiết..."
                                      aria-label={`Ghi chú tiết ${subject.name} ${day.label}`}
                                      className="h-8 text-center text-xs"
                                    />
                                  ) : null}
                                </div>
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>

            {/* Mobile stacked day cards */}
            <div className="grid grid-cols-1 gap-4 md:hidden">
              {WEEKDAYS.map((day) => (
                <Card key={day.value}>
                  <CardHeader className="pb-3">
                    <CardTitle className="text-base">{day.label}</CardTitle>
                  </CardHeader>
                  <CardContent className="space-y-2.5">
                    {GRADE_6_SUBJECTS.map((subject) => {
                      const entry = draftMap.get(
                        makeCellKey(day.value, subject.code)
                      );
                      const selected = Boolean(entry);
                      return (
                        <div key={subject.code} className="space-y-1.5">
                          <button
                            type="button"
                            onClick={() =>
                              toggleSubjectCell(day.value, subject.code)
                            }
                            aria-pressed={selected}
                            className={cn(
                              'flex h-11 w-full items-center justify-between rounded-lg border px-3.5 text-sm font-semibold transition-colors',
                              selected
                                ? 'border-brand-strong bg-brand-soft text-brand-strong dark:border-sky-500 dark:text-sky-200'
                                : 'bg-background text-foreground hover:bg-muted'
                            )}
                          >
                            <span>{subject.name}</span>
                            {selected ? (
                              <Check className="h-4 w-4" strokeWidth={2} />
                            ) : (
                              <span className="text-xs font-normal text-muted-foreground">
                                Chạm để thêm
                              </span>
                            )}
                          </button>

                          {selected && entry ? (
                            <Input
                              value={entry.periodNote ?? ''}
                              onChange={(e) =>
                                updatePeriodNote(
                                  day.value,
                                  subject.code,
                                  e.target.value
                                )
                              }
                              maxLength={30}
                              placeholder="Ghi chú tiết học (VD: Tiết 1-2)"
                              className="h-10 text-sm"
                            />
                          ) : null}
                        </div>
                      );
                    })}
                  </CardContent>
                </Card>
              ))}
            </div>

            <p className="text-right text-xs text-muted-foreground">
              Cập nhật gần nhất: {formatViDate(timetable.effectiveDate)} · Bởi{' '}
              {timetable.updatedByName}
            </p>
          </>
        )}
      </QueryStateBoundary>
    </div>
  );
}
