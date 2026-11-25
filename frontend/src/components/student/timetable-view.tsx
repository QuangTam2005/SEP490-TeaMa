'use client';

import { useEffect, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  CalendarDays,
  Check,
  History,
  Loader2,
  Save,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import {
  ALL_SUBJECTS,
  GRADE_OPTIONS,
  WEEKDAYS,
  formatViDate,
  getSubjectsForGrade,
} from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import { useMockRuntime } from '@/mocks/mock-runtime';
import {
  IS_MOCK_MODE,
  authService,
  linkService,
  timetableService,
} from '@/services';
import { useAuthStore } from '@/store/auth-store';
import { useGuardianStore } from '@/store/guardian-store';
import type { SubjectCode, TimetableEntryDto, Weekday } from '@/types';

function makeCellKey(weekday: Weekday, subjectCode: SubjectCode): string {
  return `${weekday}:${subjectCode}`;
}

export function TimetableView() {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const user = useAuthStore((state) => state.user);
  const syncUser = useAuthStore((state) => state.syncUser);
  const selectedStudentId = useGuardianStore((state) => state.selectedStudentId);
  const setSelectedStudentId = useGuardianStore(
    (state) => state.setSelectedStudentId
  );
  const [draftMap, setDraftMap] = useState<Map<string, TimetableEntryDto>>(
    () => new Map()
  );
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

  const isStudentMode = IS_MOCK_MODE
    ? runtime.simulatedRole === 'STUDENT'
    : user?.role === 'STUDENT';

  const childrenQuery = useQuery({
    queryKey: [
      'linked-children',
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () => linkService.getLinkedChildren(),
    enabled: !isStudentMode,
  });

  useEffect(() => {
    if (isStudentMode || !childrenQuery.data?.length) {
      return;
    }
    const stillValid = childrenQuery.data.some(
      (c) => c.studentId === selectedStudentId
    );
    if (!selectedStudentId || !stillValid) {
      setSelectedStudentId(childrenQuery.data[0].studentId);
    }
  }, [
    isStudentMode,
    childrenQuery.data,
    selectedStudentId,
    setSelectedStudentId,
  ]);

  const activeChild = childrenQuery.data?.find(
    (c) => c.studentId === selectedStudentId
  );
  const activeGrade = isStudentMode
    ? (user?.grade ?? 6)
    : activeChild?.grade;
  const subjectsQuery = useQuery({
    queryKey: ['subjects', activeGrade ?? 'all'],
    queryFn: () => timetableService.getSubjects(activeGrade),
  });

  const baseSubjects =
    subjectsQuery.data && subjectsQuery.data.length > 0
      ? subjectsQuery.data.map((item) => ({
          code: item.code,
          name: item.name,
        }))
      : (activeGrade ? getSubjectsForGrade(activeGrade) : ALL_SUBJECTS).map((item) => ({
          code: item.code,
          name: item.name,
        }));

  const selectedExtraCodes = new Set<SubjectCode>();
  draftMap.forEach((entry) => selectedExtraCodes.add(entry.subjectCode));
  const displayedSubjects = [
    ...baseSubjects,
    ...ALL_SUBJECTS.filter(
      (s) =>
        selectedExtraCodes.has(s.code) &&
        !baseSubjects.some((b) => b.code === s.code)
    ).map((s) => ({ code: s.code, name: s.name })),
  ];

  const gradeMutation = useMutation({
    mutationFn: (grade: number) => authService.updateStudentProfile({ grade }),
    onSuccess: (updatedUser) => {
      syncUser(updatedUser);
      toast.success(`Đã cập nhật sang Khối ${updatedUser.grade}`);
    },
    onError: (err) => {
      toast.error(extractApiErrorMessage(err));
    },
  });

  const timetableQuery = useQuery({
    queryKey: [
      'student-timetable',
      isStudentMode ? 'self' : selectedStudentId,
      runtime.resetCounter,
      runtime.scenario,
      runtime.simulatedRole,
    ],
    queryFn: () =>
      timetableService.getTimetable(
        isStudentMode ? undefined : (selectedStudentId ?? undefined)
      ),
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
      timetableService.updateTimetable(
        { entries },
        isStudentMode ? undefined : (selectedStudentId ?? undefined)
      ),
    onSuccess: (saved) => {
      queryClient.setQueryData(
        [
          'student-timetable',
          isStudentMode ? 'self' : selectedStudentId,
          runtime.resetCounter,
          runtime.scenario,
          runtime.simulatedRole,
        ],
        saved
      );
      void queryClient.invalidateQueries({ queryKey: ['next-day-plan'] });
      setHasUnsavedChanges(false);
      toast.success('Đã lưu thời khóa biểu');
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
        next.set(key, { weekday, subjectCode, periodNote: null });
      }
      return next;
    });
    setHasUnsavedChanges(true);
  };

  const handleSave = () => {
    const entries = Array.from(draftMap.values())
      .map((entry) => ({
        weekday: entry.weekday,
        subjectCode: entry.subjectCode,
        periodNote: null,
      }))
      .sort((a, b) => a.weekday - b.weekday);
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
          </div>
          <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
            <History className="h-3.5 w-3.5 shrink-0" strokeWidth={1.75} />
            <span>
              Chỉnh lịch chỉ áp dụng cho ngày mới, lịch sử bài cũ luôn được giữ nguyên.
            </span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          {isStudentMode ? (
            <select
              aria-label="Chọn khối lớp của con"
              value={user?.grade ?? 6}
              disabled={gradeMutation.isPending}
              onChange={(e) => gradeMutation.mutate(Number(e.target.value))}
              className="h-11 rounded-lg border bg-background px-3 text-sm font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {GRADE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          ) : childrenQuery.data && childrenQuery.data.length > 0 ? (
            <div className="flex items-center gap-1.5">
              <Users className="h-4 w-4 text-muted-foreground" strokeWidth={1.75} />
              <select
                aria-label="Chọn học sinh chỉnh thời khóa biểu"
                value={selectedStudentId ?? ''}
                onChange={(e) => {
                  const id = Number(e.target.value);
                  setSelectedStudentId(Number.isFinite(id) ? id : null);
                }}
                className="h-11 rounded-lg border bg-background px-3 text-sm font-semibold text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {childrenQuery.data.map((child) => (
                  <option key={child.studentId} value={child.studentId}>
                    {child.displayName} (Lớp {child.grade})
                  </option>
                ))}
              </select>
            </div>
          ) : null}

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
                          Môn học
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
                      {displayedSubjects.map((subject) => (
                        <tr key={subject.code}>
                          <td className="px-4 py-3 font-semibold text-foreground">
                            {subject.name}
                          </td>
                          {WEEKDAYS.map((day) => {
                            const selected = draftMap.has(
                              makeCellKey(day.value, subject.code)
                            );
                            return (
                              <td
                                key={day.value}
                                className="p-2 align-middle"
                              >
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
                    {displayedSubjects.map((subject) => {
                      const selected = draftMap.has(
                        makeCellKey(day.value, subject.code)
                      );
                      return (
                        <button
                          key={subject.code}
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
