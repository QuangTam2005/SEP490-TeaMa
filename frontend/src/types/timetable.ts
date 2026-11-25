export type SubjectCode =
  | 'MATH'
  | 'LIT'
  | 'ENG'
  | 'SCI'
  | 'HISGEO'
  | 'PHY'
  | 'CHEM'
  | 'BIO'
  | 'HIST'
  | 'GEO'
  | 'INFO'
  | 'TECH'
  | 'CIVICS';

/**
 * ISO weekday: 1 = Monday .. 7 = Sunday
 */
export type Weekday = 1 | 2 | 3 | 4 | 5 | 6 | 7;

export interface SubjectResponse {
  id: number;
  code: SubjectCode;
  name: string;
  minGrade: number;
  maxGrade: number;
}

export interface TimetableEntryDto {
  weekday: Weekday;
  subjectCode: SubjectCode;
  periodNote: string | null;
}

export interface TimetableResponse {
  id: number;
  version: number;
  effectiveDate: string;
  updatedAt: string;
  updatedByName: string;
  entries: TimetableEntryDto[];
}

export interface UpdateTimetableRequest {
  entries: TimetableEntryDto[];
}
