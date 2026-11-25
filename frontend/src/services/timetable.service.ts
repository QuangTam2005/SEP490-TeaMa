import type {
  SubjectResponse,
  TimetableResponse,
  UpdateTimetableRequest,
} from '@/types';

export interface TimetableService {
  getSubjects(grade?: number): Promise<SubjectResponse[]>;
  getTimetable(): Promise<TimetableResponse>;
  updateTimetable(request: UpdateTimetableRequest): Promise<TimetableResponse>;
}
