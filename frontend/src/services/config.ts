export type DataMode = 'mock' | 'api';

/**
 * Phase A (UI-First) defaults to 'mock'.
 * Phase C flips NEXT_PUBLIC_DATA_MODE=api to switch all services to real Spring Boot REST APIs
 * without modifying any React UI component.
 */
export const DATA_MODE: DataMode =
  process.env.NEXT_PUBLIC_DATA_MODE === 'api' ? 'api' : 'mock';

export const IS_MOCK_MODE = DATA_MODE === 'mock';
