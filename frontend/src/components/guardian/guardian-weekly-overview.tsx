'use client';

import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { formatViDate } from '@/lib/study-utils';
import { cn } from '@/lib/utils';
import type { GuardianWeeklySummaryResponse } from '@/types';

interface GuardianWeeklyOverviewProps {
  weekly: GuardianWeeklySummaryResponse;
  selectedDate: string;
  onSelectDate: (isoDate: string) => void;
  onShiftWeek: (deltaDays: number) => void;
}

export function GuardianWeeklyOverview({
  weekly,
  selectedDate,
  onSelectDate,
  onShiftWeek,
}: GuardianWeeklyOverviewProps) {
  return (
    <Card>
      <CardHeader className="flex flex-col gap-2 pb-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <CardTitle className="text-base">
            Tổng quan tuần ({formatViDate(weekly.weekStart)} –{' '}
            {formatViDate(weekly.weekEnd)})
          </CardTitle>
          <p className="text-xs text-muted-foreground">
            Hoàn thành {weekly.completedTasks}/{weekly.totalTasks} nhiệm vụ ·
            Phụ huynh đã xem {weekly.acknowledgedTasks} bài
          </p>
        </div>
        <div className="flex items-center gap-1.5">
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-11 w-11 sm:h-9 sm:w-9"
            aria-label="Tuần trước"
            onClick={() => onShiftWeek(-7)}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            type="button"
            variant="outline"
            size="icon"
            className="h-11 w-11 sm:h-9 sm:w-9"
            aria-label="Tuần sau"
            onClick={() => onShiftWeek(7)}
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </CardHeader>
      {weekly.days.length > 0 ? (
        <CardContent className="grid grid-cols-2 gap-2 sm:grid-cols-7">
          {weekly.days.map((day) => (
            <button
              key={day.date}
              type="button"
              onClick={() => onSelectDate(day.date)}
              className={cn(
                'flex flex-col items-center rounded-lg border p-2.5 text-center transition-colors',
                selectedDate === day.date
                  ? 'border-brand-strong bg-brand-soft/60 text-brand-strong dark:text-sky-200'
                  : 'bg-background hover:bg-muted'
              )}
            >
              <span className="text-xs font-semibold">{day.weekdayLabel}</span>
              <span className="text-[11px] text-muted-foreground">
                {day.date.slice(5).split('-').reverse().join('/')}
              </span>
              <span className="mt-1 font-mono text-xs font-bold">
                {day.completedTasks}/{day.totalTasks}
              </span>
            </button>
          ))}
        </CardContent>
      ) : null}
    </Card>
  );
}
