import { GRADE_6_SUBJECTS } from '@/lib/study-utils';
import { buildInitialTimetable } from '@/mocks/fixtures/study-seed';
import { registerMockResetHook, runMockOperation } from '@/mocks/mock-runtime';
import type { TimetableService } from '@/services/timetable.service';
import type {
  SubjectResponse,
  TimetableResponse,
  UpdateTimetableRequest,
} from '@/types';

let timetableDb: TimetableResponse = buildInitialTimetable();

registerMockResetHook(() => {
  timetableDb = buildInitialTimetable();
});

export function getCurrentMockTimetable(): TimetableResponse {
  return timetableDb;
}

export const timetableMockService: TimetableService = {
  async getSubjects(): Promise<SubjectResponse[]> {
    return runMockOperation(
      () =>
        GRADE_6_SUBJECTS.map((item, index) => ({
          id: index + 1,
          code: item.code,
          name: item.name,
        })),
      { emptyFallback: [] }
    );
  },

  async getTimetable(): Promise<TimetableResponse> {
    return runMockOperation(
      () => ({
        ...timetableDb,
        entries: timetableDb.entries.map((entry) => ({ ...entry })),
      }),
      {
        requireRole: 'STUDENT',
        emptyFallback: {
          ...timetableDb,
          entries: [],
        },
      }
    );
  },

  async updateTimetable(
    request: UpdateTimetableRequest
  ): Promise<TimetableResponse> {
    return runMockOperation(
      () => {
        timetableDb = {
          ...timetableDb,
          version: timetableDb.version + 1,
          updatedAt: new Date().toISOString(),
          entries: request.entries.map((entry) => ({
            weekday: entry.weekday,
            subjectCode: entry.subjectCode,
            periodNote: entry.periodNote ? entry.periodNote.trim() : null,
          })),
        };
        return {
          ...timetableDb,
          entries: timetableDb.entries.map((entry) => ({ ...entry })),
        };
      },
      { requireRole: 'STUDENT' }
    );
  },
};
