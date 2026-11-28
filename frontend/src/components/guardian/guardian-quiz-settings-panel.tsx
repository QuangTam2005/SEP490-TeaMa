'use client';

import { useEffect } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { z } from 'zod';
import { Loader2, RotateCcw, Save, Settings2 } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { WEEKDAYS } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import type {
  GuardianQuizSettingsResponse,
  UpdateQuizSettingsPayload,
} from '@/types';

const settingsSchema = z.object({
  enabled: z.boolean(),
  questionsPerDay: z.coerce
    .number()
    .int('Phải là số nguyên')
    .min(1, 'Tối thiểu 1 câu/ngày')
    .max(50, 'Tối đa 50 câu/ngày'),
  activeWeekdays: z
    .array(z.number().int().min(1).max(7))
    .min(1, 'Chọn ít nhất 1 ngày trong tuần'),
  selectionMode: z.enum(['SEQUENTIAL_ROTATION', 'RANDOM']),
  shuffleOptions: z.boolean(),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

interface GuardianQuizSettingsPanelProps {
  settings: GuardianQuizSettingsResponse;
  isSaving: boolean;
  onSave: (payload: UpdateQuizSettingsPayload) => void;
}

export function GuardianQuizSettingsPanel({
  settings,
  isSaving,
  onSave,
}: GuardianQuizSettingsPanelProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    watch,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      enabled: settings.enabled,
      questionsPerDay: settings.questionsPerDay,
      activeWeekdays: settings.activeWeekdays,
      selectionMode: settings.selectionMode,
      shuffleOptions: settings.shuffleOptions,
    },
  });

  useEffect(() => {
    reset({
      enabled: settings.enabled,
      questionsPerDay: settings.questionsPerDay,
      activeWeekdays: settings.activeWeekdays,
      selectionMode: settings.selectionMode,
      shuffleOptions: settings.shuffleOptions,
    });
  }, [settings, reset]);

  const enabled = watch('enabled');
  const selectionMode = watch('selectionMode');
  const activeWeekdays = watch('activeWeekdays');

  const toggleWeekday = (day: number) => {
    const exists = activeWeekdays.includes(day);
    const next = exists
      ? activeWeekdays.filter((d) => d !== day)
      : [...activeWeekdays, day].sort((a, b) => a - b);
    setValue('activeWeekdays', next, { shouldValidate: true });
  };

  const onSubmit = (values: SettingsFormValues) => {
    onSave({
      ...values,
      resetCursor: false,
    });
  };

  const handleResetCursor = () => {
    const current = getValues();
    const parsedPerDay = Number(current.questionsPerDay);
    onSave({
      enabled: current.enabled,
      questionsPerDay:
        Number.isFinite(parsedPerDay) && parsedPerDay >= 1 && parsedPerDay <= 50
          ? Math.trunc(parsedPerDay)
          : settings.questionsPerDay,
      activeWeekdays:
        current.activeWeekdays.length > 0
          ? current.activeWeekdays
          : settings.activeWeekdays,
      selectionMode: current.selectionMode,
      shuffleOptions: current.shuffleOptions,
      resetCursor: true,
    });
  };

  return (
    <Card>
      <CardHeader className="border-b bg-brand-soft/30 dark:bg-sky-950/25">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Settings2
              className="h-4 w-4 text-brand-strong dark:text-sky-400"
              strokeWidth={1.75}
            />
            <CardTitle className="text-base font-bold">
              Cài đặt hiển thị câu hỏi đầu ngày
            </CardTitle>
          </div>
          <Badge variant={settings.enabled ? 'success' : 'secondary'}>
            {settings.enabled ? 'Đang bật' : 'Đang tắt'}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="p-5">
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
            <div className="flex items-center justify-between rounded-xl border bg-background p-3.5 lg:col-span-5">
              <div>
                <p className="text-sm font-semibold text-foreground">
                  Yêu cầu làm quiz khi vào app
                </p>
                <p className="text-xs text-muted-foreground">
                  Khóa tạm bài học cho tới khi con trả lời xong
                </p>
              </div>
              <Button
                type="button"
                size="sm"
                variant={enabled ? 'default' : 'outline'}
                onClick={() => setValue('enabled', !enabled)}
                className="h-9 min-w-[78px]"
              >
                {enabled ? 'Đang bật' : 'Đã tắt'}
              </Button>
            </div>

            <div className="space-y-1.5 lg:col-span-3">
              <Label htmlFor="quiz-questions-per-day">Số câu mỗi ngày</Label>
              <Input
                id="quiz-questions-per-day"
                type="number"
                min={1}
                max={50}
                {...register('questionsPerDay')}
              />
              {errors.questionsPerDay ? (
                <p className="text-xs text-rose-600 dark:text-rose-400">
                  {errors.questionsPerDay.message}
                </p>
              ) : null}
            </div>

            <div className="space-y-1.5 lg:col-span-4">
              <Label>Chế độ chọn câu hỏi</Label>
              <Controller
                control={control}
                name="selectionMode"
                render={({ field }) => (
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      type="button"
                      onClick={() => field.onChange('SEQUENTIAL_ROTATION')}
                      className={cn(
                        'h-10 rounded-lg border px-2.5 text-xs font-semibold transition-colors',
                        field.value === 'SEQUENTIAL_ROTATION'
                          ? 'border-brand-strong bg-brand-soft text-brand-strong dark:text-sky-200'
                          : 'bg-background text-muted-foreground hover:bg-muted'
                      )}
                    >
                      Xoay vòng (1→N)
                    </button>
                    <button
                      type="button"
                      onClick={() => field.onChange('RANDOM')}
                      className={cn(
                        'h-10 rounded-lg border px-2.5 text-xs font-semibold transition-colors',
                        field.value === 'RANDOM'
                          ? 'border-brand-strong bg-brand-soft text-brand-strong dark:text-sky-200'
                          : 'bg-background text-muted-foreground hover:bg-muted'
                      )}
                    >
                      Ngẫu nhiên
                    </button>
                  </div>
                )}
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label>Các ngày áp dụng trong tuần</Label>
            <div className="flex flex-wrap gap-2">
              {WEEKDAYS.map((day) => {
                const selected = activeWeekdays.includes(day.value);
                return (
                  <button
                    key={day.value}
                    type="button"
                    onClick={() => toggleWeekday(day.value)}
                    className={cn(
                      'h-10 min-w-[54px] rounded-lg border px-3 text-sm font-semibold transition-colors',
                      selected
                        ? 'border-brand-strong bg-brand-strong text-white dark:bg-sky-600'
                        : 'bg-background text-muted-foreground hover:bg-muted'
                    )}
                  >
                    {day.shortLabel}
                  </button>
                );
              })}
            </div>
            {errors.activeWeekdays ? (
              <p className="text-xs text-rose-600 dark:text-rose-400">
                {errors.activeWeekdays.message}
              </p>
            ) : null}
          </div>

          <div className="flex flex-col gap-3 rounded-xl border bg-muted/30 p-3.5 sm:flex-row sm:items-center sm:justify-between">
            <label className="flex cursor-pointer items-center gap-2.5 text-sm font-medium text-foreground">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-border accent-sky-600"
                {...register('shuffleOptions')}
              />
              <span>Xáo trộn vị trí 2 đáp án A / B mỗi lượt làm</span>
            </label>

            <div className="flex flex-wrap items-center gap-2">
              <Badge variant="info" className="font-mono">
                Lượt tới: {settings.nextBatchLabel}
              </Badge>
              {selectionMode === 'SEQUENTIAL_ROTATION' &&
              settings.rotationCursor > 0 ? (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={isSaving}
                  onClick={handleResetCursor}
                  className="h-9 gap-1.5 text-xs"
                >
                  <RotateCcw className="h-3.5 w-3.5" strokeWidth={1.75} />
                  <span>Đặt lại từ câu 1</span>
                </Button>
              ) : null}
            </div>
          </div>

          <div className="flex justify-end">
            <Button
              type="submit"
              disabled={isSaving}
              className="h-11 gap-2 sm:h-10"
            >
              {isSaving ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" strokeWidth={1.75} />
              )}
              <span>Lưu cài đặt</span>
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
