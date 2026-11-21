export type ScreenFunctionType = 'SCREEN' | 'NON_SCREEN';
export type AppMessageType = 'INLINE' | 'TOAST' | 'FIELD_ERROR';
export type DiagramType =
  | 'CONTEXT'
  | 'USE_CASE'
  | 'SCREEN_FLOW'
  | 'ERD'
  | 'ARCHITECTURE';

export interface ScreenAuthPermissions {
  queryAll: boolean;
  queryOwn: boolean;
  queryManaged: boolean;
  add: boolean;
  updateAll: boolean;
  updateOwn: boolean;
  updateManaged: boolean;
  delete: boolean;
}

export interface SystemActorResponse {
  id: number;
  projectId: number;
  name: string;
  description: string | null;
}

export interface UseCaseResponse {
  id: number;
  projectId: number;
  featureId: number;
  featureCode: string;
  code: string;
  name: string;
  actors: string;
  description: string | null;
}

export interface ScreenFunctionResponse {
  id: number;
  projectId: number;
  useCaseId: number | null;
  useCaseCode: string | null;
  code: string;
  name: string;
  type: ScreenFunctionType;
  description: string | null;
  fieldCount: number;
  transactionCount: number;
  authMatrix: Record<string, ScreenAuthPermissions>;
}

export interface BusinessRuleResponse {
  id: number;
  projectId: number;
  code: string;
  definition: string;
}

export interface AppMessageResponse {
  id: number;
  projectId: number;
  code: string;
  messageType: AppMessageType;
  context: string;
  content: string;
}

export interface DiagramAttachmentResponse {
  id: number;
  projectId: number;
  diagramType: DiagramType;
  title: string;
  fileName: string;
  fileUrl: string;
  uploadedAt: string;
}
