import type {
  SubjectResponse,
  TimetableResponse,
  UpdateTimetableRequest,
} from '@/types';

export interface TimetableService {
  getSubjects(): Promise<SubjectResponse[]>;
  getTimetable(): Promise<TimetableResponse>;
  updateTimetable(request: UpdateTimetableRequest): Promise<TimetableResponse>;
}
