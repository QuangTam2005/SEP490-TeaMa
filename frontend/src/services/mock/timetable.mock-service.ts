import { ALL_SUBJECTS } from '@/lib/study-utils';
import { buildInitialTimetable } from '@/mocks/fixtures/study-seed';
import {
  getMockRuntimeState,
  registerMockResetHook,
  runMockOperation,
} from '@/mocks/mock-runtime';
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
  async getSubjects(grade?: number): Promise<SubjectResponse[]> {
    return runMockOperation(
      () =>
        ALL_SUBJECTS.filter(
          (item) => !grade || (grade >= item.minGrade && grade <= item.maxGrade)
        ).map((item, index) => ({
          id: index + 1,
          code: item.code,
          name: item.name,
          minGrade: item.minGrade,
          maxGrade: item.maxGrade,
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
        requireRoles: ['STUDENT', 'GUARDIAN'],
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
        const { simulatedRole } = getMockRuntimeState();
        timetableDb = {
          ...timetableDb,
          version: timetableDb.version + 1,
          updatedAt: new Date().toISOString(),
          updatedByName:
            simulatedRole === 'GUARDIAN'
              ? 'Nguyễn Thu Hằng (Phụ huynh)'
              : 'Nguyễn Minh Khôi',
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
      { requireRoles: ['STUDENT', 'GUARDIAN'] }
    );
  },
};
