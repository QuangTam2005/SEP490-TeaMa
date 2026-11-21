export type WorkCategory =
  | 'REQUIREMENT'
  | 'DESIGN'
  | 'CODING'
  | 'TESTING'
  | 'PM';

export type ComplexityLevel = 'SIMPLE' | 'MEDIUM' | 'COMPLEX';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type IterationCode = 'ITER_1' | 'ITER_2' | 'ITER_3' | 'ITER_4';
export type RaciRole = 'D' | 'R' | 'S' | 'I';
export type RiskLevel = 'HIGH' | 'MEDIUM' | 'LOW';
export type TestingStage =
  | 'REVIEWING'
  | 'UNIT_TEST'
  | 'INTEGRATION_TEST'
  | 'SYSTEM_TEST'
  | 'ACCEPTANCE_TEST';

export interface GitHubPrChip {
  repoId: number;
  repoLabel: string;
  prNumber: number;
  prUrl: string;
  status: 'OPEN' | 'MERGED' | 'CLOSED';
  author: string;
  updatedAt: string;
}

export interface FeatureResponse {
  id: number;
  projectId: number;
  code: string;
  name: string;
  description: string | null;
}

export interface WbsTaskResponse {
  id: number;
  projectId: number;
  featureId: number | null;
  featureCode: string | null;
  useCaseId: number | null;
  useCaseCode: string | null;
  screenFunctionId: number | null;
  screenCode: string | null;
  code: string;
  title: string;
  workCategory: WorkCategory;
  iteration: IterationCode;
  fieldCount: number;
  transactionCount: number;
  complexity: ComplexityLevel;
  estMandays: number;
  actualMandays: number;
  assigneeId: number | null;
  assigneeName: string | null;
  status: TaskStatus;
  githubPrs: GitHubPrChip[];
}

export interface EffortDistributionItem {
  workCategory: WorkCategory;
  estMandays: number;
  actualMandays: number;
  percentage: number;
}

export interface RaciAssignmentResponse {
  id: number;
  deliverableName: string;
  assignments: Record<number, RaciRole | null>;
}

export interface ProjectRiskResponse {
  id: number;
  projectId: number;
  riskDescription: string;
  impact: RiskLevel;
  possibility: RiskLevel;
  responsePlan: string;
}

export interface QualityObjectiveResponse {
  id: number;
  projectId: number;
  testingStage: TestingStage;
  targetCoveragePercent: number;
  targetDefectRatePercent: number;
}
