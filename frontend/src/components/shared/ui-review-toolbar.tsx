'use client';

import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  ChevronDown,
  ChevronUp,
  FlaskConical,
  RotateCcw,
  Shield,
} from 'lucide-react';
import { toast } from 'sonner';
import { IS_MOCK_MODE } from '@/services/config';
import {
  resetMockRuntime,
  setMockScenario,
  setSimulatedRole,
  useMockRuntime,
  type MockScenario,
} from '@/mocks/mock-runtime';
import type { UserRole } from '@/types';
import { cn } from '@/lib/utils';

const SCENARIO_OPTIONS: { value: MockScenario; label: string }[] = [
  { value: 'normal', label: 'Normal (Có dữ liệu)' },
  { value: 'empty', label: 'Empty State (Không có lịch)' },
  { value: 'slow', label: 'Slow Network (1.2s)' },
  { value: 'error_400', label: '400 VAL_001' },
  { value: 'error_403', label: '403 GRD_001' },
  { value: 'error_404', label: '404 STUDY_001' },
  { value: 'error_500', label: '500 Server Error' },
  { value: 'offline', label: 'Offline (Status 0)' },
];

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: 'STUDENT', label: 'Student' },
  { value: 'GUARDIAN', label: 'Guardian' },
];

/**
 * Dev-only floating review harness rendered ONLY when NEXT_PUBLIC_DATA_MODE !== 'api'.
 * Lets reviewers inspect Empty, Loading, Error, and Role states live across every screen.
 */
export function UiReviewToolbar() {
  const [expanded, setExpanded] = useState(false);
  const queryClient = useQueryClient();
  const runtime = useMockRuntime();

  if (!IS_MOCK_MODE) {
    return null;
  }

  const refreshQueries = () => {
    void queryClient.invalidateQueries();
  };

  const handleScenarioChange = (scenario: MockScenario) => {
    setMockScenario(scenario);
    refreshQueries();
    toast.info(`Mock Scenario: ${scenario}`);
  };

  const handleRoleChange = (role: UserRole) => {
    setSimulatedRole(role);
    refreshQueries();
    toast.info(`Đã chuyển vai trò xem trước: ${role}`);
  };

  const handleReset = () => {
    resetMockRuntime();
    refreshQueries();
    toast.success('Đã khôi phục dữ liệu mẫu ban đầu');
  };

  return (
    <div className="fixed bottom-4 left-4 z-50 max-w-xs">
      {!expanded ? (
        <button
          type="button"
          onClick={() => setExpanded(true)}
          className="inline-flex h-9 items-center gap-2 rounded-full border bg-card px-3.5 text-xs font-medium text-foreground shadow-sm transition-colors hover:bg-muted"
          aria-expanded={false}
          aria-label="Mở bảng mô phỏng trạng thái UI"
        >
          <FlaskConical className="h-3.5 w-3.5 text-brand" strokeWidth={1.75} />
          <span>UI Mock</span>
          <span className="rounded bg-muted px-1.5 py-0.5 font-mono text-[11px] text-muted-foreground">
            {runtime.simulatedRole}
          </span>
          {runtime.scenario !== 'normal' ? (
            <span className="rounded bg-amber-100 px-1.5 py-0.5 font-mono text-[11px] text-amber-800 dark:bg-amber-950 dark:text-amber-300">
              {runtime.scenario}
            </span>
          ) : null}
          <ChevronUp className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.75} />
        </button>
      ) : (
        <div className="w-72 rounded-lg border bg-card p-3.5 text-xs shadow-lg">
          <div className="mb-3 flex items-center justify-between border-b pb-2">
            <div className="flex items-center gap-1.5 font-semibold text-foreground">
              <FlaskConical className="h-4 w-4 text-brand" strokeWidth={1.75} />
              <span>UI Review Simulator</span>
            </div>
            <button
              type="button"
              onClick={() => setExpanded(false)}
              className="rounded p-1 text-muted-foreground hover:bg-muted hover:text-foreground"
              aria-label="Thu gọn bảng mô phỏng"
            >
              <ChevronDown className="h-4 w-4" strokeWidth={1.75} />
            </button>
          </div>

          <div className="space-y-3">
            <div>
              <label
                htmlFor="mock-scenario-select"
                className="mb-1 block font-medium text-muted-foreground"
              >
                Kịch bản dữ liệu &amp; lỗi
              </label>
              <select
                id="mock-scenario-select"
                value={runtime.scenario}
                onChange={(e) =>
                  handleScenarioChange(e.target.value as MockScenario)
                }
                className="h-8 w-full rounded-md border bg-background px-2 text-xs text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
              >
                {SCENARIO_OPTIONS.map((opt) => (
                  <option key={opt.value} value={opt.value}>
                    {opt.label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <span className="mb-1 flex items-center gap-1 font-medium text-muted-foreground">
                <Shield className="h-3.5 w-3.5" strokeWidth={1.75} />
                Vai trò đang xem
              </span>
              <div className="grid grid-cols-2 gap-1.5">
                {ROLE_OPTIONS.map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => handleRoleChange(opt.value)}
                    className={cn(
                      'h-7 rounded border text-[11px] font-medium transition-colors',
                      runtime.simulatedRole === opt.value
                        ? 'border-brand bg-brand text-white'
                        : 'bg-background text-foreground hover:bg-muted'
                    )}
                  >
                    {opt.label}
                  </button>
                ))}
              </div>
            </div>

            <div className="flex items-center justify-between border-t pt-2.5">
              <span className="font-mono text-[11px] text-muted-foreground">
                mode: mock
              </span>
              <button
                type="button"
                onClick={handleReset}
                className="inline-flex h-7 items-center gap-1 rounded border px-2 text-[11px] font-medium text-foreground hover:bg-muted"
              >
                <RotateCcw className="h-3 w-3" strokeWidth={1.75} />
                Reset DB
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
