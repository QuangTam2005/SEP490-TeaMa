'use client';

import {
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Eye,
  Flame,
  Target,
} from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import type { GuardianQuizStatsResponse } from '@/types';

interface GuardianQuizStatsPanelProps {
  stats: GuardianQuizStatsResponse;
  daysWindow: number;
  onChangeDaysWindow: (days: number) => void;
  onInspectSession: (sessionId: number) => void;
}

const DAY_WINDOWS = [7, 14, 30, 90] as const;

export function GuardianQuizStatsPanel({
  stats,
  daysWindow,
  onChangeDaysWindow,
  onInspectSession,
}: GuardianQuizStatsPanelProps) {
  const chartItems = [...stats.dailyStats].reverse().slice(-14);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <BarChart3
            className="h-5 w-5 text-brand-strong dark:text-sky-400"
            strokeWidth={1.75}
          />
          <h2 className="text-lg font-bold text-foreground">
            Thống kê kết quả · {stats.studentName}
          </h2>
        </div>

        <div className="flex items-center gap-1.5">
          {DAY_WINDOWS.map((d) => (
            <Button
              key={d}
              type="button"
              size="sm"
              variant={daysWindow === d ? 'default' : 'outline'}
              onClick={() => onChangeDaysWindow(d)}
              className="h-9 text-xs"
            >
              {d} ngày
            </Button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Tỷ lệ đúng trung bình</span>
              <Target className="h-4 w-4 text-brand-strong" strokeWidth={1.75} />
            </div>
            <p className="mt-2 font-mono text-2xl font-bold text-brand-strong dark:text-sky-300">
              {stats.averageAccuracyPercent}%
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              {stats.totalCorrectAnswers}/{stats.totalQuestionsAnswered} câu đúng
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Số phiên hoàn thành</span>
              <CheckCircle2 className="h-4 w-4 text-emerald-600" strokeWidth={1.75} />
            </div>
            <p className="mt-2 font-mono text-2xl font-bold text-foreground">
              {stats.totalCompletedSessions}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Trong {daysWindow} ngày gần đây
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Chuỗi ngày làm quiz</span>
              <Flame className="h-4 w-4 text-amber-500" strokeWidth={1.75} />
            </div>
            <p className="mt-2 font-mono text-2xl font-bold text-foreground">
              {stats.quizStreakDays} ngày
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Hôm nay:{' '}
              {stats.todayStatus === 'COMPLETED'
                ? 'Đã xong'
                : stats.todayStatus === 'IN_PROGRESS'
                  ? 'Đang làm'
                  : 'Chưa làm'}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>Quy mô ngân hàng</span>
              <BarChart3 className="h-4 w-4 text-muted-foreground" strokeWidth={1.75} />
            </div>
            <p className="mt-2 font-mono text-2xl font-bold text-foreground">
              {stats.activeQuestionCount}
            </p>
            <p className="mt-1 text-xs text-muted-foreground">
              Câu đang bật / {stats.totalQuestionCount} tổng
            </p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="border-b bg-brand-soft/20 dark:bg-sky-950/20">
          <CardTitle className="text-base font-bold">
            Biểu đồ tỷ lệ chính xác theo ngày (tối đa 14 phiên gần nhất)
          </CardTitle>
        </CardHeader>
        <CardContent className="p-5">
          {chartItems.length === 0 ? (
            <p className="py-8 text-center text-sm text-muted-foreground">
              Chưa có dữ liệu phiên làm bài trong {daysWindow} ngày vừa qua.
            </p>
          ) : (
            <div className="flex items-end gap-3 overflow-x-auto pb-2 pt-4">
              {chartItems.map((item) => {
                const barColor =
                  item.accuracyPercent >= 80
                    ? 'bg-emerald-500 dark:bg-emerald-400'
                    : item.accuracyPercent >= 50
                      ? 'bg-sky-600 dark:bg-sky-400'
                      : 'bg-amber-500 dark:bg-amber-400';

                return (
                  <button
                    key={item.sessionId}
                    type="button"
                    onClick={() => onInspectSession(item.sessionId)}
                    className="group flex min-w-[56px] flex-1 flex-col items-center gap-1.5 rounded-lg p-1.5 transition-colors hover:bg-muted/50"
                    title={`${formatViDate(item.date)}: ${item.correctCount}/${item.answeredCount} (${item.accuracyPercent}%)`}
                  >
                    <span className="font-mono text-xs font-bold text-foreground">
                      {item.accuracyPercent}%
                    </span>
                    <div className="flex h-36 w-8 items-end overflow-hidden rounded-md bg-muted p-0.5 sm:w-10">
                      <div
                        className={cn('w-full rounded-sm transition-all', barColor)}
                        style={{
                          height: `${Math.max(8, item.accuracyPercent)}%`,
                        }}
                      />
                    </div>
                    <span className="font-mono text-[11px] text-muted-foreground">
                      {item.date.slice(5).replace('-', '/')}
                    </span>
                  </button>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-12">
        <Card className="xl:col-span-7">
          <CardHeader className="border-b">
            <CardTitle className="text-base font-bold">
              Lịch sử làm bài theo ngày
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {stats.dailyStats.length === 0 ? (
              <p className="p-6 text-center text-sm text-muted-foreground">
                Con chưa hoàn thành phiên trắc nghiệm nào trong giai đoạn này.
              </p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="border-b bg-muted/40 text-xs uppercase text-muted-foreground">
                    <tr>
                      <th className="px-4 py-3">Ngày</th>
                      <th className="px-4 py-3">Lô câu</th>
                      <th className="px-4 py-3">Kết quả</th>
                      <th className="px-4 py-3">Trạng thái</th>
                      <th className="px-4 py-3 text-right">Chi tiết</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {stats.dailyStats.map((row) => (
                      <tr key={row.sessionId} className="hover:bg-muted/30">
                        <td className="whitespace-nowrap px-4 py-3 font-mono font-medium">
                          {formatViDate(row.date)}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 font-mono text-xs text-muted-foreground">
                          {row.cycleLabel}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 font-mono font-semibold">
                          {row.correctCount}/{row.totalQuestions} ({row.accuracyPercent}%)
                        </td>
                        <td className="whitespace-nowrap px-4 py-3">
                          <Badge
                            variant={row.status === 'COMPLETED' ? 'success' : 'info'}
                          >
                            {row.status === 'COMPLETED' ? 'Hoàn thành' : 'Đang làm'}
                          </Badge>
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 text-right">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => onInspectSession(row.sessionId)}
                            className="h-8 gap-1.5 px-2.5 text-xs"
                          >
                            <Eye className="h-3.5 w-3.5" strokeWidth={1.75} />
                            <span>Xem câu</span>
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="xl:col-span-5">
          <CardHeader className="border-b">
            <div className="flex items-center gap-2">
              <AlertTriangle
                className="h-4 w-4 text-amber-600 dark:text-amber-400"
                strokeWidth={1.75}
              />
              <CardTitle className="text-base font-bold">
                Câu con hay trả lời sai nhất
              </CardTitle>
            </div>
          </CardHeader>
          <CardContent className="p-4">
            {stats.hardestQuestions.length === 0 ? (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Chưa có câu trả lời sai nào được ghi nhận.
              </p>
            ) : (
              <div className="space-y-3">
                {stats.hardestQuestions.map((hq, idx) => (
                  <div
                    key={`${hq.questionId ?? 'x'}-${idx}`}
                    className="space-y-1.5 rounded-xl border p-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      {hq.topicLabel ? (
                        <Badge variant="secondary">{hq.topicLabel}</Badge>
                      ) : (
                        <span className="text-xs font-medium text-muted-foreground">
                          Câu #{idx + 1}
                        </span>
                      )}
                      <Badge variant="danger" className="font-mono">
                        Sai {hq.wrongCount}/{hq.attemptCount} lần ({hq.accuracyPercent}% đúng)
                      </Badge>
                    </div>
                    <p className="break-words text-sm font-medium text-foreground [overflow-wrap:anywhere]">
                      {hq.questionText}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
