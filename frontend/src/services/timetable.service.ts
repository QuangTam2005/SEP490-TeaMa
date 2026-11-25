import type {
  SubjectResponse,
  TimetableResponse,
  UpdateTimetableRequest,
} from '@/types';

export interface TimetableService {
  getSubjects(grade?: number): Promise<SubjectResponse[]>;
  getTimetable(studentId?: number): Promise<TimetableResponse>;
  updateTimetable(
    request: UpdateTimetableRequest,
    studentId?: number
  ): Promise<TimetableResponse>;
}
