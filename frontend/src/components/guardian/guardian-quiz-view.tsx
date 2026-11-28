'use client';

import { useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  BarChart3,
  Link2,
  ListChecks,
  Loader2,
  SlidersHorizontal,
  Users,
} from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { ErrorStateCard } from '@/components/shared/error-state-card';
import { QueryStateBoundary } from '@/components/shared/query-state-boundary';
import { extractApiErrorMessage } from '@/components/shared/form-error-utils';
import { GuardianQuizBankPanel } from '@/components/guardian/guardian-quiz-bank-panel';
import { GuardianQuizBulkDialog } from '@/components/guardian/guardian-quiz-bulk-dialog';
import { GuardianQuizQuestionDialog } from '@/components/guardian/guardian-quiz-question-dialog';
import { GuardianQuizSessionDialog } from '@/components/guardian/guardian-quiz-session-dialog';
import { GuardianQuizSettingsPanel } from '@/components/guardian/guardian-quiz-settings-panel';
import { GuardianQuizStatsPanel } from '@/components/guardian/guardian-quiz-stats-panel';
import { useMockRuntime } from '@/mocks/mock-runtime';
import { IS_MOCK_MODE, linkService, quizService } from '@/services';
import { useAuthStore } from '@/store/auth-store';
import { useGuardianStore } from '@/store/guardian-store';
import type {
  CreateQuizQuestionPayload,
  MoveDirection,
  QuizQuestionResponse,
  UpdateQuizSettingsPayload,
} from '@/types';

