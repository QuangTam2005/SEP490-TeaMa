import type { TestingStage } from './planning';

export type TestLevel = 'UT' | 'IT' | 'ST' | 'AT';
export type TestStatus = 'UNTESTED' | 'PASS' | 'FAIL';

export interface TestEvidenceAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  uploadedByName: string;
  uploadedAt: string;
}

export interface TestCaseResponse {
  id: number;
  projectId: number;
  useCaseId: number;
  useCaseCode: string;
  code: string;
  testLevel: TestLevel;
  description: string;
  expectedResult: string;
  actualResult: string | null;
  status: TestStatus;
  executedByName: string | null;
  executedAt: string | null;
  evidenceAttachments: TestEvidenceAttachment[];
}

export interface QualityStageMetric {
  testingStage: TestingStage;
  targetCoveragePercent: number;
  actualCoveragePercent: number;
  targetDefectRatePercent: number;
  actualDefectRatePercent: number;
  totalCases: number;
  passedCases: number;
  failedCases: number;
}
