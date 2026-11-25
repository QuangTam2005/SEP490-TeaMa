export type AccuracyLevel =
  | 'CORRECT'
  | 'PARTIALLY_CORRECT'
  | 'INCORRECT'
  | 'UNVERIFIABLE';

export type CompletenessLevel =
  | 'COMPLETE'
  | 'INCOMPLETE'
  | 'UNDETERMINED';

export type OverallEvaluationStatus =
  | 'GOOD'
  | 'NEEDS_IMPROVEMENT'
  | 'INCORRECT'
  | 'INSUFFICIENT_EVIDENCE';

export type ConfidenceLevel = 'HIGH' | 'MEDIUM' | 'LOW';

export interface SourceReferenceResponse {
  id: number;
  title: string;
  url: string;
  snippet: string | null;
  retrievedAt: string;
}

export interface AIEvaluationResponse {
  id: number;
  submissionId: number;
  accuracy: AccuracyLevel;
  completeness: CompletenessLevel;
  overallStatus: OverallEvaluationStatus;
  feedback: string;
  incorrectPoints: string[];
  missingPoints: string[];
  confidence: ConfidenceLevel;
  sources: SourceReferenceResponse[];
  evaluatedAt: string;
}
