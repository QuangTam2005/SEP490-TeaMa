import { apiGet, apiPut } from '@/lib/api-client';
import type { TimetableService } from '@/services/timetable.service';
import type {
  SubjectResponse,
  TimetableResponse,
  UpdateTimetableRequest,
} from '@/types';

export const timetableHttpService: TimetableService = {
  getSubjects: (grade?: number) =>
    apiGet<SubjectResponse[]>(
      '/api/subjects',
      grade ? { params: { grade } } : undefined
    ),
  getTimetable: (studentId?: number) =>
    apiGet<TimetableResponse>(
      '/api/student/timetable',
      studentId ? { params: { studentId } } : undefined
    ),
  updateTimetable: (request: UpdateTimetableRequest, studentId?: number) =>
    apiPut<TimetableResponse>(
      '/api/student/timetable',
      request,
      studentId ? { params: { studentId } } : undefined
    ),
};