export function GuardianQuizView() {
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();
  const user = useAuthStore((state) => state.user);
  const isHydrated = useAuthStore((state) => state.isHydrated);
  const selectedStudentId = useGuardianStore((state) => state.selectedStudentId);
  const setSelectedStudentId = useGuardianStore(
    (state) => state.setSelectedStudentId
  );

  const isGuardianReady = IS_MOCK_MODE || (isHydrated && user?.role === 'GUARDIAN');

  const [activeTab, setActiveTab] = useState<'STATS' | 'BANK'>('STATS');
  const [daysWindow, setDaysWindow] = useState<number>(30);
  const [questionDialogOpen, setQuestionDialogOpen] = useState(false);
  const [editingQuestion, setEditingQuestion] =
    useState<QuizQuestionResponse | null>(null);
  const [bulkDialogOpen, setBulkDialogOpen] = useState(false);
  const [bulkResetCounter, setBulkResetCounter] = useState(0);
  const [inspectSessionId, setInspectSessionId] = useState<number | null>(null);

  const childrenQuery = useQuery({
    queryKey: ['linked-children', runtime.resetCounter, runtime.simulatedRole],
    queryFn: () => linkService.getLinkedChildren(),
    enabled: isGuardianReady,
  });

  const children = useMemo(() => childrenQuery.data ?? [], [childrenQuery.data]);
  const hasLinkedChildren = children.length > 0;
  const isSelectedValid =
    selectedStudentId !== null &&
    children.some((c) => c.studentId === selectedStudentId);
  const effectiveStudentId = isSelectedValid
    ? selectedStudentId
    : (children[0]?.studentId ?? null);

  useEffect(() => {
    if (children.length > 0 && !isSelectedValid) {
      setSelectedStudentId(children[0].studentId);
    }
  }, [children, isSelectedValid, setSelectedStudentId]);

  const settingsQuery = useQuery({
    queryKey: ['guardian-quiz-settings', effectiveStudentId, runtime.resetCounter],
    queryFn: () => quizService.getGuardianSettings(effectiveStudentId),
    enabled: isGuardianReady && hasLinkedChildren,
  });

  const questionsQuery = useQuery({
    queryKey: ['guardian-quiz-questions', effectiveStudentId, runtime.resetCounter],
    queryFn: () => quizService.listGuardianQuestions(effectiveStudentId),
    enabled: isGuardianReady && hasLinkedChildren,
  });

  const statsQuery = useQuery({
    queryKey: [
      'guardian-quiz-stats',
      effectiveStudentId,
      daysWindow,
      runtime.resetCounter,
    ],
    queryFn: () =>
      quizService.getGuardianStats({
        studentId: effectiveStudentId,
        days: daysWindow,
      }),
    enabled: isGuardianReady && hasLinkedChildren,
  });

  const invalidateQuizQueries = () => {
    void queryClient.invalidateQueries({ queryKey: ['guardian-quiz-settings'] });
    void queryClient.invalidateQueries({ queryKey: ['guardian-quiz-questions'] });
    void queryClient.invalidateQueries({ queryKey: ['guardian-quiz-stats'] });
    void queryClient.invalidateQueries({ queryKey: ['student-daily-quiz'] });
  };

  const updateSettingsMutation = useMutation({
    mutationFn: (payload: UpdateQuizSettingsPayload) =>
      quizService.updateGuardianSettings(payload, effectiveStudentId),
    onSuccess: () => {
      toast.success('Đã lưu cài đặt trắc nghiệm đầu ngày');
      invalidateQuizQueries();
    },
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  const saveQuestionMutation = useMutation({
    mutationFn: ({
      payload,
      questionId,
    }: {
      payload: CreateQuizQuestionPayload;
      questionId?: number;
    }) =>
      questionId
        ? quizService.updateGuardianQuestion(questionId, payload, effectiveStudentId)
        : quizService.createGuardianQuestion(payload, effectiveStudentId),
    onSuccess: (_data, variables) => {
      toast.success(
        variables.questionId ? 'Đã cập nhật câu hỏi' : 'Đã thêm câu hỏi mới'
      );
      setQuestionDialogOpen(false);
      setEditingQuestion(null);
      invalidateQuizQueries();
    },
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  const bulkCreateMutation = useMutation({
    mutationFn: (questions: CreateQuizQuestionPayload[]) =>
      quizService.bulkCreateGuardianQuestions({ questions }, effectiveStudentId),
    onSuccess: (created) => {
      toast.success(`Đã thêm ${created.length} câu hỏi vào ngân hàng`);
      setBulkDialogOpen(false);
      setBulkResetCounter((prev) => prev + 1);
      invalidateQuizQueries();
    },
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  const toggleActiveMutation = useMutation({
    mutationFn: (q: QuizQuestionResponse) =>
      quizService.updateGuardianQuestion(
        q.id,
        {
          questionText: q.questionText,
          optionA: q.optionA,
          optionB: q.optionB,
          correctOption: q.correctOption,
          explanation: q.explanation,
          topicLabel: q.topicLabel,
          active: !q.active,
        },
        effectiveStudentId
      ),
    onSuccess: () => invalidateQuizQueries(),
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  const moveMutation = useMutation({
    mutationFn: ({
      questionId,
      direction,
    }: {
      questionId: number;
      direction: MoveDirection;
    }) =>
      quizService.moveGuardianQuestion(questionId, direction, effectiveStudentId),
    onSuccess: () => invalidateQuizQueries(),
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (questionId: number) =>
      quizService.deleteGuardianQuestion(questionId, effectiveStudentId),
    onSuccess: () => {
      toast.success('Đã xóa câu hỏi');
      invalidateQuizQueries();
    },
    onError: (err) => toast.error(extractApiErrorMessage(err)),
  });

  if (!IS_MOCK_MODE && (!isHydrated || childrenQuery.isLoading)) {
    return (
      <Card>
        <CardContent className="flex items-center justify-center gap-2 py-12 text-sm text-muted-foreground">
          <Loader2 className="h-4 w-4 animate-spin text-brand-strong" />
          <span>Đang kiểm tra tài khoản con đã liên kết...</span>
        </CardContent>
      </Card>
    );
  }

  if (childrenQuery.isError) {
    return (
      <ErrorStateCard
        error={childrenQuery.error}
        title="Không thể tải danh sách con đã liên kết"
        onRetry={() => void childrenQuery.refetch()}
      />
    );
  }

  if (!hasLinkedChildren) {
    return (
      <Card>
        <CardContent className="flex flex-col items-center justify-center gap-4 py-12 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-full bg-brand-soft text-brand-strong">
            <Link2 className="h-6 w-6" strokeWidth={1.75} />
          </div>
          <div className="space-y-1">
            <h2 className="text-lg font-bold text-foreground">
              Chưa liên kết tài khoản học sinh
            </h2>
            <p className="text-sm text-muted-foreground">
              Phụ huynh cần nhập mã liên kết 8 ký tự từ con để tạo bộ câu hỏi đầu ngày.
            </p>
          </div>
          <Button asChild className="h-11 gap-2">
            <Link href="/link-student">
              <Link2 className="h-4 w-4" strokeWidth={1.75} />
              <span>Liên kết ngay</span>
            </Link>
          </Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <ListChecks
              className="h-5 w-5 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <h1 className="text-xl font-bold tracking-tight text-foreground sm:text-2xl">
              Trắc nghiệm đầu ngày
            </h1>
          </div>
          <p className="text-sm text-muted-foreground">
            Tạo ngân hàng câu hỏi 2 lựa chọn xoay vòng mỗi sáng và theo dõi tỷ lệ đúng của con.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {children.length > 1 ? (
            <div className="flex items-center gap-2 rounded-lg border bg-card px-3 py-1.5">
              <Users className="h-4 w-4 text-brand-strong" strokeWidth={1.75} />
              <select
                aria-label="Chọn học sinh"
                value={effectiveStudentId ?? ''}
                onChange={(e) => setSelectedStudentId(Number(e.target.value))}
                className="h-8 bg-transparent text-base font-medium text-foreground focus:outline-none md:text-sm"
              >
                {children.map((child) => (
                  <option key={child.studentId} value={child.studentId}>
                    {child.displayName} (Lớp {child.grade})
                  </option>
                ))}
              </select>
            </div>
          ) : null}

          <div className="flex items-center gap-1 rounded-lg border bg-card p-1">
            <Button
              type="button"
              size="sm"
              variant={activeTab === 'STATS' ? 'default' : 'ghost'}
              onClick={() => setActiveTab('STATS')}
              className="h-9 gap-1.5 text-xs sm:text-sm"
            >
              <BarChart3 className="h-4 w-4" strokeWidth={1.75} />
              <span>Thống kê</span>
            </Button>
            <Button
              type="button"
              size="sm"
              variant={activeTab === 'BANK' ? 'default' : 'ghost'}
              onClick={() => setActiveTab('BANK')}
              className="h-9 gap-1.5 text-xs sm:text-sm"
            >
              <SlidersHorizontal className="h-4 w-4" strokeWidth={1.75} />
              <span>Ngân hàng & Cài đặt</span>
            </Button>
          </div>
        </div>
      </div>

      {activeTab === 'STATS' ? (
        <QueryStateBoundary
          data={statsQuery.data}
          isLoading={statsQuery.isLoading}
          isError={statsQuery.isError}
          error={statsQuery.error}
          onRetry={() => void statsQuery.refetch()}
        >
          {(stats) => (
            <GuardianQuizStatsPanel
              stats={stats}
              daysWindow={daysWindow}
              onChangeDaysWindow={setDaysWindow}
              onInspectSession={setInspectSessionId}
            />
          )}
        </QueryStateBoundary>
      ) : (
        <div className="space-y-6">
          <QueryStateBoundary
            data={settingsQuery.data}
            isLoading={settingsQuery.isLoading}
            isError={settingsQuery.isError}
            error={settingsQuery.error}
            onRetry={() => void settingsQuery.refetch()}
          >
            {(settings) => (
              <GuardianQuizSettingsPanel
                settings={settings}
                isSaving={updateSettingsMutation.isPending}
                onSave={(payload) => updateSettingsMutation.mutate(payload)}
              />
            )}
          </QueryStateBoundary>

          <QueryStateBoundary
            data={questionsQuery.data}
            isLoading={questionsQuery.isLoading}
            isError={questionsQuery.isError}
            error={questionsQuery.error}
            onRetry={() => void questionsQuery.refetch()}
          >
            {(questions) => (
              <GuardianQuizBankPanel
                questions={questions}
                settings={
                  settingsQuery.data ?? {
                    studentId: effectiveStudentId ?? 0,
                    enabled: false,
                    questionsPerDay: 10,
                    activeWeekdays: [1, 2, 3, 4, 5, 6, 7],
                    selectionMode: 'SEQUENTIAL_ROTATION',
                    shuffleOptions: false,
                    rotationCursor: 0,
                    totalQuestionCount: questions.length,
                    activeQuestionCount: questions.filter((q) => q.active).length,
                    nextBatchLabel: '',
                  }
                }
                isBusy={
                  saveQuestionMutation.isPending ||
                  bulkCreateMutation.isPending ||
                  toggleActiveMutation.isPending ||
                  moveMutation.isPending ||
                  deleteMutation.isPending
                }
                onAddSingle={() => {
                  setEditingQuestion(null);
                  setQuestionDialogOpen(true);
                }}
                onAddBulk={() => setBulkDialogOpen(true)}
                onEdit={(q) => {
                  setEditingQuestion(q);
                  setQuestionDialogOpen(true);
                }}
                onToggleActive={(q) => toggleActiveMutation.mutate(q)}
                onMove={(questionId, direction) =>
                  moveMutation.mutate({ questionId, direction })
                }
                onDelete={(questionId) => deleteMutation.mutate(questionId)}
              />
            )}
          </QueryStateBoundary>
        </div>
      )}

      <GuardianQuizQuestionDialog
        open={questionDialogOpen}
        onOpenChange={setQuestionDialogOpen}
        editingQuestion={editingQuestion}
        isSubmitting={saveQuestionMutation.isPending}
        onSubmit={(payload, questionId) =>
          saveQuestionMutation.mutate({ payload, questionId })
        }
      />

      <GuardianQuizBulkDialog
        open={bulkDialogOpen}
        onOpenChange={setBulkDialogOpen}
        isSubmitting={bulkCreateMutation.isPending}
        resetSignal={bulkResetCounter}
        onSubmit={(questions) => bulkCreateMutation.mutate(questions)}
      />

      <GuardianQuizSessionDialog
        sessionId={inspectSessionId}
        studentId={effectiveStudentId}
        onClose={() => setInspectSessionId(null)}
      />
    </div>
  );
}
